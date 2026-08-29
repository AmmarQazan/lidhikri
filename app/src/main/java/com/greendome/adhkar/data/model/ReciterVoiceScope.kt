package com.greendome.adhkar.data.model

/** أين يظهر القارئ: التسبيحات القصيرة، الأذكار، أو كلاهما */
enum class ReciterVoiceScope {
    BOTH,
    TASBIH,
    AZKAR;

    fun allowsTasbih(): Boolean = this != AZKAR

    fun allowsAzkar(): Boolean = this != TASBIH

    fun allows(target: VoiceSettingsTarget): Boolean = when (target) {
        VoiceSettingsTarget.TASBIH -> allowsTasbih()
        VoiceSettingsTarget.AZKAR -> allowsAzkar()
    }
}
