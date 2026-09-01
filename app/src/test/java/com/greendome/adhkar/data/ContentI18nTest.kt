package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentI18nTest {
    @Test
    fun dhikrFallsBackToOverlayForNewLanguages() {
        val dhikr = DhikrEntity(textAr = "سبحان الله", textEn = "Subhan Allah")
        assertEquals("Sübhanallah", dhikr.localizedText("tr"))
        assertEquals("سبحان اللہ", dhikr.localizedText("ur"))
        assertEquals("Subhanallah", dhikr.localizedText("id"))
        assertEquals("सुब्हानल्लाह", dhikr.localizedText("hi"))
        assertEquals("سبحان الله", dhikr.localizedText("ar"))
        assertEquals("Subhan Allah", dhikr.localizedText("en"))
        val unknown = DhikrEntity(textAr = "نص غير معروف", textEn = "English copy")
        assertEquals("نص غير معروف", unknown.localizedText("tr"))
        assertEquals("نص غير معروف", unknown.localizedText("id"))
        assertEquals("English copy", unknown.localizedText("en"))
    }

    @Test
    fun collectionTitlesCoverNewLanguages() {
        val morning = AdhkarCollectionEntity(id = "morning", titleAr = "أذكار الصباح", titleEn = "Morning adhkar")
        assertEquals("Sabah zikirleri", morning.localizedTitle("tr"))
        assertEquals("صبح کے اذکار", morning.localizedTitle("ur"))
        assertEquals("Dzikir pagi", morning.localizedTitle("id"))
        assertEquals("प्रातःकालीन अज़कार", morning.localizedTitle("hi"))
        assertEquals("أذكار الصباح", morning.localizedTitle("ar"))
        val blessed = AdhkarCollectionEntity(
            id = "blessed_days",
            titleAr = "أذكار الأيام المباركة",
            titleEn = "Blessed days adhkar",
        )
        assertEquals("Mübarek günlerin zikirleri", blessed.localizedTitle("tr"))
        assertEquals("مبارک دنوں کے اذکار", blessed.localizedTitle("ur"))
        assertEquals("أذكار الأيام المباركة", blessed.localizedTitle("ar"))
        val riding = AdhkarCollectionEntity(id = "riding", titleAr = "أذكار الركوب", titleEn = "Riding adhkar")
        assertEquals("Bineğe binince", riding.localizedTitle("tr"))
        assertEquals("أذكار الركوب", riding.localizedTitle("ar"))
        assertEquals("Riding adhkar", riding.localizedTitle("en"))
    }

    @Test
    fun azkarShowsTranslatedTextNotEnglishFallback() {
        val item = AzkarItemEntity(
            collectionId = "after_prayer",
            textAr = "أَسْتَغْفِرُ اللهَ.",
            virtueAr = "",
        )
        assertEquals("أَسْتَغْفِرُ اللهَ.", item.localizedText("ar"))
        assertEquals("Allah'tan bağışlanma dilerim.", item.localizedText("tr"))
        assertEquals("میں اللہ سے بخشش مانگتا ہوں۔", item.localizedText("ur"))
        assertEquals("Aku memohon ampun kepada Allah.", item.localizedText("id"))
        assertEquals("मैं अल्लाह से क्षमा माँगता हूँ।", item.localizedText("hi"))
        assertEquals("I seek Allah's forgiveness.", item.localizedText("en"))
        assertEquals("Je demande pardon à Allah.", item.localizedText("fr"))
        assertTrue(item.localizedText("tr") != item.localizedText("en"))
        assertTrue(item.localizedText("id") != item.localizedText("en"))
    }

    @Test
    fun morningDhikrHasMeaningInNewLanguages() {
        val text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ ، وَالْحَمْدُ لِلَّهِ ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ"
        val tr = ContentI18n.azkarMeaning(text, "tr")
        val ur = ContentI18n.azkarMeaning(text, "ur")
        val id = ContentI18n.azkarMeaning(text, "id")
        val hi = ContentI18n.azkarMeaning(text, "hi")
        assertTrue(tr!!.contains("Sabaha"))
        assertTrue(ur!!.contains("صبح"))
        assertTrue(id!!.contains("pagi"))
        assertTrue(hi!!.contains("सुबह"))
    }

    @Test
    fun virtueFallsBackByPhrase() {
        val virtue = ContentI18n.virtue("سؤال العلم النافع، والرزق الطيب، والعمل المتقبل.", "tr")
        assertTrue(virtue!!.contains("ilim"))
    }
}
