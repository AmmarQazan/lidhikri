package com.greendome.adhkar.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRecoveryTest {

    @Test
    fun emptyDhikrMeansCatalogEmpty() {
        assertTrue(CatalogRecovery.isOfficialCatalogEmpty(0, 8))
    }

    @Test
    fun emptyCollectionsMeansCatalogEmpty() {
        assertTrue(CatalogRecovery.isOfficialCatalogEmpty(36, 0))
    }

    @Test
    fun populatedCatalogIsNotEmpty() {
        assertFalse(CatalogRecovery.isOfficialCatalogEmpty(36, 8))
    }

    @Test
    fun skipsBuiltinSeedOnlyWhenRemoteCatalogAlreadyPresent() {
        assertTrue(CatalogRecovery.shouldSkipBuiltinSeed(2, catalogEmpty = false))
        assertFalse(CatalogRecovery.shouldSkipBuiltinSeed(2, catalogEmpty = true))
        assertFalse(CatalogRecovery.shouldSkipBuiltinSeed(0, catalogEmpty = false))
        assertFalse(CatalogRecovery.shouldSkipBuiltinSeed(0, catalogEmpty = true))
    }

    @Test
    fun forcesRemoteSyncWhenCatalogEmptyEvenIfVersionMatches() {
        assertTrue(CatalogRecovery.shouldForceRemoteSync(catalogEmpty = true))
        assertFalse(CatalogRecovery.shouldForceRemoteSync(catalogEmpty = false))
    }

    @Test
    fun reappliesWhenRemoteHashDiffersEvenIfVersionLooksCurrent() {
        assertFalse(
            CatalogRecovery.shouldSkipRemoteApply(
                force = false,
                remoteSha256 = "aaa",
                appliedSha256 = ""
            )
        )
        assertFalse(
            CatalogRecovery.shouldSkipRemoteApply(
                force = false,
                remoteSha256 = "aaa",
                appliedSha256 = "bbb"
            )
        )
        assertTrue(
            CatalogRecovery.shouldSkipRemoteApply(
                force = false,
                remoteSha256 = "AAA",
                appliedSha256 = "aaa"
            )
        )
        assertFalse(
            CatalogRecovery.shouldSkipRemoteApply(
                force = true,
                remoteSha256 = "aaa",
                appliedSha256 = "aaa"
            )
        )
    }

    @Test
    fun backfillsWhenMorningOrShortTasbihMissing() {
        assertTrue(CatalogRecovery.needsBuiltinBackfill(hasMorningCollection = false, defaultShortTasbihCount = 12))
        assertTrue(CatalogRecovery.needsBuiltinBackfill(hasMorningCollection = true, defaultShortTasbihCount = 0))
        assertFalse(CatalogRecovery.needsBuiltinBackfill(hasMorningCollection = true, defaultShortTasbihCount = 12))
    }
}
