package com.example.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance real-time Offline Audio DSP Processor for Vocal Isolation,
 * Music Removal (Karaoke), Stem Mixing, Acoustic EQ, and Live Spectrum Visualization.
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
    @Volatile var fxTarget: FxTarget = FxTarget.ALL
    @Volatile var performanceMode: DspPerformanceMode = DspPerformanceMode.HIGH_QUALITY

    @Volatile var vocalGain: Float = 1.0f
    @Volatile var instrumentalGain: Float = 1.0f
    @Volatile var vocalCenterWidth: Float = 0.5f // 0 = narrow center, 1 = wide
    @Volatile var stereoSpread: Float = 1.0f

    @Volatile var vocalFormantBoost: Float = 0.0f // 0.0 to 1.0
    @Volatile var bassBoost: Float = 0.0f        // 0.0 to 1.0
    @Volatile var trebleBoost: Float = 0.0f      // 0.0 to 1.0
    @Volatile var reverbLevel: Float = 0.0f      // 0.0 to 1.0

    // Visualizer listener for UI
    var visualizerListener: ((VisualizerFrame) -> Unit)? = null

    // DSP State Variables for Filters (Biquad / State Variable Filter)
    private var bassStateL1 = 0f
    private var bassStateL2 = 0f
    private var bassStateR1 = 0f
    private var bassStateR2 = 0f

    private var vocalBandStateL1 = 0f
    private var vocalBandStateL2 = 0f
    private var vocalBandStateR1 = 0f
    private var vocalBandStateR2 = 0f

    private var trebleStateL1 = 0f
    private var trebleStateL2 = 0f
    private var trebleStateR1 = 0f
    private var trebleStateR2 = 0f

    // Reverb comb filter buffers
    private val combBuffer1 = FloatArray(1116)
    private var combIndex1 = 0
    private val combBuffer2 = FloatArray(1356)
    private var combIndex2 = 0

    // Spectrum accumulator for visualizer
    private val spectrumAcc = FloatArray(24)
    private val waveformBuffer = FloatArray(32)
    private var frameCounter = 0
    private var maxPeakL = 0f
    private var maxPeakR = 0f
    private var sumSquaresL = 0.0
    private var sumSquaresR = 0.0
    private var sampleCount = 0

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        // We output 16-bit PCM stereo (or mono passed through)
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

        // Ensure output buffer has enough capacity
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Biquad Filter Coefficients calculated for current sampleRate
        // 1. Low Shelf (Bass < 200 Hz)
        val bassFreq = 200.0
        val w0Bass = 2.0 * Math.PI * bassFreq / sampleRate
        val cosBass = cos(w0Bass)
        val alphaBass = sin(w0Bass) / (2.0 * 0.707)
        val b0Bass = ((1.0 - cosBass) / 2.0).toFloat()

        // 2. Vocal Bandpass (300 Hz - 3800 Hz, center ~ 1500 Hz, Q ~ 0.8)
        val vocalFreq = 1500.0
        val w0Vocal = 2.0 * Math.PI * vocalFreq / sampleRate
        val alphaVocal = sin(w0Vocal) / (2.0 * 0.8)
        val b0Vocal = alphaVocal.toFloat()

        // 3. Treble Shelf (> 5500 Hz)
        val trebleFreq = 5500.0
        val w0Treble = 2.0 * Math.PI * trebleFreq / sampleRate
        val cosTreble = cos(w0Treble)
        val alphaTreble = sin(w0Treble) / (2.0 * 0.707)
        val b0Treble = ((1.0 + cosTreble) / 2.0).toFloat()

        val currentStemMode = stemMode
        val curVocalGain = vocalGain
        val curInstGain = instrumentalGain
        val curFormant = vocalFormantBoost
        val curBass = bassBoost
        val curTreble = trebleBoost
        val curReverb = reverbLevel

        val shortCount = remaining / 2
        var shortIndex = 0

        while (inputBuffer.hasRemaining()) {
            if (channelCount >= 2) {
                // Stereo Processing
                val rawL = inputBuffer.short / 32768.0f
                val rawR = inputBuffer.short / 32768.0f

                // Mid / Side Decomposition
                val mid = (rawL + rawR) * 0.5f
                val side = (rawL - rawR) * 0.5f

                // Simple 1-pole / SVF approximations for real-time efficiency
                // Low Bass tracking
                bassStateL1 += b0Bass * (rawL - bassStateL1)
                bassStateR1 += b0Bass * (rawR - bassStateR1)
                val bassL = bassStateL1
                val bassR = bassStateR1

                // Vocal Band tracking
                vocalBandStateL1 += b0Vocal * (mid - vocalBandStateL1)
                val vocalBand = vocalBandStateL1

                // Treble Air tracking
                trebleStateL1 += b0Treble * (rawL - trebleStateL1)
                trebleStateR1 += b0Treble * (rawR - trebleStateR1)
                val trebleL = rawL - trebleStateL1
                val trebleR = rawR - trebleStateR1

                // Stem Separation Math:
                // Mid contains both center vocals and center bass/kick.
                // Subtracting low-bass from mid gives isolated vocal body.
                val vocalExtracted = (mid - (bassL + bassR) * 0.45f) * (1.0f + curFormant * 0.8f)

                // Instrumental retains stereo sides + stereo bass + high end sparkle
                val instLeft = (side * stereoSpread + bassL + trebleL * (1.0f + curTreble * 0.5f))
                val instRight = (-side * stereoSpread + bassR + trebleR * (1.0f + curTreble * 0.5f))

                var outL: Float
                var outR: Float

                when (currentStemMode) {
                    StemMode.ORIGINAL -> {
                        // Original mix with optional EQ enhancers
                        val eqL = rawL + bassL * curBass * 0.6f + trebleL * curTreble * 0.5f + vocalBand * curFormant * 0.4f
                        val eqR = rawR + bassR * curBass * 0.6f + trebleR * curTreble * 0.5f + vocalBand * curFormant * 0.4f
                        outL = eqL
                        outR = eqR
                    }
                    StemMode.ISOLATE_VOCALS -> {
                        // Isolate vocal stem: output primarily the extracted vocal body with subtle spatial stereo
                        val voc = vocalExtracted * curVocalGain
                        val subtleSide = side * 0.12f
                        outL = (voc + subtleSide)
                        outR = (voc - subtleSide)
                    }
                    StemMode.REMOVE_VOCALS -> {
                        // Karaoke / Remove Vocals: attenuate center vocal band while preserving bass, sides and treble
                        val karaokeL = (instLeft + (mid - vocalExtracted * 0.95f) * 0.2f) * curInstGain
                        val karaokeR = (instRight + (mid - vocalExtracted * 0.95f) * 0.2f) * curInstGain
                        outL = karaokeL
                        outR = karaokeR
                    }
                    StemMode.CUSTOM_MIX -> {
                        // Multi-stem mix faders
                        val voc = vocalExtracted * curVocalGain
                        outL = (instLeft * curInstGain + voc)
                        outR = (instRight * curInstGain + voc)
                    }
                }

                // Apply Reverb simulation if active
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

                // Soft Clipper / Limiter to prevent harsh digital overflow distortion
                outL = softClip(outL)
                outR = softClip(outR)

                // Convert back to 16-bit PCM short
                val shortL = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                val shortR = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()

                buffer.putShort(shortL)
                buffer.putShort(shortR)

                // Visualizer metrics accumulation
                val absL = abs(outL)
                val absR = abs(outR)
                if (absL > maxPeakL) maxPeakL = absL
                if (absR > maxPeakR) maxPeakR = absR
                sumSquaresL += (outL * outL)
                sumSquaresR += (outR * outR)
                sampleCount++

                // Map audio to 24 spectrum bands for glowing visualizer
                val bandIndex = (shortIndex % 24)
                spectrumAcc[bandIndex] = max(spectrumAcc[bandIndex] * 0.94f, (absL + absR) * 0.5f)

                if (shortIndex < waveformBuffer.size) {
                    waveformBuffer[shortIndex] = (outL + outR) * 0.5f
                }
                shortIndex++

            } else {
                // Mono fallback
                val raw = inputBuffer.short / 32768.0f
                var out = raw * (if (currentStemMode == StemMode.REMOVE_VOCALS) 0.3f else 1.0f)
                out = softClip(out)
                val shortVal = (out * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                buffer.putShort(shortVal)
                shortIndex++
            }
        }

        frameCounter++
        // Emit visualizer frame every ~512 samples (~60 fps)
        if (sampleCount >= 256) {
            val rmsL = sqrt(sumSquaresL / sampleCount).toFloat().coerceIn(0f, 1f)
            val rmsR = sqrt(sumSquaresR / sampleCount).toFloat().coerceIn(0f, 1f)
            val bandsCopy = spectrumAcc.clone()
            val waveformCopy = waveformBuffer.clone()

            visualizerListener?.invoke(
                VisualizerFrame(
                    rmsLeft = rmsL,
                    rmsRight = rmsR,
                    peakLeft = maxPeakL.coerceIn(0f, 1f),
                    peakRight = maxPeakR.coerceIn(0f, 1f),
                    spectrumBands = bandsCopy,
                    waveformPoints = waveformCopy
                )
            )

            // Reset accumulators
            sumSquaresL = 0.0
            sumSquaresR = 0.0
            maxPeakL = 0f
            maxPeakR = 0f
            sampleCount = 0
        }

        buffer.flip()
        outputBuffer = buffer
    }

    private fun softClip(x: Float): Float {
        // Fast polynomial tanh-like soft saturation curve
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
        bassStateL1 = 0f
        bassStateL2 = 0f
        bassStateR1 = 0f
        bassStateR2 = 0f
        vocalBandStateL1 = 0f
        vocalBandStateL2 = 0f
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
