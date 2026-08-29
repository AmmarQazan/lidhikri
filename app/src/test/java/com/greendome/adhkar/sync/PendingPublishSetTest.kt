package com.greendome.adhkar.sync

import com.greendome.adhkar.data.local.PendingPublishChangeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPublishSetTest {

    @Test
    fun emptyDoesNotUploadAudio() {
        val set = PendingPublishSet()
        assertTrue(set.isEmpty)
        assertFalse(set.shouldUploadDhikrAudio(1))
        assertFalse(set.shouldUploadReciterAudio(2))
        assertFalse(set.shouldUploadReciterAzkarAudio(3))
    }

    @Test
    fun uploadsOnlyPendingReciterAudio() {
        val set = PendingPublishSet.from(
            listOf(
                change(PendingPublishType.RECITER, 1L),
                change(PendingPublishType.RECITER_AUDIO, 9L),
                change(PendingPublishType.DHIKR, 4L)
            )
        )
        assertTrue(set.shouldUploadReciterAudio(9))
        assertFalse(set.shouldUploadReciterAudio(8))
        assertTrue(set.shouldUploadDhikrAudio(4))
        assertFalse(set.shouldUploadDhikrAudio(1))
        assertFalse(set.shouldUploadReciterAzkarAudio(9))
        val counts = PendingPublishCounts.from(
            listOf(
                change(PendingPublishType.RECITER, 1L),
                change(PendingPublishType.RECITER_AUDIO, 9L),
                change(PendingPublishType.DHIKR, 4L)
            )
        )
        assertEquals(1, counts.reciters)
        assertEquals(1, counts.audio)
        assertEquals(1, counts.dhikr)
    }

    @Test
    fun deletedAudioIsNotUploaded() {
        val changes = listOf(
            change(
                type = PendingPublishType.RECITER_AUDIO,
                entityId = 9L,
                action = PendingPublishAction.DELETE
            )
        )
        val set = PendingPublishSet.from(changes)
        assertFalse(set.shouldUploadReciterAudio(9))
        val counts = PendingPublishCounts.from(changes)
        assertEquals(1, counts.audio)
        assertFalse(counts.isEmpty)
    }

    private fun change(
        type: String,
        entityId: Long,
        entityKey: String = "",
        action: String = PendingPublishAction.UPSERT
    ) = PendingPublishChangeEntity(
        changeKey = PendingPublishRepository.changeKey(type, entityId, entityKey),
        entityType = type,
        entityId = entityId,
        entityKey = entityKey,
        action = action,
        createdAt = 1L
    )
}
