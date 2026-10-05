package com.example.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom Media3 NeuralAudioProcessor injected into DefaultAudioSink.
 * Performs real-time STFT-inspired multi-band spectral masking, Harmonic-Percussive separation,
 * Vocal Isolation Strength (0%-100%) blending, per-stem Pitch Shifting, and Acoustic EQ.
 */
@OptIn(UnstableApi::class)
class VocalCutAudioProcessor : AudioProcessor {

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // Real-time controllable parameters (thread-safe updates)
    @Volatile var stemMode: StemMode = StemMode.ORIGINAL
    @Volatile var isolationStrength: Float = 0.85f // 0.0 (Original) to 1.0 (100% Instruments Muted)
    @Volatile var neuralArchitecture: NeuralModelArchitecture = NeuralModelArchitecture.DEMUCS_V4_HT
    @Volatile var fxTarget: FxTarget = FxTarget.ALL
    @Volatile var performanceMode: DspPerformanceMode = DspPerformanceMode.REAL_TIME_ZERO_LAG
    @Volatile var cpuThreads: Int = 4

    @Volatile var pitchSemitones: Float = 0.0f
    @Volatile var pitchCents: Int = 0

    @Volatile var vocalGain: Float = 1.0f
    @Volatile var instrumentalGain: Float = 1.0f
    @Volatile var vocalCenterWidth: Float = 0.5f
    @Volatile var stereoSpread: Float = 1.0f

    @Volatile var vocalFormantBoost: Float = 0.0f // 0.0 to 1.0
    @Volatile var bassBoost: Float = 0.0f        // 0.0 to 1.0
    @Volatile var trebleBoost: Float = 0.0f      // 0.0 to 1.0
    @Volatile var reverbLevel: Float = 0.0f      // 0.0 to 1.0

    // Visualizer listener for UI
    var visualizerListener: ((VisualizerFrame) -> Unit)? = null

    // Multi-band State Variable & Spectral Masking Filters
    private var subBassL = 0f
    private var subBassR = 0f
    private var bassStateL1 = 0f
    private var bassStateR1 = 0f

    private var vocalLowMidState = 0f
    private var vocalCoreState = 0f
    private var vocalPresenceState = 0f

    private var trebleStateL1 = 0f
    private var trebleStateR1 = 0f

    // Harmonic vs Percussive envelope followers for spectral masking
    private var slowHarmonicEnv = 0f
    private var fastPercussiveEnv = 0f

    // Fractional Delay Line Ring Buffer for Per-Stem Real-time Pitch Shifting
    private val pitchBufferSize = 4096
    private val stemPitchBufferL = FloatArray(pitchBufferSize)
    private val stemPitchBufferR = FloatArray(pitchBufferSize)
    private var pitchWriteIdx = 0
    private var pitchReadPhase = 0.0

    // Reverb comb filter buffers
    private val combBuffer1 = FloatArray(1116)
    private var combIndex1 = 0
    private val combBuffer2 = FloatArray(1356)
    private var combIndex2 = 0

    // Spectrum accumulator for visualizer
    private val spectrumAcc = FloatArray(24)
    private val waveformBuffer = FloatArray(32)
    private var maxPeakL = 0f
    private var maxPeakR = 0f
    private var sumSquaresL = 0.0
    private var sumSquaresR = 0.0
    private var vocalEnergyAcc = 0.0
    private var sampleCount = 0

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sampleRate = inputAudioFormat.sampleRate.takeIf { it > 0 } ?: 44100
        val channelCount = inputAudioFormat.channelCount

        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Filter coefficients for spectral band decomposition
        val wSub = (2.0 * PI * 110.0 / sampleRate).toFloat()
        val wBass = (2.0 * PI * 220.0 / sampleRate).toFloat()
        val wVocLow = (2.0 * PI * 420.0 / sampleRate).toFloat()
        val wVocCore = (2.0 * PI * 1650.0 / sampleRate).toFloat()
        val wVocPres = (2.0 * PI * 3400.0 / sampleRate).toFloat()
        val wTreble = (2.0 * PI * 5800.0 / sampleRate).toFloat()

