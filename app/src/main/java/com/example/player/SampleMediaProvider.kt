package com.example.player

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

data class MediaItemInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val uri: Uri,
    val isSample: Boolean = true,
    val durationMs: Long = 20000L
)

object SampleMediaProvider {

    fun getSampleTracks(context: Context): List<MediaItemInfo> {
        val dir = File(context.filesDir, "samples").apply { mkdirs() }

        val track1 = createSynthPopSample(File(dir, "pop_studio_hit.wav"))
        val track2 = createAcousticBalladSample(File(dir, "acoustic_ballad.wav"))
        val track3 = createSpeechSample(File(dir, "vocal_speech_studio.wav"))

        return listOf(
            MediaItemInfo(
                id = "sample_pop",
                title = "Electro Pop (Vocal + Stereo Beats)",
                subtitle = "Demo: Center Lead Melody + Wide Stereo Synth & Bass",
                uri = Uri.fromFile(track1),
                isSample = true,
                durationMs = 24000L
            ),
            MediaItemInfo(
                id = "sample_ballad",
                title = "Acoustic Melody & Piano",
                subtitle = "Demo: Center Vocal Lead with Stereo Piano Chords",
                uri = Uri.fromFile(track2),
                isSample = true,
                durationMs = 20000L
            ),
            MediaItemInfo(
                id = "sample_speech",
                title = "Vocal Speech & Studio Narration",
                subtitle = "Demo: Human voice for Pitch & Tempo audition",
                uri = Uri.fromFile(track3),
                isSample = true,
                durationMs = 18000L
            )
        )
    }

    private fun createSynthPopSample(file: File): File {
        if (file.exists() && file.length() > 1000) return file
        val sampleRate = 44100
        val durationSeconds = 24
        val totalSamples = sampleRate * durationSeconds
        val buffer = ByteBuffer.allocate(44 + totalSamples * 4).order(ByteOrder.LITTLE_ENDIAN)

        // Write WAV Header
        writeWavHeader(buffer, totalSamples * 4, sampleRate, 2)

        val chords = listOf(
            listOf(261.63, 329.63, 392.00), // C Maj
            listOf(220.00, 261.63, 329.63), // A Min
            listOf(174.61, 220.00, 261.63), // F Maj
            listOf(196.00, 246.94, 293.66)  // G Maj
        )
        val melodyNotes = listOf(523.25, 587.33, 659.25, 783.99, 659.25, 587.33, 523.25, 392.00)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val beat = (t * 2.0).toInt()
            val chord = chords[(beat / 4) % chords.size]

            // 1. Stereo Instrumentation (Left/Right panned differently)
            var leftInst = 0.0
            var rightInst = 0.0

            // Bass (100Hz center-low)
            val bassFreq = chord[0] / 2.0
            val bass = sin(2 * PI * bassFreq * t) * 0.25

            // Stereo Synth Chords (wide panning)
            val chordNote1 = sin(2 * PI * chord[0] * t) * 0.15
            val chordNote2 = sin(2 * PI * chord[1] * t) * 0.15
            val chordNote3 = sin(2 * PI * chord[2] * t) * 0.15

            leftInst += bass + chordNote1 * 1.2 + chordNote2 * 0.4
            rightInst += bass + chordNote3 * 1.2 + chordNote2 * 0.4

            // Drum Snare/Hihat on beats (stereo)
            val drumPhase = (t * 4.0) % 1.0
            if (drumPhase < 0.08) {
                val noise = ((i % 17) / 17.0 - 0.5) * 0.2
                leftInst += noise
                rightInst += noise
            }

            // 2. CENTER VOCAL MELODY (Identical in Left and Right)
            val noteIndex = ((t * 3.0).toInt()) % melodyNotes.size
            val melFreq = melodyNotes[noteIndex]
            val vibrato = sin(2 * PI * 5.5 * t) * 4.0 // 5.5 Hz vibrato
            val vocalOsc = sin(2 * PI * (melFreq + vibrato) * t)
            // Vocal formant harmonic
            val vocalHarmonic = sin(2 * PI * (melFreq * 2.0) * t) * 0.35
            val vocalCenter = (vocalOsc + vocalHarmonic) * 0.4

            // Final Master Mix: Center Vocal is in BOTH L & R equally
            val masterL = ((leftInst + vocalCenter) * 0.8).coerceIn(-1.0, 1.0)
            val masterR = ((rightInst + vocalCenter) * 0.8).coerceIn(-1.0, 1.0)

            buffer.putShort((masterL * 32767).toInt().toShort())
            buffer.putShort((masterR * 32767).toInt().toShort())
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
        return file
    }

