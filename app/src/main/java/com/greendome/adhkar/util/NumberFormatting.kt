package com.greendome.adhkar.util

import com.greendome.adhkar.data.model.NumberDigitStyle

private const val ARABIC_INDIC_ZERO = '\u0660'
private const val ARABIC_INDIC_NINE = '\u0669'
private const val EXTENDED_ARABIC_INDIC_ZERO = '\u06F0'
private const val EXTENDED_ARABIC_INDIC_NINE = '\u06F9'

fun Int.formatDigits(style: NumberDigitStyle): String = toString().formatDigits(style)

fun String.formatDigits(style: NumberDigitStyle): String = when (style) {
    NumberDigitStyle.LATIN -> mapDigits { digit -> '0' + digit }
    NumberDigitStyle.ARABIC_INDIC -> mapDigits { digit -> ARABIC_INDIC_ZERO + digit }
}

private inline fun String.mapDigits(crossinline digitToChar: (Int) -> Char): String {
    var changed = false
    val mapped = buildString(length) {
        for (char in this@mapDigits) {
            val digit = char.decimalDigitOrNull()
            if (digit == null) {
                append(char)
            } else {
                val next = digitToChar(digit)
                if (next != char) changed = true
                append(next)
            }
        }
    }
    return if (changed) mapped else this
}

private fun Char.decimalDigitOrNull(): Int? = when (this) {
    in '0'..'9' -> this - '0'
    in ARABIC_INDIC_ZERO..ARABIC_INDIC_NINE -> this - ARABIC_INDIC_ZERO
    in EXTENDED_ARABIC_INDIC_ZERO..EXTENDED_ARABIC_INDIC_NINE -> this - EXTENDED_ARABIC_INDIC_ZERO
    else -> null
}
