package com.greendome.adhkar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.ArabicFontStyle

val LocalArabicFontFamily = compositionLocalOf<FontFamily> { FontFamily.Default }

private val Uthmani1Family = FontFamily(Font(R.font.scheherazade_new_regular))
private val Uthmani2Family = FontFamily(Font(R.font.amiri_regular))
private val Indopak1Family = FontFamily(Font(R.font.noto_naskh_arabic_regular))
private val Indopak2Family = FontFamily(Font(R.font.lateef_regular))
private val BengaliFamily = FontFamily(Font(R.font.noto_sans_bengali_regular))

fun arabicFontFamily(style: ArabicFontStyle): FontFamily = when (style) {
    ArabicFontStyle.DEFAULT -> FontFamily.Default
    ArabicFontStyle.UTHMANI_1 -> Uthmani1Family
    ArabicFontStyle.UTHMANI_2 -> Uthmani2Family
    ArabicFontStyle.INDOPAK_1 -> Indopak1Family
    ArabicFontStyle.INDOPAK_2 -> Indopak2Family
    ArabicFontStyle.BENGALI -> BengaliFamily
}

@Composable
fun AppArabicFont(style: ArabicFontStyle, content: @Composable () -> Unit) {
    val family = remember(style) { arabicFontFamily(style) }
    CompositionLocalProvider(LocalArabicFontFamily provides family) {
        content()
    }
}

@Composable
fun arabicTextStyle(base: TextStyle = MaterialTheme.typography.bodyLarge): TextStyle =
    base.copy(fontFamily = LocalArabicFontFamily.current)

@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        style = arabicTextStyle(style),
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}
