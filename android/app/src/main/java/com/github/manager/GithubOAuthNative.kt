package com.github.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * GitHub OAuth App PKCE flow. Supports either an owner-operated exchange endpoint or
 * optional, user-provided OAuth App credentials stored with Android Keystore encryption.
 * No third-party author's OAuth identity is bundled in the distributed APK.
 */
class GithubOAuthNative(
    private val activity: AppCompatActivity,
    private val onResult: (token: String?, error: String?) -> Unit,
) {
    private val prefs = activity.getSharedPreferences("githubm_oauth_transient", Context.MODE_PRIVATE)
    private val callback = "githubm://oauth"
    private val ownCredentials = GithubOAuthCredentials(activity)

    /** Available from the login screen; only Android's native UI receives the secret. */
    fun startUserConfigured() {
        val saved = ownCredentials.load()
        if (saved != null) startRequest(saved.clientId, "user")
        else showUserConfiguration()
    }

    fun showUserConfiguration() {
        val current = ownCredentials.load()
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(10), dp(22), 0)
        }
        val tip = TextView(activity).apply {
            text = "在 GitHub Developer settings 注册自己的 OAuth App，回调填 githubm://oauth。\nClient Secret 只在本机加密保存，不会打包进 APK 或发送给第三方代理。"
            setPadding(0, 0, 0, dp(12))
        }
        val idField = EditText(activity).apply {
            hint = "Client ID"
            setSingleLine(true)
            setText(current?.clientId ?: "")
        }
        val secretField = EditText(activity).apply {
            hint = if (current == null) "Client Secret" else "Client Secret（留空沿用已保存的密钥）"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(tip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(idField)
        layout.addView(secretField)
        val dialog = AlertDialog.Builder(activity)
            .setTitle("自行配置 GitHub OAuth")
            .setView(layout)
            .setNegativeButton("取消", null)
            .setNeutralButton("清除配置", null)
            .setPositiveButton("保存并授权", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                ownCredentials.clear()
                dialog.dismiss()
                onResult(null, "已清除本机 OAuth 应用配置，仍可使用 PAT 登录")
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val id = idField.text.toString().trim()
                val incomingSecret = secretField.text.toString()
                val secret = if (incomingSecret.isEmpty() && current?.clientId == id) (current?.clientSecret ?: "") else incomingSecret
                try {
                    ownCredentials.save(id, secret)
                    dialog.dismiss()
                    startRequest(id, "user")
                } catch (_: Exception) {
                    secretField.error = "请填写有效的 Client ID 和 Client Secret"
                }
            }
        }
        dialog.show()
    }

    private fun randomUrlSafe(bytes: Int): String {
        val buffer = ByteArray(bytes)
        SecureRandom().nextBytes(buffer)
        return Base64.encodeToString(buffer, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun start(clientId: String, exchangeUrl: String) {
        if (!clientId.matches(Regex("[A-Za-z0-9]+"))) {
            onResult(null, "请先配置 GitHubM 自己的 OAuth Client ID")
            return
        }
        val workerUri = runCatching { Uri.parse(exchangeUrl) }.getOrNull()
        if (workerUri?.scheme != "https" || workerUri.host.isNullOrBlank() || workerUri.userInfo != null) {
            onResult(null, "OAuth 交换服务必须是可信 HTTPS 地址")
            return
        }
        startRequest(clientId, "worker", exchangeUrl)
    }

    private fun startRequest(clientId: String, mode: String, exchangeUrl: String = "") {
        val state = randomUrlSafe(24)
        val verifier = randomUrlSafe(48)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        prefs.edit().putString("state", state).putString("verifier", verifier)
            .putString("exchange", exchangeUrl).putString("client_id", clientId)
            .putString("mode", mode).putLong("created", System.currentTimeMillis()).apply()
        val url = Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", callback)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("scope", "repo read:user notifications").build()
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)) }
        catch (_: Exception) { prefs.edit().clear().apply(); onResult(null, "无法打开系统浏览器") }
    }

    /** Return true when this deep link was an OAuth callback. */
    fun handle(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (intent.action != Intent.ACTION_VIEW || uri.scheme != "githubm" || uri.host != "oauth") return false
        val state = prefs.getString("state", null)
        val verifier = prefs.getString("verifier", null)
        val exchange = prefs.getString("exchange", null)
        val clientId = prefs.getString("client_id", null)
        val mode = prefs.getString("mode", "worker") ?: "worker"
        val created = prefs.getLong("created", 0)
        // Remove single-use state before any network request, even if validation fails.
        prefs.edit().clear().apply()
        val code = uri.getQueryParameter("code")
        val receivedState = uri.getQueryParameter("state")
        if (code.isNullOrEmpty() || receivedState != state || state.isNullOrBlank() || verifier.isNullOrBlank()
            || clientId.isNullOrBlank() || (mode == "worker" && exchange.isNullOrBlank())
            || (System.currentTimeMillis() - created) !in 0L..600_000L) {
            onResult(null, "GitHub 授权状态无效或已过期，请重试")
            return true
        }
        CoroutineScope(Dispatchers.IO).launch {
            var token: String? = null
            var error: String? = null
            try {
                val credentials = if (mode == "user") ownCredentials.load() else null
                if (mode == "user" && (credentials == null || credentials.clientId != clientId)) {
                    throw IllegalStateException("本机 OAuth 配置丢失或已经更改")
                }
                val endpoint = if (mode == "user") "https://github.com/login/oauth/access_token" else exchange!!
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 12_000
                    conn.readTimeout = 15_000
                    conn.setRequestProperty("Accept", "application/json")
                    conn.useCaches = false
                    conn.instanceFollowRedirects = false
                    conn.doOutput = true
                    if (mode == "user") {
                        // Never pass the user-created secret through JavaScript or to a proxy.
                        val form = listOf(
                            "client_id" to clientId,
                            "client_secret" to credentials!!.clientSecret,
                            "code" to code,
                            "redirect_uri" to callback,
                            "code_verifier" to verifier,
                        ).joinToString("&") { (k, v) -> "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}" }
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        conn.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
                    } else {
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("X-GitHubM-Native", "1")
                        val payload = JSONObject().put("code", code).put("code_verifier", verifier).put("redirect_uri", callback)
                        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val status = conn.responseCode
                    val responseText = (if (status in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() } ?: "{}"
                    val response = JSONObject(responseText)
                    token = if (status in 200..299) response.optString("access_token").takeIf { it.isNotBlank() } else null
                    if (token == null) {
                        val githubMessage = response.optString("error_description").take(240)
                        error = if (githubMessage.isNotEmpty()) "GitHub 授权失败：$githubMessage"
                            else "GitHub 授权未完成（HTTP $status）"
                    }
                } finally { conn.disconnect() }
            } catch (_: Exception) { error = "授权失败，请检查网络及本机 OAuth 配置（不会自动重试）" }
            activity.runOnUiThread { onResult(token, error) }
        }
        return true
    }
}
