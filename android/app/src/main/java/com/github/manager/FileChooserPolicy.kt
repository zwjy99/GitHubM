package com.github.manager

/** File requests must not become a camera/photo chooser. */
internal object FileChooserPolicy {
    private fun accepted(types: List<String>) = types.flatMap { it.split(',') }.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    fun needsCamera(types: List<String>): Boolean = accepted(types).any {
        it.startsWith("image/") || it in listOf(".png", ".jpg", ".jpeg", ".webp", ".gif")
    }
    fun mimeTypes(types: List<String>): List<String> {
        val values = accepted(types)
        if (values.any { it == ".apk" || it == "application/vnd.android.package-archive" }) return listOf("application/vnd.android.package-archive")
        if (values.any { it == ".json" }) return listOf("application/json")
        return values.filter { it.contains('/') && !it.contains(' ') }.distinct().ifEmpty { listOf("*/*") }
    }
}
