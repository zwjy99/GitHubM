package com.github.manager

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Explicit user-selected APK inspection. Never executes/installs it or sends login tokens. */
internal object ApkProvenanceVerifier {
    fun inspect(context: Context, inputUrl: String, cancelled: () -> Boolean = { false }): JSONObject {
        fun checked(value: String): URL {
            val u = URL(value)
            require(u.protocol == "https" && u.userInfo == null && u.host.isNotBlank()) { "APK 链接必须是 HTTPS 且不含账号密码" }
            return u
        }
        var url = checked(inputUrl)
        val file = File.createTempFile("githubm-provenance-", ".apk", context.cacheDir)
        var connection: HttpURLConnection? = null
        val deadline = System.currentTimeMillis() + 120000
        try {
            var downloaded = false
            for (redirect in 0..5) {
                require(!cancelled()) { "已停止 APK 验证" }
                connection = url.openConnection() as HttpURLConnection
                val c = connection
                c.connectTimeout = 10000; c.readTimeout = 15000
                c.instanceFollowRedirects = false; c.useCaches = false
                if (c.responseCode in listOf(301,302,303,307,308)) {
                    val location = c.getHeaderField("Location") ?: error("下载跳转缺少地址")
                    url = checked(URL(url, location).toString()); c.disconnect(); continue
                }
                require(c.responseCode == 200) { "下载失败 HTTP ${c.responseCode}" }
                require(c.contentLengthLong <= 130L * 1024 * 1024) { "APK 超过 130 MB 验证上限" }
                var total = 0L
                c.inputStream.use { stream -> file.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        require(!cancelled()) { "已停止 APK 验证" }
                        require(System.currentTimeMillis() < deadline) { "APK 验证下载超时" }
                        val n = stream.read(buffer); if (n < 0) break
                        total += n; require(total <= 130L * 1024 * 1024) { "APK 超过 130 MB 验证上限" }
                        output.write(buffer,0,n)
                    }
                } }
                c.disconnect(); downloaded = true; break
            }
            require(downloaded) { "APK 下载跳转次数过多" }
            require(!cancelled()) { "已停止 APK 验证" }
            return InstalledAppsScanner.inspectApkFile(context, file).put("sourceUrl", inputUrl)
        } finally { connection?.disconnect(); file.delete() }
    }
}