    private fun createAcousticBalladSample(file: File): File {
        if (file.exists() && file.length() > 1000) return file
        val sampleRate = 44100
        val durationSeconds = 20
        val totalSamples = sampleRate * durationSeconds
        val buffer = ByteBuffer.allocate(44 + totalSamples * 4).order(ByteOrder.LITTLE_ENDIAN)

        writeWavHeader(buffer, totalSamples * 4, sampleRate, 2)

        val notes = listOf(440.0, 493.88, 523.25, 587.33, 659.25)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Gentle Piano Arpeggio
            val pianoL = (sin(2 * PI * 220.0 * t) + sin(2 * PI * 330.0 * t)) * 0.2
            val pianoR = (sin(2 * PI * 261.6 * t) + sin(2 * PI * 392.0 * t)) * 0.2

            // Center Emotional Lead
            val note = notes[((t * 1.5).toInt()) % notes.size]
            val vocal = (sin(2 * PI * note * t) + 0.25 * sin(2 * PI * note * 2 * t)) * 0.45

            val outL = ((pianoL + vocal) * 0.85).coerceIn(-1.0, 1.0)
            val outR = ((pianoR + vocal) * 0.85).coerceIn(-1.0, 1.0)

            buffer.putShort((outL * 32767).toInt().toShort())
            buffer.putShort((outR * 32767).toInt().toShort())
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
        return file
    }

    private fun createSpeechSample(file: File): File {
        if (file.exists() && file.length() > 1000) return file
        val sampleRate = 44100
        val durationSeconds = 18
        val totalSamples = sampleRate * durationSeconds
        val buffer = ByteBuffer.allocate(44 + totalSamples * 4).order(ByteOrder.LITTLE_ENDIAN)

        writeWavHeader(buffer, totalSamples * 4, sampleRate, 2)

        val speechInflections = listOf(140.0, 155.0, 170.0, 160.0, 145.0, 130.0, 150.0, 165.0)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val segment = ((t * 2.5).toInt()) % speechInflections.size
            val f0 = speechInflections[segment]

            // Natural human speech formant simulation (F0, F1 ~ 700Hz, F2 ~ 1800Hz, F3 ~ 2800Hz)
            val formant1 = sin(2 * PI * 700.0 * t) * 0.3
            val formant2 = sin(2 * PI * 1800.0 * t) * 0.2
            val glottalPulse = sin(2 * PI * f0 * t) * 0.4

            // Slight background ambient studio noise
            val ambient = ((i % 13) / 13.0 - 0.5) * 0.02

            val voice = (glottalPulse + formant1 + formant2) * 0.6
            val out = (voice + ambient).coerceIn(-1.0, 1.0)

            buffer.putShort((out * 32767).toInt().toShort())
            buffer.putShort((out * 32767).toInt().toShort())
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
        return file
    }

    private fun writeWavHeader(buffer: ByteBuffer, dataSize: Int, sampleRate: Int, channels: Int) {
        val byteRate = sampleRate * channels * 2
        buffer.put("RIFF".toByteArray())
        buffer.putInt(dataSize + 36)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // SubChunk1Size (PCM)
        buffer.putShort(1)  // AudioFormat (PCM = 1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
    }
}
