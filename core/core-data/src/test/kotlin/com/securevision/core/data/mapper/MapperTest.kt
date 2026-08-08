package com.securevision.core.data.mapper

import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.DetectionEvent
import com.securevision.core.model.EnrolledProfile
import com.securevision.core.model.Recording
import com.securevision.core.model.Severity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Entity ⇄ Model conversions, in both directions, for every table. */
class MapperTest {

    @Test
    fun `enrolled profile survives a model to entity to model round trip`() {
        val original = EnrolledProfile(
            id = "profile-1",
            name = "Ayesha Khan",
            age = 31,
            photoUri = "file:///data/user/0/com.securevision/files/profiles/profile_1.jpg",
            embedding = FloatArray(EnrolledProfile.EMBEDDING_SIZE) { it * 0.001f },
            isWatchlisted = true,
            createdAt = 1_754_000_000_000L,
        )

        val restored = original.toEntity().toDomain()

        // EnrolledProfile compares embeddings by content, so this covers the array too.
        assertEquals(original, restored)
        assertArrayEquals(original.embedding, restored.embedding, 0f)
    }

    @Test
    fun `alert survives a round trip with every nullable field populated`() {
        val original = alert(snapshotUri = "file:///snapshots/a.jpg", hasBeard = true, hasMask = false)

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `alert preserves nulls rather than coercing them to defaults`() {
        val original = alert(snapshotUri = null, hasBeard = null, hasMask = null)

        val restored = original.toEntity().toDomain()

        // null means "no face was analysed", which is a different claim from false.
        // Coercing it would make a notification assert something it does not know.
        assertNull(restored.snapshotUri)
        assertNull(restored.hasBeard)
        assertNull(restored.hasMask)
        assertEquals(original, restored)
    }

    @Test
    fun `alert preserves enum identity across the string column`() {
        AlertType.entries.forEach { type ->
            Severity.entries.forEach { severity ->
                val original = alert(type = type, severity = severity)
                val restored = original.toEntity().toDomain()

                assertEquals(type, restored.type)
                assertEquals(severity, restored.severity)
            }
        }
    }

    @Test
    fun `detection event survives a round trip`() {
        val original = DetectionEvent(
            id = "event-1",
            type = AlertType.MOTION,
            label = "Movement in frame",
            confidence = 0.42f,
            cameraFacing = "back",
            timestamp = 1_754_000_111_000L,
        )

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `recording survives a round trip including a null thumbnail`() {
        val withThumbnail = Recording(
            id = "rec-1",
            filePath = "/data/user/0/com.securevision/files/recordings/recording_1.mp4",
            durationMs = 65_000L,
            thumbnailUri = "file:///recordings/recording_1.jpg",
            createdAt = 1_754_000_222_000L,
        )
        val withoutThumbnail = withThumbnail.copy(id = "rec-2", thumbnailUri = null)

        assertEquals(withThumbnail, withThumbnail.toEntity().toDomain())
        assertEquals(withoutThumbnail, withoutThumbnail.toEntity().toDomain())
        assertNull(withoutThumbnail.toEntity().toDomain().thumbnailUri)
    }

    @Test
    fun `list mappers preserve order`() {
        val alerts = listOf(alert(id = "a"), alert(id = "b"), alert(id = "c"))

        val restored = alerts.map(AlertRecord::toEntity).toDomain()

        assertEquals(listOf("a", "b", "c"), restored.map(AlertRecord::id))
    }

    private fun alert(
        id: String = "alert-1",
        type: AlertType = AlertType.UNKNOWN_PERSON,
        severity: Severity = Severity.HIGH,
        snapshotUri: String? = "file:///snapshots/a.jpg",
        hasBeard: Boolean? = true,
        hasMask: Boolean? = false,
    ) = AlertRecord(
        id = id,
        type = type,
        severity = severity,
        confidence = 0.87f,
        cameraFacing = "front",
        snapshotUri = snapshotUri,
        hasBeard = hasBeard,
        hasMask = hasMask,
        timestamp = 1_754_000_333_000L,
        isRead = false,
    )
}
