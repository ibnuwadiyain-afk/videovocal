package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dsp.NeuralModelArchitecture
import com.example.dsp.StemMode
import com.example.dsp.VocalCutAudioProcessor
import com.example.export.VideoExportPipeline
import com.example.network.WebMediaManager
import com.example.ui.localization.StudioLanguage
import com.example.ui.localization.StudioTranslations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VocalCut Pro+", appName)
    }

    @Test
    fun `verify 6 studio languages and Arabic RTL configuration`() {
        assertEquals(6, StudioLanguage.entries.size)
        assertTrue(StudioLanguage.AR.isRtl)
        assertFalse(StudioLanguage.EN.isRtl)

        StudioLanguage.entries.forEach { lang ->
            val strings = StudioTranslations.forLanguage(lang)
            assertTrue(strings.vocalOnly.isNotBlank())
            assertTrue(strings.isolationStrength.isNotBlank())
            assertTrue(strings.exportVideoTitle.isNotBlank())
        }
    }

    @Test
    fun `verify Arabic and Unicode filename sanitization for MP4 export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pipeline = VideoExportPipeline(context)

        val sanitized = pipeline.sanitizeUnicodeFileName("مقطع_صوتي:معزول*بدون?موسيقى")
        assertEquals("مقطع_صوتي_معزول_بدون_موسيقى.mp4", sanitized)
    }

    @Test
    fun `verify anti HTML masquerading security check in WebMediaManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = WebMediaManager(context)

        val htmlBytes = "<!DOCTYPE html><html><body>Error</body></html>".toByteArray()
        val wavBytes = "RIFF....WAVEfmt ".toByteArray()

        assertTrue(manager.isHtmlMasquerading(htmlBytes))
        assertFalse(manager.isHtmlMasquerading(wavBytes))
    }

    @Test
    fun `verify neural vocal processor offline chunk processing`() {
        val processor = VocalCutAudioProcessor().apply {
            stemMode = StemMode.ISOLATE_VOCALS
            isolationStrength = 0.95f
            neuralArchitecture = NeuralModelArchitecture.DEMUCS_V4_HT
        }
        val input = ShortArray(1024) { idx -> if (idx % 2 == 0) 8000 else 8000 }
        val output = processor.processOfflinePcmChunk(input, 44100, 2)
        assertEquals(1024, output.size)
    }
}
