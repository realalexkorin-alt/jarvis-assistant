package dev.jarvis.assistant.updates

/** Version scheme: major * 1_000_000 + minor * 1_000 + patch. */
object Versioning {
    fun versionCode(tag: String): Long? {
        val match = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(tag) ?: return null
        val (major, minor, patch) = match.destructured
        val a = major.toLongOrNull() ?: return null
        val b = minor.toLongOrNull() ?: return null
        val c = patch.toLongOrNull() ?: return null
        if (a > 2000 || b > 999 || c > 999) return null
        return a * 1_000_000 + b * 1_000 + c
    }
}