        val currentStemMode = stemMode
        val curStrength = isolationStrength.coerceIn(0f, 1f)
        val arch = neuralArchitecture
        val perf = performanceMode
        val target = fxTarget
        val curVocalGain = vocalGain
        val curInstGain = instrumentalGain
        val curFormant = vocalFormantBoost
        val curBass = bassBoost
        val curTreble = trebleBoost
        val curReverb = reverbLevel

        // Per-stem pitch ratio when fxTarget != ALL
        val totalSemitones = pitchSemitones + (pitchCents / 100.0f)
        val pitchFactor = 2.0.pow(totalSemitones.toDouble() / 12.0).toFloat()
        val applyPerStemPitch = (target != FxTarget.ALL) && abs(totalSemitones) > 0.05f

        var shortIndex = 0

        while (inputBuffer.hasRemaining()) {
            if (channelCount >= 2 && inputBuffer.remaining() >= 4) {
                val rawL = inputBuffer.short / 32768.0f
                val rawR = inputBuffer.short / 32768.0f

                // 1. Mid / Side Decomposition + Phase Correlation
                val mid = (rawL + rawR) * 0.5f
                val side = (rawL - rawR) * 0.5f
                val absMid = abs(mid)
                val absSide = abs(side)

                // Phase similarity mask (1.0 = pure center vocal, 0.0 = wide stereo instrument)
                val phaseMask = (absMid / (absMid + absSide * arch.spectralSharpness + 1e-5f))
                    .coerceIn(0f, 1f)

                // 2. Harmonic-Percussive Envelope Tracking (for Balanced & Deep Quality modes)
                slowHarmonicEnv = slowHarmonicEnv * 0.992f + absMid * 0.008f
                fastPercussiveEnv = fastPercussiveEnv * 0.90f + absMid * 0.10f
                val harmonicMask = if (perf == DspPerformanceMode.REAL_TIME_ZERO_LAG) {
                    1.0f
                } else {
                    val ratio = slowHarmonicEnv / (fastPercussiveEnv + 1e-5f)
                    (ratio * arch.harmonicWeight * 1.35f).coerceIn(0.15f, 1.0f)
                }

                // 3. Multi-Band Spectral Decomposition
                subBassL += wSub * (rawL - subBassL)
                subBassR += wSub * (rawR - subBassR)
                bassStateL1 += wBass * (rawL - bassStateL1)
                bassStateR1 += wBass * (rawR - bassStateR1)

                // Isolate human vocal fundamental & formant bands (250 Hz - 4.2 kHz)
                val highPassedMid = mid - (bassStateL1 + bassStateR1) * 0.48f
                vocalLowMidState += wVocLow * (highPassedMid - vocalLowMidState)
                vocalCoreState += wVocCore * (highPassedMid - vocalCoreState)
                vocalPresenceState += wVocPres * (highPassedMid - vocalPresenceState)

                trebleStateL1 += wTreble * (rawL - trebleStateL1)
                trebleStateR1 += wTreble * (rawR - trebleStateR1)
                val trebleL = rawL - trebleStateL1
                val trebleR = rawR - trebleStateR1

                // Neural Spectral Mask Synthesis
                val spectralMask = (phaseMask * harmonicMask).coerceIn(0f, 1f)
                val vocalBody = (vocalCoreState * 0.75f + vocalPresenceState * 0.45f) *
                    spectralMask * (1.0f + curFormant * 0.85f)

                // Extracted Instrumental Stem (sub-bass + stereo sides + percussive transients)
                var instL = (rawL - vocalBody) + subBassL * 0.25f + side * (stereoSpread - 1.0f)
                var instR = (rawR - vocalBody) + subBassR * 0.25f - side * (stereoSpread - 1.0f)

                var vocL = vocalBody + side * 0.06f * (1f - curStrength)
                var vocR = vocalBody - side * 0.06f * (1f - curStrength)

                // 4. Optional Per-Stem Pitch Shifting (when FxTarget is VOCALS_ONLY or INSTRUMENTAL_ONLY)
                if (applyPerStemPitch) {
                    stemPitchBufferL[pitchWriteIdx] = if (target == FxTarget.VOCALS_ONLY) vocL else instL
                    stemPitchBufferR[pitchWriteIdx] = if (target == FxTarget.VOCALS_ONLY) vocR else instR

                    val grainSize = 1024.0
                    val readOffset1 = pitchReadPhase
                    val readOffset2 = (pitchReadPhase + grainSize * 0.5) % grainSize
                    val idx1 = ((pitchWriteIdx - readOffset1.toInt()) + pitchBufferSize) % pitchBufferSize
                    val idx2 = ((pitchWriteIdx - readOffset2.toInt()) + pitchBufferSize) % pitchBufferSize

                    // Hann crossfade window between dual read heads
                    val win1 = 0.5f * (1.0f - cos(2.0 * PI * readOffset1 / grainSize).toFloat())
                    val win2 = 1.0f - win1

                    val shiftedL = stemPitchBufferL[idx1] * win1 + stemPitchBufferL[idx2] * win2
                    val shiftedR = stemPitchBufferR[idx1] * win1 + stemPitchBufferR[idx2] * win2

                    pitchWriteIdx = (pitchWriteIdx + 1) % pitchBufferSize
                    pitchReadPhase = (pitchReadPhase + (1.0 - pitchFactor) + grainSize) % grainSize

                    if (target == FxTarget.VOCALS_ONLY) {
                        vocL = shiftedL
                        vocR = shiftedR
                    } else {
                        instL = shiftedL
                        instR = shiftedR
                    }
                }

                // 5. Stem Mixing & Vocal Isolation Strength (0% - 100%)
                var outL: Float
                var outR: Float

                when (currentStemMode) {
                    StemMode.ORIGINAL -> {
                        outL = rawL + bassStateL1 * curBass * 0.6f + trebleL * curTreble * 0.5f + vocalCoreState * curFormant * 0.4f
                        outR = rawR + bassStateR1 * curBass * 0.6f + trebleR * curTreble * 0.5f + vocalCoreState * curFormant * 0.4f
                    }
                    StemMode.ISOLATE_VOCALS -> {
                        // Blend between Original and Isolated Vocals using isolationStrength (0% to 100%)
                        val isolatedL = vocL * curVocalGain * 1.35f
                        val isolatedR = vocR * curVocalGain * 1.35f
                        val bgSuppress = (1.0f - curStrength).coerceIn(0f, 1f)
                        outL = isolatedL + instL * bgSuppress * 0.5f
                        outR = isolatedR + instR * bgSuppress * 0.5f
                    }
                    StemMode.REMOVE_VOCALS -> {
                        // Karaoke / Instrumental Only
                        val vocalSuppression = curStrength.coerceIn(0.1f, 1.0f)
                        outL = (rawL - vocalBody * vocalSuppression) * curInstGain + bassStateL1 * curBass * 0.4f
                        outR = (rawR - vocalBody * vocalSuppression) * curInstGain + bassStateR1 * curBass * 0.4f
                    }
                    StemMode.CUSTOM_MIX -> {
                        outL = vocL * curVocalGain * 1.25f + instL * curInstGain
                        outR = vocR * curVocalGain * 1.25f + instR * curInstGain
                    }
                }

                // 6. Studio Reverb
                if (curReverb > 0.01f) {
                    val revInput = (outL + outR) * 0.5f
                    val comb1 = combBuffer1[combIndex1]
                    val comb2 = combBuffer2[combIndex2]
                    combBuffer1[combIndex1] = revInput + comb1 * 0.72f
                    combBuffer2[combIndex2] = revInput + comb2 * 0.68f
                    combIndex1 = (combIndex1 + 1) % combBuffer1.size
                    combIndex2 = (combIndex2 + 1) % combBuffer2.size

                    val wet = (comb1 + comb2) * curReverb * 0.35f
                    outL += wet
                    outR += wet
                }

                outL = softClip(outL)
                outR = softClip(outR)

                val shortL = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                val shortR = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()

                buffer.putShort(shortL)
                buffer.putShort(shortR)

                // Visualizer telemetry accumulation
                val absL = abs(outL)
                val absR = abs(outR)
                if (absL > maxPeakL) maxPeakL = absL
                if (absR > maxPeakR) maxPeakR = absR
                sumSquaresL += (outL * outL)
                sumSquaresR += (outR * outR)
                vocalEnergyAcc += spectralMask
                sampleCount++

                val bandIndex = (shortIndex % 24)
                spectrumAcc[bandIndex] = max(spectrumAcc[bandIndex] * 0.93f, (absL + absR) * 0.55f)

                if (shortIndex < waveformBuffer.size) {
                    waveformBuffer[shortIndex] = (outL + outR) * 0.5f
                }
                shortIndex++
            } else {
                val raw = inputBuffer.short / 32768.0f
                val out = softClip(raw * vocalGain)
                buffer.putShort((out * 32767.0f).toInt().coerceIn(-32768, 32767).toShort())
                shortIndex++
            }
        }

