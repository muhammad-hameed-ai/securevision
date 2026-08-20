package com.securevision.core.model

/**
 * A person the app has been taught to recognise — an **enrolled person profile**.
 *
 * Stored on-device only. Neither the photo nor the face embedding is ever
 * uploaded; this entity has no Firebase representation and must not acquire one.
 * Contrast with [UserAccount], the app-login account, which is cloud-backed.
 *
 * @property id Locally generated stable identifier.
 * @property name Display name shown on a green box when this person is matched.
 * @property age Age in years, captured at enrolment.
 * @property photoUri `content://` or `file://` URI of the enrolment photo held in
 *   the app's internal storage.
 * @property embedding L2-normalised embedding of the **aligned** face crop.
 *   Cosine similarity against this vector is what produces a match. Its length is
 *   whatever the model that produced it emits — see [embeddingSize].
 * @property accessLevel How the operator has classified this person. Display
 *   only — recognition treats every enrolled person identically.
 * @property isWatchlisted Whether a sighting of this person should be escalated
 *   rather than treated as a routine known-person event. Orthogonal to
 *   [accessLevel]: a VIP and a restricted person can both be watchlisted, for
 *   opposite reasons.
 * @property createdAt Enrolment time, epoch milliseconds UTC.
 */
class EnrolledProfile(
    val id: String,
    val name: String,
    val age: Int,
    val photoUri: String,
    val embedding: FloatArray,
    val accessLevel: AccessLevel = AccessLevel.DEFAULT,
    val isWatchlisted: Boolean,
    val createdAt: Long,
) {

    /**
     * Dimensionality of this profile's embedding.
     *
     * Read from the vector rather than assumed, because it is a property of the
     * model that produced it. Two embeddings of different lengths came from
     * different models and are not comparable at all — comparing them would
     * produce meaningless similarity scores rather than an obvious failure.
     */
    val embeddingSize: Int get() = embedding.size

    /**
     * Structural equality, with [embedding] compared by content.
     *
     * Hand-written rather than generated: a `data class` compares `FloatArray` by
     * reference, which would make two profiles holding identical embeddings
     * unequal and silently break de-duplication, diffing and test assertions.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnrolledProfile) return false

        return id == other.id &&
            name == other.name &&
            age == other.age &&
            photoUri == other.photoUri &&
            embedding.contentEquals(other.embedding) &&
            accessLevel == other.accessLevel &&
            isWatchlisted == other.isWatchlisted &&
            createdAt == other.createdAt
    }

    /** Mirrors [equals]; [embedding] contributes via [FloatArray.contentHashCode]. */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + age
        result = 31 * result + photoUri.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + accessLevel.hashCode()
        result = 31 * result + isWatchlisted.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    /** Deliberately omits the raw embedding — 512 floats in a log line help nobody. */
    override fun toString(): String =
        "EnrolledProfile(id=$id, name=$name, age=$age, photoUri=$photoUri, " +
            "embedding=${embedding.size} dims, accessLevel=$accessLevel, " +
            "isWatchlisted=$isWatchlisted, createdAt=$createdAt)"

    /**
     * Returns a copy with the given fields replaced, matching the ergonomics of a
     * `data class` without inheriting its reference-based array equality.
     */
    fun copy(
        id: String = this.id,
        name: String = this.name,
        age: Int = this.age,
        photoUri: String = this.photoUri,
        embedding: FloatArray = this.embedding,
        accessLevel: AccessLevel = this.accessLevel,
        isWatchlisted: Boolean = this.isWatchlisted,
        createdAt: Long = this.createdAt,
    ): EnrolledProfile = EnrolledProfile(
        id = id,
        name = name,
        age = age,
        photoUri = photoUri,
        embedding = embedding,
        accessLevel = accessLevel,
        isWatchlisted = isWatchlisted,
        createdAt = createdAt,
    )

    companion object {
        /**
         * Dimensionality of the model currently shipped in `ml-face` assets.
         *
         * A reference value, **not an invariant**. The embedder reads its real
         * output dimension from the loaded model, and [embeddingSize] reports what
         * a given profile actually holds. Treating 512 as a hard rule is how a
         * swapped model turns into silently wrong match scores instead of a clear
         * "re-enrol" message.
         */
        const val SHIPPED_MODEL_EMBEDDING_SIZE: Int = 512
    }
}
