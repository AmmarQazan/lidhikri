package com.greendome.adhkar.util

import com.greendome.adhkar.data.model.NumberDigitStyle

private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.formatDigits(style: NumberDigitStyle): String = toString().formatDigits(style)

fun String.formatDigits(style: NumberDigitStyle): String {
    if (style == NumberDigitStyle.LATIN) return this
    return buildString(length) {
        for (char in this@formatDigits) {
            append(
                if (char in '0'..'9') {
                    ARABIC_INDIC_DIGITS[char - '0']
                } else {
                    char
                }
            )
        }
    }
}
