package com.github.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.io.File

/** Reads packages installed for the current Android user only; never transmits the inventory. */
internal object InstalledAppsScanner {
    /** Render a small device-local package icon on demand; never persist or upload it. */
    fun iconBase64(context: Context, packageName: String): String? {
        if (!packageName.matches(Regex("[A-Za-z_][\\w]*(?:\\.[A-Za-z_][\\w]*)+"))) return null
        val icon = try { context.packageManager.getApplicationIcon(packageName) } catch (_: Exception) { return null }
        val size = (56 * context.resources.displayMetrics.density).toInt().coerceIn(48, 112)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        return try {
            val canvas = android.graphics.Canvas(bitmap)
            icon.setBounds(0, 0, size, size)
            icon.draw(canvas)
            val bytes = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bytes)
            "data:image/png;base64," + android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP)
        } finally { bitmap.recycle() }
    }

    /** Inspect selected installed base APK locally; never transmit APK bytes or the inventory. */
    @Suppress("DEPRECATION")
    fun inspectInstalledApk(context: Context, packageName: String): JSONObject {
        require(packageName.matches(Regex("[A-Za-z_][\\w]*(?:\\.[A-Za-z_][\\w]*)+"))) { "无效包名" }
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        else pm.getApplicationInfo(packageName, 0)
        return inspectApkFile(context, File(info.sourceDir))
    }

    @Suppress("DEPRECATION")
    fun inspectApkFile(context: Context, file: File): JSONObject {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val pkg = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IllegalArgumentException("不是可读取的 Android APK")
        fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val signers = if (Build.VERSION.SDK_INT >= 28) pkg.signingInfo?.apkContentsSigners else pkg.signatures
        val wholeHash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val chunk = ByteArray(65536)
            while (true) { val n = stream.read(chunk); if (n < 0) break; wholeHash.update(chunk, 0, n) }
        }
        val result = JSONObject().put("packageId", pkg.packageName)
            .put("versionName", pkg.versionName ?: "")
            .put("versionCode", if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toString() else pkg.versionCode.toString())
            .put("currentSigners", JSONArray(signers?.map { digest(it.toByteArray()) } ?: emptyList<String>()))
            .put("sha256", wholeHash.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) })
            .put("splitApkNote", "仅检查 base APK；拆分包的其它 split 未作同源认定")

        val urls = linkedSetOf<String>()
        val repos = linkedSetOf<String>()
        val fingerprints = JSONArray()
        val adIndicators = linkedMapOf<String, JSONObject>()
        val adSdkPatterns = listOf(
            Triple("google-mobile-ads", "Google Mobile Ads", "com/google/android/gms/ads/"),
            Triple("meta-audience-network", "Meta Audience Network", "com/facebook/ads/"),
            Triple("unity-ads", "Unity Ads", "com/unity3d/ads/"),
            Triple("applovin", "AppLovin", "com/applovin/"),
            Triple("ironsource", "ironSource", "com/ironsource/"),
            Triple("pangle", "Pangle / 穿山甲", "com/bytedance/sdk/openadsdk/"),
            Triple("tencent-ads", "腾讯广告", "com/qq/e/ads/")
        )
        val inspected = JSONArray()
        val urlRegex = Regex("https?://[A-Za-z0-9._~:/?#@!$&()*+,;=%-]{4,220}")
        val ghRegex = Regex("https?://(?:www\\.)?github\\.com/([A-Za-z0-9-]{1,39})/([A-Za-z0-9_.-]{1,100})", RegexOption.IGNORE_CASE)
        var total = 0
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val filename = entry.name
                if (filename != "resources.arsc" && filename != "AndroidManifest.xml" && !filename.matches(Regex("classes[0-9]*\\.dex")) &&
                    !filename.matches(Regex("(?:assets|res/raw)/.+\\.(?:json|txt|xml|properties|yaml|yml)"))) continue
                if (entry.size < 0 || entry.size > 8L * 1024 * 1024 || total + entry.size > 20L * 1024 * 1024) continue
                val bytes = zip.getInputStream(entry).use { stream ->
                    val out = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(32768)
                    while (out.size() <= 8 * 1024 * 1024) {
                        val n = stream.read(chunk)
                        if (n < 0) break
                        out.write(chunk, 0, n)
                    }
                    out.toByteArray()
                }
                if (bytes.size > 8 * 1024 * 1024) continue
                total += bytes.size
                inspected.put(filename)
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
                fingerprints.put(JSONObject().put("path", filename).put("sha256", sha))
                val text = String(bytes, Charsets.ISO_8859_1)
                for ((id, label, pattern) in adSdkPatterns) {
                    if (text.contains(pattern) && !adIndicators.containsKey(id)) {
                        adIndicators[id] = JSONObject().put("id", id).put("label", label)
                            .put("category", "广告 SDK 代码线索").put("confidence", "string-match")
                    }
                }
                for (match in urlRegex.findAll(text)) {
                    if (urls.size < 25) urls.add(match.value.substringBefore('?').substringBefore('#'))
                    for (github in ghRegex.findAll(match.value)) {
                        if (repos.size < 20) repos.add("${github.groupValues[1]}/${github.groupValues[2].removeSuffix(".git")}")
                    }
                }
            }
        }
        return result.put("githubRepos", JSONArray(repos.toList()))
            .put("updateUrls", JSONArray(urls.toList()))
            .put("fingerprints", fingerprints).put("inspectedEntries", inspected)
            .put("adIndicators", JSONArray(adIndicators.values.toList()))
    }

    @Suppress("DEPRECATION")
    fun collect(context: Context): JSONObject {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            pm.getInstalledPackages(flags)
        }
        val apps = JSONArray()
        for (pkg in installed) {
            try {
                val app = pkg.applicationInfo ?: continue
                val system = app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val signatureHistory = JSONArray()
                val signature = try {
                    val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val signingInfo = pkg.signingInfo
                        if (signingInfo?.hasMultipleSigners() == true) signingInfo.apkContentsSigners
                        else signingInfo?.signingCertificateHistory
                    } else pkg.signatures
                    certs?.take(8)?.forEach { cert ->
                        val hash = MessageDigest.getInstance("SHA-256").digest(cert.toByteArray())
                            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                        signatureHistory.put(hash)
                    }
                    // Android returns history from older to newer for a valid rotation;
                    // prefer the current certificate, not the earliest in history.
                    val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.signingInfo?.apkContentsSigners?.firstOrNull()
                    } else pkg.signatures?.firstOrNull()
                    current?.toByteArray()?.let { bytes ->
                        MessageDigest.getInstance("SHA-256").digest(bytes)
                            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    } ?: ""
                } catch (_: Exception) { "" }
                val installer = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        pm.getInstallSourceInfo(pkg.packageName).installingPackageName ?: ""
                    } else pm.getInstallerPackageName(pkg.packageName) ?: ""
                } catch (_: Exception) { "" }
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode.toString()
                } else pkg.versionCode.toString()
                apps.put(JSONObject()
                    .put("name", pm.getApplicationLabel(app).toString())
                    .put("packageId", pkg.packageName)
                    .put("versionName", pkg.versionName ?: "")
                    .put("versionCode", versionCode)
                    .put("signature", signature)
                    .put("signatureHistory", signatureHistory)
                    .put("installSource", installer)
                    .put("isSystem", system)
                    .put("enabled", app.enabled))
            } catch (_: Exception) {
                // One inaccessible/broken package must not prevent enumerating the rest.
            }
        }
        return JSONObject().put("apps", apps).put("count", apps.length())
    }
}
