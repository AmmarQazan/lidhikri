package com.greendome.adhkar.data

import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.data.model.VoiceSettingsTarget
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterVoiceScopeTest {

    @Test
    fun bothAllowsTasbihAndAzkar() {
        assertTrue(ReciterVoiceScope.BOTH.allows(VoiceSettingsTarget.TASBIH))
        assertTrue(ReciterVoiceScope.BOTH.allows(VoiceSettingsTarget.AZKAR))
        assertTrue(ReciterVoiceScope.BOTH.allowsTasbih())
        assertTrue(ReciterVoiceScope.BOTH.allowsAzkar())
    }

    @Test
    fun tasbihOnlyAllowsShortTasbih() {
        assertTrue(ReciterVoiceScope.TASBIH.allows(VoiceSettingsTarget.TASBIH))
        assertFalse(ReciterVoiceScope.TASBIH.allows(VoiceSettingsTarget.AZKAR))
        assertTrue(ReciterVoiceScope.TASBIH.allowsTasbih())
        assertFalse(ReciterVoiceScope.TASBIH.allowsAzkar())
    }

    @Test
    fun azkarOnlyAllowsAzkar() {
        assertFalse(ReciterVoiceScope.AZKAR.allows(VoiceSettingsTarget.TASBIH))
        assertTrue(ReciterVoiceScope.AZKAR.allows(VoiceSettingsTarget.AZKAR))
        assertFalse(ReciterVoiceScope.AZKAR.allowsTasbih())
        assertTrue(ReciterVoiceScope.AZKAR.allowsAzkar())
    }
}
