package com.example.geostamp.domain.cleanup

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkingCopyRulesTest {
    private val now = 1_000_000_000L
    private val hour = 60L * 60 * 1000

    private fun file(name: String, ageMs: Long) = WorkingFile(name, now - ageMs, 100)

    @Test
    fun used_files_are_never_cleaned() {
        val files = listOf(file("a.jpg", 100 * hour))
        assertEquals(emptyList<WorkingFile>(), WorkingCopyRules.selectCleanable(files, setOf("a.jpg"), now, WorkingCopyRules.AUTO_MIN_AGE_MS))
    }

    @Test
    fun young_unused_files_are_kept_because_an_editor_may_use_them() {
        val files = listOf(file("new.jpg", 5 * 60 * 1000))
        assertEquals(0, WorkingCopyRules.selectCleanable(files, emptySet(), now, WorkingCopyRules.AUTO_MIN_AGE_MS).size)
        assertEquals(0, WorkingCopyRules.selectCleanable(files, emptySet(), now, WorkingCopyRules.MANUAL_MIN_AGE_MS).size)
    }

    @Test
    fun old_unused_files_are_selected() {
        val files = listOf(file("old.jpg", 30 * hour), file("used.jpg", 30 * hour), file("fresh.jpg", hour))
        val result = WorkingCopyRules.selectCleanable(files, setOf("used.jpg"), now, WorkingCopyRules.AUTO_MIN_AGE_MS)
        assertEquals(listOf("old.jpg"), result.map { it.name })
    }

    @Test
    fun manual_clean_up_uses_a_shorter_age() {
        val files = listOf(file("a.jpg", hour))
        assertEquals(1, WorkingCopyRules.selectCleanable(files, emptySet(), now, WorkingCopyRules.MANUAL_MIN_AGE_MS).size)
    }
}
