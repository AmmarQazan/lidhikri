package com.greendome.adhkar.data.model

/** مظهر النافذة المنبثقة (تسبيح أو أذكار) */
data class PopupAppearance(
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val boxWidthFraction: Float = 0.85f,
    val fontScale: Float = 1f
) {
    fun horizontalBias(): Float = positionX.coerceIn(0f, 1f) * 2f - 1f
    fun verticalBias(): Float = positionY.coerceIn(0f, 1f) * 2f - 1f

    companion object {
        const val MIN_BOX_WIDTH = 0.45f
        const val MAX_BOX_WIDTH = 1f
        const val MIN_FONT_SCALE = 0.7f
        const val MAX_FONT_SCALE = 1.8f
        const val MIN_AUTO_DISMISS_SECONDS = 0
        const val MAX_AUTO_DISMISS_SECONDS = 60
        const val TASBIH_DEFAULT_AUTO_DISMISS_SECONDS = 5

        const val TASBIH_DEFAULT_BOX_WIDTH = 0.85f
        const val TASBIH_DEFAULT_FONT_SCALE = 1f

        const val AZKAR_DEFAULT_BOX_WIDTH = 0.92f
        const val AZKAR_DEFAULT_FONT_SCALE = 1.15f
        const val AZKAR_DEFAULT_AUTO_DISMISS_SECONDS = 20
        const val AZKAR_MAX_AUTO_DISMISS_SECONDS = 180
    }
}

enum class PopupSettingsTarget { TASBIH, AZKAR }
