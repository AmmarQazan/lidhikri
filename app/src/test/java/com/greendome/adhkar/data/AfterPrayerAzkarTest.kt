package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AfterPrayerAzkarTest {

    @Test
    fun catalogAddsAuthenticItemsWithoutDuplicateTexts() {
        val items = IslambookAzkarSeed.afterPrayerCatalog()
        assertTrue(items.any { it.text.contains("لَا مَانِعَ") })
        assertTrue(items.any { it.text.contains("كَرِهَ الْكَافِرُونَ") })
        assertTrue(items.any { it.text.contains("يُحْيِي وَيُمِيتُ") && it.repeat == 10 })
        assertTrue(items.any { it.text.contains("أَعِنِّي عَلَى ذِكْرِكَ") })
        assertTrue(items.any { it.text.contains("الْبُخْلِ") && it.text.contains("أَرْذَلِ") })
        assertTrue(items.any { it.detect == "كرسي" })
        assertTrue(items.any { it.detect == "الصمد" })
        assertTrue(items.any { it.detect == "الفلق" })
        assertTrue(items.any { it.detect == "الوسواس" })
        val shortTahlil33 = items.filter { seed ->
            seed.repeat == 33 &&
                seed.matches(seed.text, 33) &&
                SubaihatReciterSeed.normalizeAr(seed.text).contains("وحده لا شريك") &&
                listOf("لا مانع", "لا حول", "يحيي", "نعبد").none {
                    SubaihatReciterSeed.normalizeAr(seed.text).contains(SubaihatReciterSeed.normalizeAr(it))
                }
        }
        assertTrue(shortTahlil33.isEmpty())
        val keys = items.map { SubaihatReciterSeed.normalizeAr(it.text) to it.repeat }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun detectsDoNotOverlapWithinCatalog() {
        val items = IslambookAzkarSeed.afterPrayerCatalog()
        items.forEach { seed ->
            assertTrue("detect must match own text: ${seed.detect}", seed.matches(seed.text, seed.repeat))
            items.filter { it !== seed }.forEach { other ->
                assertFalse(
                    "${seed.detect} matched other (${other.detect})",
                    seed.matches(other.text, other.repeat),
                )
            }
        }
    }

    @Test
    fun newMeaningsCoverAddedAdhkar() {
        val catalog = IslambookAzkarSeed.afterPrayerCatalog()
        val laMani = catalog.first { it.detect == "لا مانع" }.text
        val laNaabudu = catalog.first { it.detect == "كره الكافرون" }.text
        val ten = catalog.first { it.detect == "يحيي ويميت" }.text
        val ainni = catalog.first { it.detect == "اعني على ذكرك" }.text
        val bukhl = catalog.first { it.detect == "البخل" }.text
        assertTrue(ContentI18n.azkarMeaning(laMani, "en")!!.contains("withhold", ignoreCase = true))
        assertTrue(ContentI18n.azkarMeaning(laNaabudu, "en")!!.contains("worship", ignoreCase = true))
        assertTrue(ContentI18n.azkarMeaning(ten, "en")!!.contains("life", ignoreCase = true))
        assertTrue(ContentI18n.azkarMeaning(ainni, "en")!!.contains("remember", ignoreCase = true))
        assertTrue(ContentI18n.azkarMeaning(bukhl, "en")!!.contains("miserliness", ignoreCase = true))
        assertTrue(ContentI18n.azkarMeaning(laMani, "tr")!!.contains("Allah"))
        assertTrue(ContentI18n.virtue("بعد المغرب والفجر عشر مرات.", "en")!!.contains("Maghrib"))
        assertTrue(ContentI18n.virtue("بعد كل فريضة. رواه البخاري ومسلم.", "en")!!.contains("Bukhari"))
    }

    @Test
    fun reusableAudioIsLinkedAndLongDuasHaveOwnFiles() {
        val catalog = IslambookAzkarSeed.afterPrayerCatalog()
        fun file(detect: String) = SubaihatReciterSeed.matchedAzkarFile(
            "after_prayer",
            catalog.first { it.detect == detect }.text,
        )
        assertEquals("after_prayer/istighfar_3.mp3", file("استغفر الله"))
        assertEquals("after_prayer/salam.mp3", file("السلام"))
        assertEquals("after_prayer/tawhid.mp3", file("وحده لا شريك"))
        assertEquals("after_prayer/tawhid.mp3", file("يحيي ويميت"))
        assertEquals("after_prayer/subhan.mp3", file("سبحان الله"))
        assertEquals("after_prayer/hamd.mp3", file("الحمد لله"))
        assertEquals("after_prayer/takbir.mp3", file("الله اكبر"))
        assertEquals("after_prayer/ayat_kursi.mp3", file("كرسي"))
        assertEquals("after_prayer/ikhlas.mp3", file("الصمد"))
        assertEquals("after_prayer/falaq.mp3", file("الفلق"))
        assertEquals("after_prayer/nas.mp3", file("الوسواس"))
        assertEquals("after_prayer/la_mani.mp3", file("لا مانع"))
        assertEquals("after_prayer/mukhlisin.mp3", file("كره الكافرون"))
        assertEquals("after_prayer/bukhl.mp3", file("البخل"))
        assertEquals("after_prayer/ainni.mp3", file("اعني على ذكرك"))
    }
}
