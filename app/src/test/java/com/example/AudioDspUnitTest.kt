package com.example

import com.example.dsp.DspPerformanceMode
import com.example.dsp.FxTarget
import com.example.dsp.NeuralModelArchitecture
import com.example.dsp.StemMode
import com.example.dsp.VocalCutAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDspUnitTest {

    @Test
    fun testDspProcessorDefaults() {
        val processor = VocalCutAudioProcessor()
        assertEquals(StemMode.ORIGINAL, processor.stemMode)
        assertEquals(FxTarget.ALL, processor.fxTarget)
        assertEquals(DspPerformanceMode.REAL_TIME_ZERO_LAG, processor.performanceMode)
        assertEquals(NeuralModelArchitecture.DEMUCS_V4_HT, processor.neuralArchitecture)
        assertEquals(0.85f, processor.isolationStrength, 0.001f)
        assertEquals(4, processor.cpuThreads)
        assertEquals(1.0f, processor.vocalGain, 0.001f)
        assertEquals(1.0f, processor.instrumentalGain, 0.001f)
    }

    @Test
    fun testDspParameterChanges() {
        val processor = VocalCutAudioProcessor()
        processor.stemMode = StemMode.ISOLATE_VOCALS
        processor.isolationStrength = 1.0f
        processor.neuralArchitecture = NeuralModelArchitecture.UVR_MDX_NET
        processor.performanceMode = DspPerformanceMode.DEEP_QUALITY
        processor.cpuThreads = 8
        processor.vocalGain = 1.4f
        processor.bassBoost = 0.5f
        processor.trebleBoost = 0.3f
        processor.reverbLevel = 0.25f

        assertEquals(StemMode.ISOLATE_VOCALS, processor.stemMode)
        assertEquals(1.0f, processor.isolationStrength, 0.001f)
        assertEquals(NeuralModelArchitecture.UVR_MDX_NET, processor.neuralArchitecture)
        assertEquals(DspPerformanceMode.DEEP_QUALITY, processor.performanceMode)
        assertEquals(8, processor.cpuThreads)
        assertEquals(1.4f, processor.vocalGain, 0.001f)
        assertEquals(0.5f, processor.bassBoost, 0.001f)
        assertEquals(0.3f, processor.trebleBoost, 0.001f)
        assertEquals(0.25f, processor.reverbLevel, 0.001f)
    }
}
