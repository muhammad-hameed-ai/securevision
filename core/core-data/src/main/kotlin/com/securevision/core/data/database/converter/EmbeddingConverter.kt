package com.securevision.core.data.database.converter

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Persists a FaceNet-512 embedding as a BLOB.
 *
 * Byte order is pinned to [ByteOrder.LITTLE_ENDIAN] rather than left to
 * `ByteBuffer`'s platform default, which is big-endian on the JVM but follows
 * the hardware elsewhere. Without pinning, a database written on one
 * architecture would decode to different floats on another — and the symptom
 * would not be a crash, it would be recognition quietly failing for every
 * enrolled person.
 *
 * The conversion is exact in both directions: [Float] bits are copied verbatim,
 * never rounded or re-quantised.
 */
class EmbeddingConverter {

    /**
     * Encodes an embedding for storage.
     *
     * @param embedding The vector to encode.
     * @return Four bytes per float, little-endian.
     */
    @TypeConverter
    fun fromFloatArray(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer
            .allocate(embedding.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)

        embedding.forEach(buffer::putFloat)

        return buffer.array()
    }

    /**
     * Decodes a stored embedding.
     *
     * @param bytes The BLOB read from the database.
     * @return The original vector.
     * @throws IllegalArgumentException if the blob length is not a multiple of
     *   four, which would mean the column holds something that is not an
     *   embedding.
     */
    @TypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) {
            "Embedding blob length ${bytes.size} is not a multiple of ${Float.SIZE_BYTES}"
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.getFloat() }
    }
}
