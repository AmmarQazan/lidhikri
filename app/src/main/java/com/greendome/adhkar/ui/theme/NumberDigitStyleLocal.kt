package com.greendome.adhkar.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.greendome.adhkar.data.model.NumberDigitStyle
import com.greendome.adhkar.util.formatDigits

val LocalNumberDigitStyle = compositionLocalOf { NumberDigitStyle.ARABIC_INDIC }

@Composable
fun stringResourceDigits(@StringRes id: Int, vararg formatArgs: Any): String =
    stringResource(id, *formatArgs).formatDigits(LocalNumberDigitStyle.current)

@Composable
fun Int.formatLocalizedDigits(): String = formatDigits(LocalNumberDigitStyle.current)

@Composable
fun String.formatLocalizedDigits(): String = formatDigits(LocalNumberDigitStyle.current)
