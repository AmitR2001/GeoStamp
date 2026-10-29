package com.example.geostamp.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationAccessTest {
    @Test fun nothing_granted_is_none() =
        assertEquals(LocationAccess.NONE, LocationAccess.from(fineGranted = false, coarseGranted = false))

    @Test fun coarse_only_is_approximate() =
        assertEquals(LocationAccess.APPROXIMATE, LocationAccess.from(fineGranted = false, coarseGranted = true))

    @Test fun fine_and_coarse_is_precise() =
        assertEquals(LocationAccess.PRECISE, LocationAccess.from(fineGranted = true, coarseGranted = true))

    @Test fun fine_alone_is_still_precise() =
        assertEquals(LocationAccess.PRECISE, LocationAccess.from(fineGranted = true, coarseGranted = false))
}
