package com.whattocook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAccessTest {

    @Test
    fun `free users get the first five dishes of each list`() {
        (0 until 5).forEach { assertFalse(LibraryAccess.isLocked(it, isPro = false)) }
        assertTrue(LibraryAccess.isLocked(5, isPro = false))
        assertTrue(LibraryAccess.isLocked(30, isPro = false))
    }

    @Test
    fun `pro users never see locks`() {
        assertFalse(LibraryAccess.isLocked(5, isPro = true))
        assertFalse(LibraryAccess.isLocked(99, isPro = true))
        assertEquals(0, LibraryAccess.lockedCount(40, isPro = true))
    }

    @Test
    fun `locked count never goes negative`() {
        assertEquals(18, LibraryAccess.lockedCount(23, isPro = false))
        assertEquals(0, LibraryAccess.lockedCount(3, isPro = false))
    }
}
