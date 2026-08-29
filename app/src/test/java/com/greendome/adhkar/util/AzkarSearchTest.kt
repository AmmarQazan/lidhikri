package com.greendome.adhkar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarSearchTest {

    private val sources = listOf(
        AzkarSearchHit(
            key = "azkar:1",
            textAr = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            virtueAr = "من قالها حمد الله",
            sectionTitle = "أذكار الصباح",
            collectionId = "morning",
            itemId = 1,
        ),
        AzkarSearchHit(
            key = "azkar:2",
            textAr = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ",
            sectionTitle = "أذكار المساء",
            collectionId = "evening",
            itemId = 2,
        ),
        AzkarSearchHit(
            key = "dhikr:10",
            textAr = "سبحان الله وبحمده",
            sectionTitle = "تسبيحاتي",
            extraSearchText = "Subhan Allah",
            isMyDhikr = true,
        ),
    )

    @Test
    fun matchesIgnoresTashkeelAndAlefVariants() {
        assertTrue(AzkarSearch.matches("الْحَمْدُ لِلَّهِ", "الحمد"))
        assertTrue(AzkarSearch.matches("أعوذ بكلمات الله", "اعوذ"))
        assertTrue(AzkarSearch.matches("سبحان الله وبحمده", "سبحان"))
        assertFalse(AzkarSearch.matches("سبحان الله", "الحمد"))
    }

    @Test
    fun matchesRequiresAllQueryWords() {
        assertTrue(AzkarSearch.matches("الحمد لله رب العالمين", "الحمد رب"))
        assertFalse(AzkarSearch.matches("الحمد لله", "الحمد المساء"))
    }

    @Test
    fun blankQueryReturnsNoHits() {
        assertTrue(AzkarSearch.search("   ", sources).isEmpty())
    }

    @Test
    fun searchFindsItemAndShowsSection() {
        val hits = AzkarSearch.search("الحمد", sources)
        assertEquals(1, hits.size)
        assertEquals("أذكار الصباح", hits[0].sectionTitle)
        assertEquals(1L, hits[0].itemId)
        assertEquals("morning", hits[0].collectionId)
    }

    @Test
    fun searchBySectionTitle() {
        val hits = AzkarSearch.search("المساء", sources)
        assertEquals(1, hits.size)
        assertEquals(2L, hits[0].itemId)
        assertEquals("أذكار المساء", hits[0].sectionTitle)
    }

    @Test
    fun searchIncludesCustomDhikrAndEnglishText() {
        val hits = AzkarSearch.search("Subhan", sources)
        assertEquals(1, hits.size)
        assertTrue(hits[0].isMyDhikr)
        assertEquals("تسبيحاتي", hits[0].sectionTitle)
    }
}
