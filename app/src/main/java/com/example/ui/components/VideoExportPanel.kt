package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.NeuralModelArchitecture
import com.example.export.ExportJobState
import com.example.player.MediaItemInfo
import com.example.ui.localization.LocalStudioStrings
import com.example.ui.theme.AmberWarn
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
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoExportPanel(
    currentTrack: MediaItemInfo?,
    exportState: ExportJobState,
    isolationStrength: Float,
    neuralArchitecture: NeuralModelArchitecture,
    pitchSemitones: Float,
    tempo: Float,
    onFileNameChange: (String) -> Unit,
    onDeleteOriginalToggle: (Boolean) -> Unit,
    onStartExport: () -> Unit,
    onShareExported: (Uri, String) -> Unit,
    onPlayExportedFile: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStudioStrings.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. PIPELINED MP4 EXPORT CONFIGURATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VocalColor.copy(alpha = 0.45f))
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
                        Surface(
                            shape = CircleShape,
                            color = VocalColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MovieCreation,
                                    contentDescription = "Export MP4",
                                    tint = VocalColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.exportVideoTitle,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.exportVideoSubtitle,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Surface(
                        color = VocalColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, VocalColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "MP4 REMUX",
                            color = VocalColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Active Pipeline Summary Badges
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PipelineParamChip("Model: ${neuralArchitecture.displayName}", CyanNeon)
                    PipelineParamChip("Instrument Mute: ${(isolationStrength * 100).toInt()}%", VocalColor)
                    PipelineParamChip(
                        "Pitch: ${if (pitchSemitones >= 0) "+$pitchSemitones" else "$pitchSemitones"}st",
                        PitchColor
                    )
                    PipelineParamChip("Tempo: ${tempo}x", TempoColor)
                }

                // Full Arabic & Unicode Filename Input
                OutlinedTextField(
                    value = exportState.outputFileName,
                    onValueChange = onFileNameChange,
                    label = { Text(strings.exportFileNameHint, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VocalColor,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_filename_input")
                )

                // Quick Arabic & Unicode Naming Templates
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val nameTemplates = listOf(
                        "مقطع_صوتي_معزول_بدون_موسيقى.mp4",
                        "تلاوة_أو_نشيد_صافي_VocalOnly.mp4",
                        "Studio_Vocal_Isolated_Master.mp4",
                        "Voz_Aislada_Sin_Instrumentos.mp4"
                    )
                    nameTemplates.forEach { presetName ->
                        Surface(
                            onClick = { onFileNameChange(presetName) },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0E1420),
                            border = BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = presetName,
                                    color = VioletLight,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Optional Toggle: Automatically delete original source file after verified export
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E1420),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Auto-delete source",
                                tint = AmberWarn,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.deleteOriginalAfterExport,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = exportState.deleteOriginalOnSuccess,
                            onCheckedChange = onDeleteOriginalToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AmberWarn,
                                uncheckedTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("delete_original_after_export_switch")
                        )
                    }
                }

                // Start Pipelined Export Button
                Button(
                    onClick = onStartExport,
                    enabled = !exportState.isExporting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VocalColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("start_mp4_export_button")
                ) {
                    if (exportState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${exportState.stageDescription} (${exportState.progressPercent}%)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = strings.startExportBtn,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.startExportBtn,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (exportState.isExporting) {
                    LinearProgressIndicator(
                        progress = { (exportState.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = VocalColor,
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }

        // 2. VERIFIED EXPORT RESULT & ONE-TAP FILEPROVIDER SHARE CARD
        if (exportState.exportedFilePath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exported_mp4_result_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = exportState.outputFileName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Size: ${exportState.fileSizeKb} KB • MediaStore Registered • FileProvider Ready",
                                    color = EmeraldAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportState.exportedFilePath?.let { path ->
                                    onPlayExportedFile(Uri.fromFile(File(path)), exportState.outputFileName)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("play_exported_mp4_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Exported",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.loadIntoPlayerBtn,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                exportState.exportedShareUri?.let { uri ->
                                    onShareExported(uri, exportState.outputFileName)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyanNeon),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_exported_mp4_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = strings.shareExportedBtn,
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.shareExportedBtn,
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineParamChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
