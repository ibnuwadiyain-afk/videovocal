package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.AudioEngineStats
import com.example.dsp.DspPerformanceMode
import com.example.dsp.StemMode
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.InstrumentalColor
import com.example.ui.theme.PitchColor
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VocalColor

@Composable
fun StemSeparationPanel(
    stemMode: StemMode,
    vocalGain: Float,
    instrumentalGain: Float,
    vocalFormantBoost: Float,
    bassBoost: Float,
    trebleBoost: Float,
    reverbLevel: Float,
    performanceMode: DspPerformanceMode,
    engineStats: AudioEngineStats,
    onStemModeChange: (StemMode) -> Unit,
    onStemGainsChange: (Float, Float) -> Unit,
    onAcousticEqChange: (Float, Float, Float, Float) -> Unit,
    onPerformanceModeChange: (DspPerformanceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 1. STEM SEPARATION MODE SELECTOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VocalColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = VocalColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Stem Separation",
                                    tint = VocalColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Stem Separation & Vocal Extraction",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Real-time Offline Demucs/Spleeter Matrix",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "OFFLINE",
                        color = EmeraldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stem Mode Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModeCard(
                            mode = StemMode.ORIGINAL,
                            isSelected = stemMode == StemMode.ORIGINAL,
                            icon = Icons.Default.MusicNote,
                            accentColor = CyanNeon,
                            onClick = { onStemModeChange(StemMode.ORIGINAL) },
                            modifier = Modifier.weight(1f).testTag("mode_original_button")
                        )

                        ModeCard(
                            mode = StemMode.REMOVE_VOCALS,
                            isSelected = stemMode == StemMode.REMOVE_VOCALS,
                            icon = Icons.Default.Headphones,
                            accentColor = InstrumentalColor,
                            onClick = { onStemModeChange(StemMode.REMOVE_VOCALS) },
                            modifier = Modifier.weight(1f).testTag("mode_remove_vocals_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModeCard(
                            mode = StemMode.ISOLATE_VOCALS,
                            isSelected = stemMode == StemMode.ISOLATE_VOCALS,
                            icon = Icons.Default.Mic,
                            accentColor = VocalColor,
                            onClick = { onStemModeChange(StemMode.ISOLATE_VOCALS) },
                            modifier = Modifier.weight(1f).testTag("mode_isolate_vocals_button")
                        )

                        ModeCard(
                            mode = StemMode.CUSTOM_MIX,
                            isSelected = stemMode == StemMode.CUSTOM_MIX,
                            icon = Icons.Default.GraphicEq,
                            accentColor = VioletLight,
                            onClick = { onStemModeChange(StemMode.CUSTOM_MIX) },
                            modifier = Modifier.weight(1f).testTag("mode_custom_mix_button")
                        )
                    }
                }
            }
        }

        // 2. DUAL STEM VOLUME FADERS (Vocal vs Instrumental)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Stem Mixer Volume Faders",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vocal Stem Fader
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Vocals",
                                tint = VocalColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vocal Lead Stem",
                                color = VocalColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${(vocalGain * 100).toInt()}%",
                            color = VocalColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = vocalGain,
                        onValueChange = { onStemGainsChange(it, instrumentalGain) },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = VocalColor,
                            activeTrackColor = VocalColor,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vocal_gain_slider")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Instrumental Stem Fader
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Instrumental",
                                tint = InstrumentalColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Instrumental & Beats Stem",
                                color = InstrumentalColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${(instrumentalGain * 100).toInt()}%",
                            color = InstrumentalColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = instrumentalGain,
                        onValueChange = { onStemGainsChange(vocalGain, it) },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = InstrumentalColor,
                            activeTrackColor = InstrumentalColor,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("instrumental_gain_slider")
                    )
                }
            }
        }

        // 3. ACOUSTIC ENHANCERS (Formants, Bass, Treble, Reverb)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Acoustic Filters & Studio FX",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vocal Formant Clarity
                EqSliderRow(
                    label = "Vocal Formant Clarity",
                    value = vocalFormantBoost,
                    color = VocalColor,
                    onValueChange = { onAcousticEqChange(it, bassBoost, trebleBoost, reverbLevel) }
                )

                // Bass Punch (< 200 Hz)
                EqSliderRow(
                    label = "Bass Boost (< 200Hz)",
                    value = bassBoost,
                    color = EmeraldAccent,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, it, trebleBoost, reverbLevel) }
                )

                // Treble Air (> 5kHz)
                EqSliderRow(
                    label = "Treble Air (> 5kHz)",
                    value = trebleBoost,
                    color = CyanNeon,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, bassBoost, it, reverbLevel) }
                )

                // Studio Reverb
                EqSliderRow(
                    label = "Studio Reverb Space",
                    value = reverbLevel,
                    color = VioletLight,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, bassBoost, trebleBoost, it) }
                )
            }
        }

        // 4. DSP ENGINE PERFORMANCE STATUS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1420)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "DSP Engine",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio Engine Metrics",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        onClick = {
                            val nextMode = if (performanceMode == DspPerformanceMode.HIGH_QUALITY)
                                DspPerformanceMode.FAST_WSOLA
                            else
                                DspPerformanceMode.HIGH_QUALITY
                            onPerformanceModeChange(nextMode)
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = VioletPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, VioletPrimary)
                    ) {
                        Text(
                            text = if (performanceMode == DspPerformanceMode.HIGH_QUALITY) "Mode: High Quality" else "Mode: Fast WSOLA",
                            color = VioletLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricTag("Latency", "${engineStats.latencyMs} ms")
                    MetricTag("Buffer", "${engineStats.bufferFrames} smp")
                    MetricTag("CPU Load", "${engineStats.cpuLoadPercent}%")
                    MetricTag("Rate", "${engineStats.sampleRate / 1000} kHz")
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: StemMode,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.18f) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isSelected) accentColor else SurfaceBorder),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.title,
                    tint = if (isSelected) accentColor else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mode.title,
                    color = if (isSelected) accentColor else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = mode.description,
                color = TextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EqSliderRow(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(130.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${(value * 100).toInt()}%",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(36.dp)
                .padding(start = 4.dp)
        )
    }
}

@Composable
private fun MetricTag(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(
            text = value,
            color = EmeraldAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
