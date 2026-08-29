package com.greendome.adhkar.util

import com.greendome.adhkar.data.model.NumberDigitStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormattingTest {

    @Test
    fun latinConvertsArabicIndicFromLocalizedFormat() {
        assertEquals("بعد 5 دقيقة", "بعد ٥ دقيقة".formatDigits(NumberDigitStyle.LATIN))
    }

    @Test
    fun latinConvertsArabicIndicTime() {
        assertEquals("اليوم 19:00", "اليوم ١٩:٠٠".formatDigits(NumberDigitStyle.LATIN))
    }

    @Test
    fun latinLeavesAsciiDigits() {
        assertEquals("25", "25".formatDigits(NumberDigitStyle.LATIN))
        assertEquals(25, 25.formatDigits(NumberDigitStyle.LATIN).toInt())
    }

    @Test
    fun arabicIndicConvertsAsciiDigits() {
        assertEquals("٢٥", 25.formatDigits(NumberDigitStyle.ARABIC_INDIC))
        assertEquals("اليوم ١٩:٠٠", "اليوم 19:00".formatDigits(NumberDigitStyle.ARABIC_INDIC))
    }

    @Test
    fun arabicIndicLeavesArabicIndicDigits() {
        assertEquals("بعد ٥ دقيقة", "بعد ٥ دقيقة".formatDigits(NumberDigitStyle.ARABIC_INDIC))
    }

    @Test
    fun latinConvertsPersianDigits() {
        assertEquals("19:00", "۱۹:۰۰".formatDigits(NumberDigitStyle.LATIN))
    }
}
