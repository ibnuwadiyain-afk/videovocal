package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.dsp.FxTarget
import com.example.dsp.StemMode
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PitchColor
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TempoColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VocalColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PitchTempoControlPanel(
    pitchSemitones: Float,
    pitchCents: Int,
    tempo: Float,
    fxTarget: FxTarget,
    stemMode: StemMode,
    syncVideoWithTempo: Boolean,
    onPitchChange: (Float) -> Unit,
    onPitchStep: (Float) -> Unit,
    onPitchCentsChange: (Int) -> Unit,
    onResetPitch: () -> Unit,
    onTempoChange: (Float) -> Unit,
    onResetTempo: () -> Unit,
    onFxTargetChange: (FxTarget) -> Unit,
    onSyncVideoToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 1. TARGET ROUTING SELECTOR (All / Vocals Only / Music Only)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Target Selector",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DSP Effect Target Route",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = fxTarget.title,
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetButton(
                        title = "All Audio",
                        icon = Icons.Default.Audiotrack,
                        isSelected = fxTarget == FxTarget.ALL,
                        onClick = { onFxTargetChange(FxTarget.ALL) },
                        modifier = Modifier.weight(1f).testTag("target_all_button")
                    )

                    TargetButton(
                        title = "Vocal Only",
                        icon = Icons.Default.Mic,
                        isSelected = fxTarget == FxTarget.VOCALS_ONLY,
                        onClick = { onFxTargetChange(FxTarget.VOCALS_ONLY) },
                        modifier = Modifier.weight(1f).testTag("target_vocals_button")
                    )

                    TargetButton(
                        title = "Music Only",
                        icon = Icons.Default.MusicNote,
                        isSelected = fxTarget == FxTarget.INSTRUMENTAL_ONLY,
                        onClick = { onFxTargetChange(FxTarget.INSTRUMENTAL_ONLY) },
                        modifier = Modifier.weight(1f).testTag("target_music_button")
                    )
                }

                if (stemMode == StemMode.ORIGINAL && fxTarget != FxTarget.ALL) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tip: Real-time stem splitting applies pitch/tempo strictly to ${fxTarget.title}.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // 2. PITCH SHIFT CONTROL CARD (-12 to +12 Semitones + Cents)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PitchColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header with current pitch display & reset button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PitchColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Pitch",
                                    tint = PitchColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pitch Shift (Key Transpose)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phase Vocoder Real-time Shifter",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Reset Pitch Button
                    OutlinedButton(
                        onClick = onResetPitch,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (pitchSemitones == 0f && pitchCents == 0) SurfaceBorder else PitchColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (pitchSemitones != 0f || pitchCents != 0) PitchColor.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("pitch_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Pitch",
                            tint = if (pitchSemitones != 0f || pitchCents != 0) PitchColor else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            color = if (pitchSemitones != 0f || pitchCents != 0) PitchColor else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pitch Large Readout Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${if (pitchSemitones > 0) "+" else ""}${String.format("%.1f", pitchSemitones)} st",
                            color = PitchColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = getPitchDescription(pitchSemitones),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Step buttons (-1 st / +1 st)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onPitchStep(-1.0f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF0F172A), CircleShape)
                                .testTag("pitch_minus_one_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "-1 Semitone",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onPitchStep(1.0f) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF0F172A), CircleShape)
                                .testTag("pitch_plus_one_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "+1 Semitone",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Continuous Pitch Slider (-12 to +12)
                Slider(
                    value = pitchSemitones,
                    onValueChange = onPitchChange,
                    valueRange = -12.0f..12.0f,
                    steps = 239, // 0.1 increments
                    colors = SliderDefaults.colors(
                        thumbColor = PitchColor,
                        activeTrackColor = PitchColor,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pitch_slider")
                )

                // Quick Pitch Jump Buttons
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(-12f, -7f, -4f, -2f, -1f, 0f, 1f, 2f, 4f, 7f, 12f)
                    presets.forEach { st ->
                        val isSelected = Math.abs(pitchSemitones - st) < 0.05f
                        Surface(
                            onClick = { onPitchChange(st) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) PitchColor else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) PitchColor else SurfaceBorder),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 7.dp)
                            ) {
                                Text(
                                    text = if (st == 0f) "0" else if (st > 0) "+${st.toInt()}" else "${st.toInt()}",
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cents Fine-Tuning Slider (-50 to +50 cents)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fine Tune: ${if (pitchCents >= 0) "+$pitchCents" else "$pitchCents"} cents",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Slider(
                        value = pitchCents.toFloat(),
                        onValueChange = { onPitchCentsChange(it.toInt()) },
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = VioletLight,
                            activeTrackColor = VioletLight,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .width(160.dp)
                            .testTag("pitch_cents_slider")
                    )
                }
            }
        }

        // 3. TEMPO / SPEED CONTROL CARD (0.50x to 2.00x)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, TempoColor.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header with current tempo readout & reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TempoColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Tempo",
                                    tint = TempoColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tempo / Time Stretch (Speed)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WSOLA Constant-Pitch Time Scaler",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Reset Tempo Button
                    OutlinedButton(
                        onClick = onResetTempo,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (tempo == 1.0f) SurfaceBorder else TempoColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (tempo != 1.0f) TempoColor.copy(alpha = 0.15f) else Color.Transparent
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("tempo_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Tempo",
                            tint = if (tempo != 1.0f) TempoColor else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "1.0x",
                            color = if (tempo != 1.0f) TempoColor else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tempo Large Readout Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${String.format("%.2f", tempo)}x",
                            color = TempoColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        val percent = ((tempo - 1.0f) * 100).toInt()
                        Text(
                            text = if (tempo == 1.0f) "Original Speed" else "${if (percent > 0) "+$percent%" else "$percent%"} Playback Rate",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Sync Video Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Video",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = syncVideoWithTempo,
                            onCheckedChange = onSyncVideoToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TempoColor,
                                uncheckedTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("sync_video_switch")
                        )
                    }
                }

                // Continuous Tempo Slider (0.50x to 2.00x)
                Slider(
                    value = tempo,
                    onValueChange = onTempoChange,
                    valueRange = 0.50f..2.00f,
                    steps = 150, // 0.01 increments
                    colors = SliderDefaults.colors(
                        thumbColor = TempoColor,
                        activeTrackColor = TempoColor,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tempo_slider")
                )

                // Quick Tempo Presets
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tempoPresets = listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.15f, 1.25f, 1.5f, 1.75f, 2.0f)
                    tempoPresets.forEach { speed ->
                        val isSelected = Math.abs(tempo - speed) < 0.02f
                        Surface(
                            onClick = { onTempoChange(speed) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) TempoColor else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) TempoColor else SurfaceBorder),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
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
        }
    }
}

@Composable
private fun TargetButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (isSelected) CyanNeon else SurfaceBorder),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) CyanNeon else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) CyanNeon else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun getPitchDescription(semitones: Float): String {
    return when (Math.round(semitones)) {
        -12 -> "1 Octave Lower (Bass)"
        -7 -> "Perfect 5th Down (Key of F from C)"
        -5 -> "Perfect 4th Down (Key of G from C)"
        -4 -> "Major 3rd Down (Female to Male)"
        -2 -> "Major 2nd Down"
        0 -> "Original Key (No transposition)"
        2 -> "Major 2nd Up"
        4 -> "Major 3rd Up (Male to Female)"
        5 -> "Perfect 4th Up"
        7 -> "Perfect 5th Up"
        12 -> "1 Octave Higher (High Air)"
        else -> "Transposed Key"
    }
}
