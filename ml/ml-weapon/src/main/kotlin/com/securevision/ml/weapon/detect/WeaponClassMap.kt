package com.securevision.ml.weapon.detect

/**
 * Maps the detector's class indices onto weapon type names.
 *
 * The order must match the `names` list the model was exported with. A mismatch
 * does not throw — it silently labels a knife as a rifle, which is why the loader
 * logs the expected class count and refuses a model whose output implies a
 * different one.
 */
object WeaponClassMap {

    /**
     * Class labels in export order, for a single-class weapons model.
     *
     * The shipped export detects one class rather than distinguishing gun from
     * knife from rifle. That is a deliberate trade: the app asks one question —
     * is there a weapon in frame — and a wrong subtype printed on a CRITICAL
     * alarm is worse than no subtype at all.
     *
     * Change this list and [EXPECTED_CLASS_COUNT] together if you export a model
     * with different classes. The loader refuses any model whose output channel
     * count disagrees with this list rather than mislabelling detections.
     */
    val LABELS: List<String> = listOf("Weapon")

    /** How many classes the loaded model must emit for [LABELS] to be valid. */
    val EXPECTED_CLASS_COUNT: Int = LABELS.size

    /**
     * Resolves a class index.
     *
     * @param index Class index from the model.
     * @return The label, or `null` when the index is outside [LABELS] — which
     *   means the model does not match this map and the detection is discarded
     *   rather than mislabelled.
     */
    fun labelFor(index: Int): String? = LABELS.getOrNull(index)
}
