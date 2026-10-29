package com.example.geostamp.domain.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizesTest {
    @Test fun small_sizes_are_in_bytes() = assertEquals("512 B", FileSizes.human(512))
    @Test fun kilobytes() = assertEquals("1.5 KB", FileSizes.human(1536))
    @Test fun megabytes() = assertEquals("5.0 MB", FileSizes.human(5L * 1024 * 1024))
    @Test fun gigabytes() = assertEquals("2.0 GB", FileSizes.human(2L * 1024 * 1024 * 1024))
    @Test fun zero() = assertEquals("0 B", FileSizes.human(0))
}
