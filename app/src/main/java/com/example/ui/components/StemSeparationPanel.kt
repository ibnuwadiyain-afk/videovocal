package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.AudioEngineStats
import com.example.dsp.DspPerformanceMode
import com.example.dsp.NeuralModelArchitecture
import com.example.dsp.OfflineExtractionState
import com.example.dsp.StemMode
import com.example.ui.localization.LocalStudioStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.InstrumentalColor
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
    isolationStrength: Float,
    neuralArchitecture: NeuralModelArchitecture,
    cpuThreads: Int,
    offlineExtractionState: OfflineExtractionState,
    vocalGain: Float,
    instrumentalGain: Float,
    vocalFormantBoost: Float,
    bassBoost: Float,
    trebleBoost: Float,
    reverbLevel: Float,
    performanceMode: DspPerformanceMode,
    engineStats: AudioEngineStats,
    onStemModeChange: (StemMode) -> Unit,
    onIsolationStrengthChange: (Float) -> Unit,
    onNeuralArchitectureChange: (NeuralModelArchitecture) -> Unit,
    onCpuThreadsChange: (Int) -> Unit,
    onRunOfflineExtraction: () -> Unit,
    onStemGainsChange: (Float, Float) -> Unit,
    onAcousticEqChange: (Float, Float, Float, Float) -> Unit,
    onPerformanceModeChange: (DspPerformanceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStudioStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. REAL-TIME VOCAL ISOLATION & STRENGTH CONTROL (0% - 100%)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VocalColor.copy(alpha = 0.45f))
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
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Vocal Isolation",
                                    tint = VocalColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.neuralArchitectureTitle,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.neuralArchitectureSub,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "ON-DEVICE",
                        color = EmeraldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stem Mode Grid (Original Audio / Vocal Only / Karaoke / Custom Mix)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModeCard(
                            title = strings.originalAudio,
                            subtitle = StemMode.ORIGINAL.description,
                            isSelected = stemMode == StemMode.ORIGINAL,
                            icon = Icons.Default.MusicNote,
                            accentColor = CyanNeon,
                            onClick = { onStemModeChange(StemMode.ORIGINAL) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_original_button")
                        )

                        ModeCard(
                            title = strings.vocalOnly,
                            subtitle = StemMode.ISOLATE_VOCALS.description,
                            isSelected = stemMode == StemMode.ISOLATE_VOCALS,
                            icon = Icons.Default.Mic,
                            accentColor = VocalColor,
                            onClick = { onStemModeChange(StemMode.ISOLATE_VOCALS) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_isolate_vocals_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModeCard(
                            title = strings.karaokeMode,
                            subtitle = StemMode.REMOVE_VOCALS.description,
                            isSelected = stemMode == StemMode.REMOVE_VOCALS,
                            icon = Icons.Default.Headphones,
                            accentColor = InstrumentalColor,
                            onClick = { onStemModeChange(StemMode.REMOVE_VOCALS) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_remove_vocals_button")
                        )

                        ModeCard(
                            title = strings.customMix,
                            subtitle = StemMode.CUSTOM_MIX.description,
                            isSelected = stemMode == StemMode.CUSTOM_MIX,
                            icon = Icons.Default.GraphicEq,
                            accentColor = VioletLight,
                            onClick = { onStemModeChange(StemMode.CUSTOM_MIX) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_custom_mix_button")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vocal Isolation Strength Slider (0% - 100%)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.isolationStrength,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(isolationStrength * 100).toInt()}% (Mute Music)",
                        color = VocalColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = isolationStrength,
                    onValueChange = onIsolationStrengthChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = VocalColor,
                        activeTrackColor = VocalColor,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("isolation_strength_slider")
                )
            }
        }

        // 2. MULTI-MODEL ARCHITECTURE, OPERATIONAL MODES & CPU THREAD ALLOCATION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Neural Models",
                            tint = VioletLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Multi-Model Neural Engine & CPU Threads",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${cpuThreads}T • ${performanceMode.shortLabel}",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3 Separation Architectures (Demucs v4 HT, Deezer Spleeter 2-Stem, UVR-MDX-Net)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NeuralModelArchitecture.entries.forEach { arch ->
                        val isSelected = neuralArchitecture == arch
                        Surface(
                            onClick = { onNeuralArchitectureChange(arch) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) VioletPrimary.copy(alpha = 0.25f) else Color(0xFF0E1420),
                            border = BorderStroke(1.dp, if (isSelected) VioletPrimary else SurfaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("arch_${arch.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = arch.displayName,
                                    color = if (isSelected) VioletLight else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (arch == NeuralModelArchitecture.DEMUCS_V4_HT) "Hybrid HT"
                                    else if (arch == NeuralModelArchitecture.SPLEETER_2STEM) "2-Stem Fast"
                                    else "Band-Split",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3 Operational Modes (Real-Time Zero-Lag, Balanced, Deep Quality)
                Text(
                    text = strings.performanceModeTitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val coreModes = listOf(
                    DspPerformanceMode.REAL_TIME_ZERO_LAG,
                    DspPerformanceMode.BALANCED,
                    DspPerformanceMode.DEEP_QUALITY
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    coreModes.forEach { mode ->
                        val isSelected = performanceMode == mode ||
                            (mode == DspPerformanceMode.REAL_TIME_ZERO_LAG && performanceMode == DspPerformanceMode.FAST_WSOLA) ||
                            (mode == DspPerformanceMode.DEEP_QUALITY && performanceMode == DspPerformanceMode.HIGH_QUALITY)
                        Surface(
                            onClick = { onPerformanceModeChange(mode) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0xFF0E1420),
                            border = BorderStroke(1.dp, if (isSelected) CyanNeon else SurfaceBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("perf_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.shortLabel,
                                    color = if (isSelected) CyanNeon else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${mode.latencyMs}ms • ${mode.fftWindowSize}pt",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // CPU Inference Thread Allocation (1, 2, 4, 8 threads)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.cpuThreadsTitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 4, 8).forEach { count ->
                            val isSelected = cpuThreads == count
                            Surface(
                                onClick = { onCpuThreadsChange(count) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) EmeraldAccent else Color(0xFF0E1420),
                                border = BorderStroke(1.dp, if (isSelected) EmeraldAccent else SurfaceBorder),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("cpu_threads_$count")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "${count}T",
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Chunked Offline Neural Extraction Pass
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E1420),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.chunkedOfflineExtract,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = offlineExtractionState.statusMessage,
                                    color = if (offlineExtractionState.progressPercent == 100) EmeraldAccent else TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = onRunOfflineExtraction,
                                enabled = !offlineExtractionState.isExtracting,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("offline_chunk_extract_button")
                            ) {
                                Text(
                                    text = if (offlineExtractionState.isExtracting) "${offlineExtractionState.progressPercent}%"
                                    else strings.extractVocalsNow,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (offlineExtractionState.isExtracting || offlineExtractionState.progressPercent > 0) {
                            LinearProgressIndicator(
                                progress = { (offlineExtractionState.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = EmeraldAccent,
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. DUAL STEM VOLUME FADERS (Vocal vs Instrumental)
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
                    text = strings.stemMixerTitle,
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
                                text = strings.vocalStemLabel,
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
                                text = strings.instrumentalStemLabel,
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

        // 4. ACOUSTIC ENHANCERS (Formants, Bass, Treble, Reverb)
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
                    text = strings.acousticFiltersTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                EqSliderRow(
                    label = "Vocal Formant Clarity",
                    value = vocalFormantBoost,
                    color = VocalColor,
                    onValueChange = { onAcousticEqChange(it, bassBoost, trebleBoost, reverbLevel) }
                )

                EqSliderRow(
                    label = "Bass Boost (< 200Hz)",
                    value = bassBoost,
                    color = EmeraldAccent,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, it, trebleBoost, reverbLevel) }
                )

                EqSliderRow(
                    label = "Treble Air (> 5kHz)",
                    value = trebleBoost,
                    color = CyanNeon,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, bassBoost, it, reverbLevel) }
                )

                EqSliderRow(
                    label = "Studio Reverb Space",
                    value = reverbLevel,
                    color = VioletLight,
                    onValueChange = { onAcousticEqChange(vocalFormantBoost, bassBoost, trebleBoost, it) }
                )
            }
        }

        // 5. DSP ENGINE PERFORMANCE METRICS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131D)),
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
                            text = "Neural Audio Sink Telemetry",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = engineStats.activeArchitecture.displayName,
                        color = VioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricTag("Latency", "${engineStats.latencyMs} ms")
                    MetricTag("STFT Win", "${engineStats.bufferFrames} smp")
                    MetricTag("Threads", "${engineStats.cpuThreads} CPU")
                    MetricTag("CPU Load", "${engineStats.cpuLoadPercent}%")
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.18f) else Color(0xFF0E1420),
        border = BorderStroke(1.dp, if (isSelected) accentColor else SurfaceBorder),
        modifier = modifier.height(58.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) accentColor else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = if (isSelected) accentColor else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = subtitle,
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
