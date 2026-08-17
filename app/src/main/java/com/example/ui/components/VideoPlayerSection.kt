package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.dsp.StemMode
import com.example.player.MediaItemInfo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PitchColor
import com.example.ui.theme.TempoColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VocalColor

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSection(
    player: ExoPlayer?,
    currentTrack: MediaItemInfo?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    pitchSemitones: Float,
    tempo: Float,
    stemMode: StemMode,
    loopABEnabled: Boolean,
    hudNotification: String?,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleLoopAB: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF070A0F)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            // Android Media3 PlayerView for Video & Offline Audio playback
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // We use our custom Compose studio controls
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_player_surface")
            )

            // Dynamic Gradient Overlay for high visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top Status Bar: Track Title & Active DSP Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "VocalCut Pro+ Video Player",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentTrack?.subtitle ?: "Offline Real-time DSP Engine",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                // Active DSP Pill Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pitchSemitones != 0.0f) {
                        DspBadge(
                            text = "${if (pitchSemitones > 0) "+" else ""}${pitchSemitones}st",
                            color = PitchColor
                        )
                    }
                    if (tempo != 1.0f) {
                        DspBadge(
                            text = "${tempo}x",
                            color = TempoColor
                        )
                    }
                    if (stemMode != StemMode.ORIGINAL) {
                        DspBadge(
                            text = when (stemMode) {
                                StemMode.ISOLATE_VOCALS -> "VOCALS"
                                StemMode.REMOVE_VOCALS -> "KARAOKE"
                                StemMode.CUSTOM_MIX -> "MIXER"
                                else -> ""
                            },
                            color = VocalColor
                        )
                    }
                }
            }

            // Center Buffering or Large HUD Notification
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center),
                    color = VioletPrimary,
                    strokeWidth = 3.dp
                )
            }

            AnimatedVisibility(
                visible = hudNotification != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                hudNotification?.let { msg ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = msg,
                            color = CyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Transport Controls & Timeline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Scrubbing Timeline Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .testTag("timeline_slider")
                    )

                    Text(
                        text = formatTime(durationMs),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Player Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loop A-B Button
                    IconButton(
                        onClick = onToggleLoopAB,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("loop_ab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Loop A-B",
                            tint = if (loopABEnabled) EmeraldAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Seek -10s
                    IconButton(
                        onClick = { onSeekRelative(-10000L) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("seek_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Central Play / Pause Button
                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = VioletPrimary,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Seek +10s
                    IconButton(
                        onClick = { onSeekRelative(10000L) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("seek_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Status Indicator
                    Text(
                        text = if (isPlaying) "LIVE DSP" else "PAUSED",
                        color = if (isPlaying) EmeraldAccent else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DspBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
