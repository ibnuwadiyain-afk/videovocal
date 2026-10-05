package com.example.dsp

enum class StemMode(val title: String, val description: String) {
    ORIGINAL("Original Audio", "Full mix with master studio EQ & effects"),
    ISOLATE_VOCALS("Vocal Only", "Isolate human vocals & mute background instruments"),
    REMOVE_VOCALS("Karaoke / Instrumental", "Attenuate center vocal track & preserve music"),
    CUSTOM_MIX("Custom Stem Mixer", "Independent Vocal and Instrumental volume faders")
}

enum class NeuralModelArchitecture(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val spectralSharpness: Float,
    val harmonicWeight: Float
) {
    DEMUCS_V4_HT(
        id = "demucs_v4_ht",
        displayName = "Demucs v4 HT",
        subtitle = "Hybrid Transformer • Time + Spectrogram Dual-Domain",
        spectralSharpness = 1.15f,
        harmonicWeight = 0.92f
    ),
    SPLEETER_2STEM(
        id = "spleeter_2stem",
        displayName = "Deezer Spleeter 2-Stem",
        subtitle = "Ultra-Fast U-Net Soft Spectral Masking",
        spectralSharpness = 0.95f,
        harmonicWeight = 0.85f
    ),
    UVR_MDX_NET(
        id = "uvr_mdx_net",
        displayName = "UVR-MDX-Net",
        subtitle = "High-Precision Band-Split Vocal Isolation",
        spectralSharpness = 1.30f,
        harmonicWeight = 0.98f
    )
}

enum class FxTarget(val title: String, val description: String) {
    ALL("All Audio", "Apply Pitch & Tempo to full video sound"),
    VOCALS_ONLY("Vocals Only", "Pitch & Tempo affects only isolated voice"),
    INSTRUMENTAL_ONLY("Music Only", "Pitch & Tempo affects only backing track")
}

enum class DspPerformanceMode(
    val title: String,
    val shortLabel: String,
    val description: String,
    val latencyMs: Int,
    val fftWindowSize: Int
) {
    REAL_TIME_ZERO_LAG(
        title = "Real-Time (Zero-Lag)",
        shortLabel = "Zero-Lag",
        description = "Ultra-low latency processing optimized for immediate A/V playback",
        latencyMs = 6,
        fftWindowSize = 256
    ),
    BALANCED(
        title = "Balanced",
        shortLabel = "Balanced",
        description = "Harmonic-percussive and spectral separation for clear vocal isolation",
        latencyMs = 14,
        fftWindowSize = 512
    ),
    DEEP_QUALITY(
        title = "Deep Quality",
        shortLabel = "Deep Quality",
        description = "High-resolution spectrogram mask separation for maximum instrument suppression",
        latencyMs = 24,
        fftWindowSize = 1024
    ),
    // Aliases for compatibility
    HIGH_QUALITY(
        title = "High Quality (Phase Vocoder + Multi-Band)",
        shortLabel = "High Quality",
        description = "High-resolution spectral & harmonic-percussive separation",
        latencyMs = 16,
        fftWindowSize = 1024
    ),
    FAST_WSOLA(
        title = "Fast Low-Latency (WSOLA Engine)",
        shortLabel = "Fast WSOLA",
        description = "Ultra-low latency processing for immediate playback",
        latencyMs = 8,
        fftWindowSize = 256
    )
}

data class VisualizerFrame(
    val rmsLeft: Float = 0f,
    val rmsRight: Float = 0f,
    val peakLeft: Float = 0f,
    val peakRight: Float = 0f,
    val vocalConfidence: Float = 0f,
    val instrumentSuppressionDb: Float = 0f,
    val spectrumBands: FloatArray = FloatArray(24) { 0f },
    val waveformPoints: FloatArray = FloatArray(32) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerFrame
        if (rmsLeft != other.rmsLeft) return false
        if (rmsRight != other.rmsRight) return false
        if (vocalConfidence != other.vocalConfidence) return false
        if (!spectrumBands.contentEquals(other.spectrumBands)) return false
        return waveformPoints.contentEquals(other.waveformPoints)
    }

    override fun hashCode(): Int {
        var result = rmsLeft.hashCode()
        result = 31 * result + rmsRight.hashCode()
        result = 31 * result + vocalConfidence.hashCode()
        result = 31 * result + spectrumBands.contentHashCode()
        result = 31 * result + waveformPoints.contentHashCode()
        return result
    }
}

data class AudioEngineStats(
    val sampleRate: Int = 44100,
    val channelCount: Int = 2,
    val latencyMs: Int = 8,
    val cpuLoadPercent: Int = 5,
    val cpuThreads: Int = 4,
    val bufferFrames: Int = 512,
    val activeArchitecture: NeuralModelArchitecture = NeuralModelArchitecture.DEMUCS_V4_HT,
    val isRealTimeProcessing: Boolean = true
)

data class OfflineExtractionState(
    val isExtracting: Boolean = false,
    val progressPercent: Int = 0,
    val chunksProcessed: Int = 0,
    val totalChunks: Int = 0,
    val statusMessage: String = "Ready for High-Speed Offline Extraction",
    val extractedWavPath: String? = null
)
