package com.theveloper.pixelplay.data.audio

/** Version 1 is a deterministic, local-only DSP profile. It contains no tags or file paths. */
data class AudioSimilarityProfile(
    val version: Int,
    val durationMs: Long,
    /** Loudness-relative RMS across 32 equal-duration bins, centered and scaled per track. */
    val energy: FloatArray,
    /** Mean normalized energy in eight logarithmic frequency bands. */
    val timbre: FloatArray,
    /** Estimated beats per minute, or null when the onset signal is not reliable. */
    val tempoBpm: Float?,
    val tempoConfidence: Float
) {
    override fun equals(other: Any?): Boolean = other is AudioSimilarityProfile &&
        version == other.version && durationMs == other.durationMs &&
        energy.contentEquals(other.energy) && timbre.contentEquals(other.timbre) &&
        tempoBpm == other.tempoBpm && tempoConfidence == other.tempoConfidence

    override fun hashCode(): Int = 31 * version + energy.contentHashCode() + timbre.contentHashCode()
}

data class AudioSimilarityResult(val score: Float, val energyScore: Float, val timbreScore: Float, val rhythmScore: Float)

object AudioSimilarity {
    const val PROFILE_VERSION = 1
    const val ENERGY_BINS = 32
    const val TIMBRE_BANDS = 8

    fun compare(a: AudioSimilarityProfile, b: AudioSimilarityProfile): AudioSimilarityResult? {
        if (a.version != PROFILE_VERSION || b.version != PROFILE_VERSION ||
            a.energy.size != ENERGY_BINS || b.energy.size != ENERGY_BINS ||
            a.timbre.size != TIMBRE_BANDS || b.timbre.size != TIMBRE_BANDS) return null

        val energyDistance = meanAbsDistance(a.energy, b.energy)
        val energyScore = (1f - energyDistance / 2f).coerceIn(0f, 1f)
        val timbreScore = cosineSimilarity(a.timbre, b.timbre)
        val rhythmScore = when {
            a.tempoBpm == null || b.tempoBpm == null -> 0.5f
            else -> {
                val delta = minOf(
                    kotlin.math.abs(a.tempoBpm - b.tempoBpm),
                    kotlin.math.abs(a.tempoBpm * 2f - b.tempoBpm),
                    kotlin.math.abs(a.tempoBpm - b.tempoBpm * 2f),
                    kotlin.math.abs(a.tempoBpm / 2f - b.tempoBpm),
                    kotlin.math.abs(a.tempoBpm - b.tempoBpm / 2f)
                )
                (1f - delta / 45f).coerceIn(0f, 1f)
            }
        }
        val rhythmWeight = if (a.tempoBpm != null && b.tempoBpm != null) {
            0.15f * minOf(a.tempoConfidence, b.tempoConfidence).coerceIn(0f, 1f)
        } else 0f
        val score = (energyScore * 0.35f + timbreScore * (0.65f - rhythmWeight) + rhythmScore * rhythmWeight)
            .coerceIn(0f, 1f)
        return AudioSimilarityResult(score, energyScore, timbreScore, rhythmScore)
    }

    private fun meanAbsDistance(a: FloatArray, b: FloatArray): Float =
        a.indices.map { kotlin.math.abs(a[it] - b[it]) }.average().toFloat()

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) { dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i] }
        if (normA <= 1e-12f || normB <= 1e-12f) return 0.5f
        return ((dot / kotlin.math.sqrt(normA * normB)) + 1f).coerceIn(0f, 2f) / 2f
    }
}
