package com.example.dsp

enum class StemMode(val title: String, val description: String) {
    ORIGINAL("Full Mix", "Original video audio with master effects"),
    ISOLATE_VOCALS("Isolate Vocals", "Extract human vocal stem & dialogue"),
    REMOVE_VOCALS("Karaoke / Instrumental", "Attenuate center vocal track"),
    CUSTOM_MIX("Custom Stem Mixer", "Independent Vocal and Instrumental volume faders")
}

enum class FxTarget(val title: String, val description: String) {
    ALL("All Audio", "Apply Pitch & Tempo to full video sound"),
    VOCALS_ONLY("Vocals Only", "Pitch & Tempo affects only isolated voice"),
    INSTRUMENTAL_ONLY("Music Only", "Pitch & Tempo affects only backing track")
}

enum class DspPerformanceMode(val title: String, val latency: String) {
    HIGH_QUALITY("High Quality (Phase Vocoder + Multi-Band)", "< 25 ms"),
    FAST_WSOLA("Fast Low-Latency (WSOLA Engine)", "< 10 ms")
}

data class VisualizerFrame(
    val rmsLeft: Float = 0f,
    val rmsRight: Float = 0f,
    val peakLeft: Float = 0f,
    val peakRight: Float = 0f,
    val spectrumBands: FloatArray = FloatArray(24) { 0f },
    val waveformPoints: FloatArray = FloatArray(32) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerFrame
        if (rmsLeft != other.rmsLeft) return false
        if (rmsRight != other.rmsRight) return false
        if (!spectrumBands.contentEquals(other.spectrumBands)) return false
        return waveformPoints.contentEquals(other.waveformPoints)
    }

    override fun hashCode(): Int {
        var result = rmsLeft.hashCode()
        result = 31 * result + rmsRight.hashCode()
        result = 31 * result + spectrumBands.contentHashCode()
        result = 31 * result + waveformPoints.contentHashCode()
        return result
    }
}

data class AudioEngineStats(
    val sampleRate: Int = 44100,
    val channelCount: Int = 2,
    val latencyMs: Int = 14,
    val cpuLoadPercent: Int = 4,
    val bufferFrames: Int = 512,
    val isRealTimeProcessing: Boolean = true
)
