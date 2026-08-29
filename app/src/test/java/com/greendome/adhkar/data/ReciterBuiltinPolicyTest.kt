package com.greendome.adhkar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterBuiltinPolicyTest {

    @Test
    fun onlySubaihatIsAppDefaultBuiltin() {
        assertTrue(
            ReciterBuiltinPolicy.isAppDefaultBuiltin(
                reciterId = SubaihatReciterSeed.RECITER_ID,
                nameAr = "ناصر الدين صبيحات",
            )
        )
        assertTrue(
            ReciterBuiltinPolicy.isAppDefaultBuiltin(
                reciterId = 99L,
                nameAr = "الشيخ صبيحات",
            )
        )
        assertFalse(
            ReciterBuiltinPolicy.isAppDefaultBuiltin(
                reciterId = ReciterLibrariesMigration.MIXED_VOICES_ID,
                nameAr = "أصوات متنوعة",
            )
        )
        assertFalse(
            ReciterBuiltinPolicy.isAppDefaultBuiltin(
                reciterId = 4L,
                nameAr = "قارئ جديد",
            )
        )
    }

    @Test
    fun adminBuiltinChoiceIsKeptWhenAnyReciterIsBuiltin() {
        assertEquals(
            null,
            ReciterBuiltinPolicy.reciterIdToPromoteAsBuiltin(
                listOf(
                    Triple(4L, "قارئ جديد", true),
                    Triple(SubaihatReciterSeed.RECITER_ID, "ناصر الدين صبيحات", false),
                ),
            ),
        )
        assertEquals(
            null,
            ReciterBuiltinPolicy.reciterIdToPromoteAsBuiltin(
                listOf(
                    Triple(ReciterLibrariesMigration.MIXED_VOICES_ID, "أصوات متنوعة", true),
                    Triple(SubaihatReciterSeed.RECITER_ID, "ناصر الدين صبيحات", false),
                ),
            ),
        )
    }

    @Test
    fun promotesSubaihatOnlyWhenNobodyIsBuiltin() {
        assertEquals(
            SubaihatReciterSeed.RECITER_ID,
            ReciterBuiltinPolicy.reciterIdToPromoteAsBuiltin(
                listOf(
                    Triple(ReciterLibrariesMigration.MIXED_VOICES_ID, "أصوات متنوعة", false),
                    Triple(SubaihatReciterSeed.RECITER_ID, "ناصر الدين صبيحات", false),
                ),
            ),
        )
    }
}
