package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterLibraryDownloadPolicyTest {

    @Test
    fun optionalReciterIsNotDownloadedUntilOptIn() {
        assertFalse(
            ReciterLibraryDownloadPolicy.shouldDownload(
                isBuiltin = false,
                reciterId = 9L,
                optedInIds = emptySet(),
            )
        )
        assertTrue(
            ReciterLibraryDownloadPolicy.shouldDownload(
                isBuiltin = false,
                reciterId = 9L,
                optedInIds = setOf(9L),
            )
        )
    }

    @Test
    fun builtinReciterDownloadsWithoutOptIn() {
        assertTrue(
            ReciterLibraryDownloadPolicy.shouldDownload(
                isBuiltin = true,
                reciterId = 3L,
                optedInIds = emptySet(),
            )
        )
    }

    @Test
    fun optionalReciterWithPendingAudioNeedsConfirm() {
        assertTrue(
            ReciterLibraryDownloadPolicy.shouldConfirmLibraryDownload(
                isBuiltin = false,
                alreadyOptedIn = false,
                pendingCount = 4,
            )
        )
    }

    @Test
    fun builtinOrOptedInOrEmptyLibrarySkipsConfirm() {
        assertFalse(
            ReciterLibraryDownloadPolicy.shouldConfirmLibraryDownload(
                isBuiltin = true,
                alreadyOptedIn = false,
                pendingCount = 4,
            )
        )
        assertFalse(
            ReciterLibraryDownloadPolicy.shouldConfirmLibraryDownload(
                isBuiltin = false,
                alreadyOptedIn = true,
                pendingCount = 4,
            )
        )
        assertFalse(
            ReciterLibraryDownloadPolicy.shouldConfirmLibraryDownload(
                isBuiltin = false,
                alreadyOptedIn = false,
                pendingCount = 0,
            )
        )
    }

    @Test
    fun allowedIdsIncludeBuiltinAndOptedInOnly() {
        val reciters = listOf(
            1L to true,
            2L to false,
            3L to false,
        )
        assertEquals(
            setOf(1L, 3L),
            ReciterLibraryDownloadPolicy.allowedReciterIds(reciters, optedInIds = setOf(3L)),
        )
    }
}
