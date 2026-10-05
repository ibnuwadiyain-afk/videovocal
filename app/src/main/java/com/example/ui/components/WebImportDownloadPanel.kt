package com.example.ui.components

import android.net.Uri
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.DownloadStatus
import com.example.network.DownloadTaskItem
import com.example.network.ProbeResult
import com.example.network.StreamOption
import com.example.ui.localization.LocalStudioStrings
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@Composable
fun WebImportDownloadPanel(
    isProbing: Boolean,
    probeResult: ProbeResult?,
    downloadTasks: List<DownloadTaskItem>,
    onProbeUrl: (String) -> Unit,
    onStartDownload: (ProbeResult, StreamOption, Boolean) -> Unit,
    onCancelDownload: (String) -> Unit,
    onLoadDownloadedTrack: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStudioStrings.current
    val clipboardManager = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf("") }
    var autoLoadOnFinish by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. WEB MEDIA PROBER CARD (4-Second Fast Timeout)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f))
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
                            color = CyanNeon.copy(alpha = 0.2f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Web Probe",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.webImportTitle,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.webImportSubtitle,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "4s PROBE",
                            color = EmeraldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text(strings.pasteUrlHint, fontSize = 11.sp) },
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear URL",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { pasted ->
                                            urlInput = pasted.trim()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste URL",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("web_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = autoLoadOnFinish,
                            onCheckedChange = { autoLoadOnFinish = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("auto_load_download_switch")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-load in player when finished",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { onProbeUrl(urlInput) },
                        enabled = !isProbing && urlInput.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        modifier = Modifier.testTag("probe_url_button")
                    ) {
                        if (isProbing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = strings.probeStreamBtn,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isProbing) strings.probingStatus.take(12) else strings.probeStreamBtn,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. PROBED STREAM RESOLUTIONS (1080p, 720p, 480p, 360p, Audio Only)
        probeResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("probed_streams_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.45f))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${strings.availableStreamsTitle} • Probed in ${result.probeTimeMs}ms",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (result.errorMessage != null) {
                                Text(
                                    text = "Note: ${result.errorMessage}",
                                    color = AmberWarn,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            color = Color(result.platform.badgeColorHex).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(result.platform.badgeColorHex))
                        ) {
                            Text(
                                text = result.platform.displayName,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    result.streams.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E1420),
                            border = BorderStroke(
                                1.dp,
                                if (option.isAudioOnly) EmeraldAccent.copy(alpha = 0.4f) else SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.resolutionLabel,
                                        color = if (option.isAudioOnly) EmeraldAccent else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${option.format} • ~${option.estimatedSizeMb} MB",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Button(
                                    onClick = { onStartDownload(result, option, autoLoadOnFinish) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (option.isAudioOnly) EmeraldAccent else VioletPrimary
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("download_stream_${option.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Download",
                                        tint = if (option.isAudioOnly) Color.Black else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Download",
                                        color = if (option.isAudioOnly) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. MULTITASK BACKGROUND DOWNLOAD MANAGER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("multitask_download_manager_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SurfaceBorder)
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
                    Text(
                        text = strings.downloadQueueTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Anti-HTML Shield",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Anti-HTML Guard",
                            color = EmeraldAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (downloadTasks.isEmpty()) {
                    Text(
                        text = "No active background downloads. Paste a stream or direct media URL above to inspect and download.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    downloadTasks.forEach { task ->
                        DownloadTaskRow(
                            task = task,
                            loadText = strings.loadIntoPlayerBtn,
                            cancelText = strings.cancelBtn,
                            onCancel = { onCancelDownload(task.id) },
                            onLoad = {
                                task.localFileUri?.let { uri ->
                                    onLoadDownloadedTrack(uri, task.title)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskRow(
    task: DownloadTaskItem,
    loadText: String,
    cancelText: String,
    onCancel: () -> Unit,
    onLoad: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0E1420),
        border = BorderStroke(
            1.dp,
            when (task.status) {
                DownloadStatus.COMPLETED -> EmeraldAccent.copy(alpha = 0.5f)
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> RoseError.copy(alpha = 0.4f)
                else -> CyanNeon.copy(alpha = 0.4f)
            }
        ),
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
                        text = task.title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    val statsLine = when (task.status) {
                        DownloadStatus.DOWNLOADING ->
                            String.format(
                                java.util.Locale.US,
                                "%.2f / %.2f MB • %.2f MB/s • %d%%",
                                task.downloadedMb,
                                task.totalMb,
                                task.speedMbps,
                                task.progressPercent
                            )
                        DownloadStatus.VERIFYING_SECURITY ->
                            "Verifying binary signature against HTML masquerading..."
                        DownloadStatus.COMPLETED ->
                            String.format(
                                java.util.Locale.US,
                                "Completed (%.2f MB) • Verified Binary Stream",
                                task.totalMb
                            )
                        DownloadStatus.CANCELLED -> "Cancelled by user"
                        DownloadStatus.FAILED -> task.errorMessage ?: "Download failed"
                    }
                    Text(
                        text = statsLine,
                        color = when (task.status) {
                            DownloadStatus.COMPLETED -> EmeraldAccent
                            DownloadStatus.VERIFYING_SECURITY -> AmberWarn
                            DownloadStatus.FAILED, DownloadStatus.CANCELLED -> RoseError
                            else -> CyanNeon
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                when (task.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING_SECURITY -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = cancelText,
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Button(
                            onClick = onLoad,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 10.dp,
                                vertical = 2.dp
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("load_completed_download_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = loadText,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = loadText,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {}
                }
            }

            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.VERIFYING_SECURITY) {
                LinearProgressIndicator(
                    progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = CyanNeon,
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                )
            }
        }
    }
}
