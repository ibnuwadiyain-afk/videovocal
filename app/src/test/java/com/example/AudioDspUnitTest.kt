package com.example

import com.example.dsp.DspPerformanceMode
import com.example.dsp.FxTarget
import com.example.dsp.StemMode
import com.example.dsp.VocalCutAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioDspUnitTest {

    @Test
    fun testDspProcessorDefaults() {
        val processor = VocalCutAudioProcessor()
        assertEquals(StemMode.ORIGINAL, processor.stemMode)
        assertEquals(FxTarget.ALL, processor.fxTarget)
        assertEquals(DspPerformanceMode.HIGH_QUALITY, processor.performanceMode)
        assertEquals(1.0f, processor.vocalGain, 0.001f)
        assertEquals(1.0f, processor.instrumentalGain, 0.001f)
    }

    @Test
    fun testDspParameterChanges() {
        val processor = VocalCutAudioProcessor()
        processor.stemMode = StemMode.ISOLATE_VOCALS
        processor.vocalGain = 1.4f
        processor.bassBoost = 0.5f
        processor.trebleBoost = 0.3f
        processor.reverbLevel = 0.25f

        assertEquals(StemMode.ISOLATE_VOCALS, processor.stemMode)
        assertEquals(1.4f, processor.vocalGain, 0.001f)
        assertEquals(0.5f, processor.bassBoost, 0.001f)
        assertEquals(0.3f, processor.trebleBoost, 0.001f)
        assertEquals(0.25f, processor.reverbLevel, 0.001f)
    }
}
