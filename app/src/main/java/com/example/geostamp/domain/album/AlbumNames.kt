package com.example.geostamp.domain.album

/** Rules for album (folder) names and file names the person types. Pure Kotlin, unit tested. */
object AlbumNames {
    const val MAX_LENGTH = 40

    /** Letters, digits, spaces, dash and underscore only. Returns null when nothing usable is left. */
    fun sanitizeAlbum(input: String): String? {
        val cleaned = input
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
            .trim()
            .replace(Regex(" +"), " ")
            .take(MAX_LENGTH)
            .trim()
        return cleaned.ifEmpty { null }
    }

    fun same(a: String?, b: String?): Boolean =
        a != null && b != null && a.equals(b, ignoreCase = true)

    /** Distinct album names, sorted ignoring case. */
    fun distinctSorted(names: List<String?>): List<String> {
        val result = mutableListOf<String>()
        for (name in names) {
            if (name == null) continue
            if (result.none { same(it, name) }) result.add(name)
        }
        return result.sortedBy { it.lowercase() }
    }

    /** A file name the person typed: keeps letters, digits, space, - _ . ( ) and forces a .jpg ending. */
    fun sanitizeFileName(input: String): String? {
        val withoutExtension = input.trim().removeSuffix(".jpg").removeSuffix(".JPG").removeSuffix(".jpeg")
        val base = withoutExtension
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' || it == '.' || it == '(' || it == ')' }
            .trim()
            .trim('.')
            .take(80)
            .trim()
        return if (base.isEmpty()) null else "$base.jpg"
    }
}
