package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.VisualizerFrame
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPrimary

@Composable
fun AudioVisualizer(
    frame: VisualizerFrame,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF0D121D), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dual Channel VU Meter (L / R)
            VuMeter(
                level = if (isPlaying) frame.rmsLeft else 0f,
                peak = if (isPlaying) frame.peakLeft else 0f,
                label = "L",
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(6.dp))

            VuMeter(
                level = if (isPlaying) frame.rmsRight else 0f,
                peak = if (isPlaying) frame.peakRight else 0f,
                label = "R",
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 24-Band Glowing Neon Spectrum Bars
            SpectrumBarsCanvas(
                bands = frame.spectrumBands,
                isPlaying = isPlaying,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun VuMeter(
    level: Float,
    peak: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "vu_level"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E2638), RoundedCornerShape(3.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height
                val fillH = (h * animatedLevel).coerceIn(0f, h)
                val topY = h - fillH

                // Color gradient based on level height: Green -> Cyan -> Pink/Red
                val gradient = Brush.verticalGradient(
                    colors = listOf(RoseError, CyanNeon, EmeraldAccent),
                    startY = 0f,
                    endY = h
                )

                drawRoundRect(
                    brush = gradient,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, fillH),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Peak line indicator
                val peakY = (h - (h * peak)).coerceIn(0f, h - 2f)
                drawLine(
                    color = Color.White,
                    start = Offset(0f, peakY),
                    end = Offset(size.width, peakY),
                    strokeWidth = 2f
                )
            }
        }
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpectrumBarsCanvas(
    bands: FloatArray,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = bands.size
        if (count == 0) return@Canvas

        val barSpacing = 3.dp.toPx()
        val totalSpacing = barSpacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2f)

        for (i in 0 until count) {
            val rawVal = if (isPlaying) bands[i] else 0.05f
            val normalizedHeight = (rawVal * 1.6f).coerceIn(0.06f, 1f) * size.height
            val x = i * (barWidth + barSpacing)
            val y = size.height - normalizedHeight

            val color = when {
                i < 6 -> EmeraldGlow // Bass
                i < 16 -> CyanGlow   // Vocals
                else -> VioletLight  // Treble
            }

            val brush = Brush.verticalGradient(
                colors = listOf(color, color.copy(alpha = 0.4f)),
                startY = y,
                endY = size.height
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
