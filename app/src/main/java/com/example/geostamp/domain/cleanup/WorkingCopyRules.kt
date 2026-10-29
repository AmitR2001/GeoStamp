package com.example.geostamp.domain.cleanup

class WorkingFile(val name: String, val lastModifiedMillis: Long, val sizeBytes: Long)

/**
 * Decides which private working copies are leftovers: not used by any saved photo and old enough
 * that no editor can still be using them. Pure Kotlin, unit tested.
 */
object WorkingCopyRules {
    const val AUTO_MIN_AGE_MS = 24L * 60 * 60 * 1000
    const val MANUAL_MIN_AGE_MS = 10L * 60 * 1000

    fun selectCleanable(
        files: List<WorkingFile>,
        referencedNames: Set<String>,
        nowMillis: Long,
        minAgeMillis: Long
    ): List<WorkingFile> = files.filter { file ->
        file.name !in referencedNames && nowMillis - file.lastModifiedMillis >= minAgeMillis
    }
}