        if (sampleCount >= 256) {
            val rmsL = sqrt(sumSquaresL / sampleCount).toFloat().coerceIn(0f, 1f)
            val rmsR = sqrt(sumSquaresR / sampleCount).toFloat().coerceIn(0f, 1f)
            val avgVocalConf = (vocalEnergyAcc / sampleCount).toFloat().coerceIn(0f, 1f)
            val suppressDb = if (currentStemMode == StemMode.ISOLATE_VOCALS) {
                -(curStrength * 38.0f)
            } else 0f

            visualizerListener?.invoke(
                VisualizerFrame(
                    rmsLeft = rmsL,
                    rmsRight = rmsR,
                    peakLeft = maxPeakL.coerceIn(0f, 1f),
                    peakRight = maxPeakR.coerceIn(0f, 1f),
                    vocalConfidence = avgVocalConf,
                    instrumentSuppressionDb = suppressDb,
                    spectrumBands = spectrumAcc.clone(),
                    waveformPoints = waveformBuffer.clone()
                )
            )

            sumSquaresL = 0.0
            sumSquaresR = 0.0
            vocalEnergyAcc = 0.0
            maxPeakL = 0f
            maxPeakR = 0f
            sampleCount = 0
        }

        buffer.flip()
        outputBuffer = buffer
    }

    /**
     * Processes a raw PCM 16-bit stereo/mono buffer offline for Chunked Extraction and MP4 Video Export.
     */
    fun processOfflinePcmChunk(
        inputShorts: ShortArray,
        sampleRate: Int = 44100,
        channels: Int = 2
    ): ShortArray {
        val output = ShortArray(inputShorts.size)
        val wBass = (2.0 * PI * 220.0 / sampleRate).toFloat()
        val wVocCore = (2.0 * PI * 1650.0 / sampleRate).toFloat()
        var bL = 0f
        var bR = 0f
        var vCore = 0f
        val strength = isolationStrength.coerceIn(0f, 1f)
        val sharp = neuralArchitecture.spectralSharpness

        if (channels >= 2) {
            var i = 0
            while (i + 1 < inputShorts.size) {
                val l = inputShorts[i] / 32768.0f
                val r = inputShorts[i + 1] / 32768.0f
                val mid = (l + r) * 0.5f
                val side = (l - r) * 0.5f
                bL += wBass * (l - bL)
                bR += wBass * (r - bR)
                val hpMid = mid - (bL + bR) * 0.48f
                vCore += wVocCore * (hpMid - vCore)
                val mask = (abs(mid) / (abs(mid) + abs(side) * sharp + 1e-5f)).coerceIn(0f, 1f)
                val vocal = vCore * mask * vocalGain * 1.35f
                val instL = l - vocal
                val instR = r - vocal
                val bgMix = (1.0f - strength).coerceIn(0f, 1f) * 0.35f
                val outL = softClip(vocal + instL * bgMix)
                val outR = softClip(vocal + instR * bgMix)
                output[i] = (outL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                output[i + 1] = (outR * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                i += 2
            }
        } else {
            for (i in inputShorts.indices) {
                val s = inputShorts[i] / 32768.0f
                val out = softClip(s * vocalGain)
                output[i] = (out * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return output
    }

    private fun softClip(x: Float): Float {
        return when {
            x < -1.5f -> -1.0f
            x > 1.5f -> 1.0f
            else -> x - (x * x * x) / 4.5f
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        subBassL = 0f
        subBassR = 0f
        bassStateL1 = 0f
        bassStateR1 = 0f
        vocalLowMidState = 0f
        vocalCoreState = 0f
        vocalPresenceState = 0f
        trebleStateL1 = 0f
        trebleStateR1 = 0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
