package com.greendome.adhkar.ui.theme

const val APP_FONT_SCALE_MIN = 0.8f
const val APP_FONT_SCALE_MAX = 1.5f

/** Stored 100% matches the former 120% size. */
const val APP_FONT_SCALE_BASELINE = 1.2f

fun effectiveAppFontScale(userScale: Float): Float =
    userScale.coerceIn(APP_FONT_SCALE_MIN, APP_FONT_SCALE_MAX) * APP_FONT_SCALE_BASELINE
