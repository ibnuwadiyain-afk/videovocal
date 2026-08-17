package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PlayerViewModel
import com.example.ui.components.AudioProfilesSheet
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.MediaPlaylistSheet
import com.example.ui.components.PitchTempoControlPanel
import com.example.ui.components.SaveProfileDialog
import com.example.ui.components.StemSeparationPanel
import com.example.ui.components.VideoPlayerSection
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PitchColor
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TempoColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletLight
import com.example.ui.theme.VioletPrimary
import com.example.ui.theme.VocalColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VocalCutProApp()
            }
        }
    }
}

@Composable
fun VocalCutProApp(viewModel: PlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val visualizerFrame by viewModel.visualizerFrame.collectAsState()
    val engineStats by viewModel.engineStats.collectAsState()
    val loopABEnabled by viewModel.loopABEnabled.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // App Header Bar
            AppHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Main Video Surface & Overlay Player Controls
            VideoPlayerSection(
                player = viewModel.engine.getPlayer(),
                currentTrack = uiState.currentTrack,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                pitchSemitones = uiState.pitchSemitones,
                tempo = uiState.tempo,
                stemMode = uiState.stemMode,
                loopABEnabled = loopABEnabled,
                hudNotification = uiState.hudNotification,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekRelative = { viewModel.seekRelative(it) },
                onToggleLoopAB = { viewModel.toggleLoopAB() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Spectrum VU Visualizer
            AudioVisualizer(
                frame = visualizerFrame,
                isPlaying = isPlaying,
                modifier = Modifier.testTag("audio_visualizer_bar")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs (Pitch & Tempo / Stem Separation / Presets / Tracks)
            StudioNavigationTabs(
                activeTab = uiState.activeTab,
                onTabSelect = { viewModel.setActiveTab(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Content Body (Scrollable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (uiState.activeTab) {
                    0 -> {
                        // Pitch & Tempo Control View
                        PitchTempoControlPanel(
                            pitchSemitones = uiState.pitchSemitones,
                            pitchCents = uiState.pitchCents,
                            tempo = uiState.tempo,
                            fxTarget = uiState.fxTarget,
                            stemMode = uiState.stemMode,
                            syncVideoWithTempo = uiState.syncVideoWithTempo,
                            onPitchChange = { viewModel.setPitchSemitones(it) },
                            onPitchStep = { viewModel.stepPitch(it) },
                            onPitchCentsChange = { viewModel.setPitchCents(it) },
                            onResetPitch = { viewModel.resetPitch() },
                            onTempoChange = { viewModel.setTempo(it) },
                            onResetTempo = { viewModel.resetTempo() },
                            onFxTargetChange = { viewModel.setFxTarget(it) },
                            onSyncVideoToggle = { viewModel.setSyncVideoWithTempo(it) },
                            modifier = Modifier.testTag("pitch_tempo_panel")
                        )
                    }
                    1 -> {
                        // Stem Separation & Mixer View
                        StemSeparationPanel(
                            stemMode = uiState.stemMode,
                            vocalGain = uiState.vocalGain,
                            instrumentalGain = uiState.instrumentalGain,
                            vocalFormantBoost = uiState.vocalFormantBoost,
                            bassBoost = uiState.bassBoost,
                            trebleBoost = uiState.trebleBoost,
                            reverbLevel = uiState.reverbLevel,
                            performanceMode = uiState.performanceMode,
                            engineStats = engineStats,
                            onStemModeChange = { viewModel.setStemMode(it) },
                            onStemGainsChange = { voc, inst -> viewModel.setStemGains(voc, inst) },
                            onAcousticEqChange = { fmt, bass, trb, rev -> viewModel.setAcousticEq(fmt, bass, trb, rev) },
                            onPerformanceModeChange = { viewModel.setPerformanceMode(it) },
                            modifier = Modifier.testTag("stem_separation_panel")
                        )
                    }
                    2 -> {
                        // Audio Presets & Profiles Sheet
                        AudioProfilesSheet(
                            profiles = savedProfiles,
                            onApplyProfile = { viewModel.applyProfile(it) },
                            onDeleteProfile = { viewModel.deleteProfile(it) },
                            onOpenSaveDialog = { showSaveDialog = true },
                            modifier = Modifier.testTag("audio_profiles_panel")
                        )
                    }
                    3 -> {
                        // Media Library & Tracks
                        MediaPlaylistSheet(
                            mediaList = uiState.mediaPlaylist,
                            currentTrack = uiState.currentTrack,
                            onSelectTrack = { viewModel.selectTrack(it) },
                            onMediaPicked = { uri, name -> viewModel.addCustomMedia(uri, name) },
                            modifier = Modifier.testTag("media_playlist_panel")
                        )
                    }
                }
            }
        }
    }

    // Save Profile Dialog
    if (showSaveDialog) {
        SaveProfileDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name, desc ->
                viewModel.saveCurrentAsProfile(name, desc)
            }
        )
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = VioletPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "VocalCut Pro+",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VocalCut",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " Pro+",
                        color = CyanNeon,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Offline Real-time DSP • Pitch • Tempo • Stems",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live DSP Engine Indicator
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(EmeraldAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LOW-LATENCY",
                    color = EmeraldAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StudioNavigationTabs(
    activeTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val tabs = listOf(
        TabItem("Pitch & Tempo", Icons.Default.Speed, PitchColor),
        TabItem("Stem Separation", Icons.Default.Headphones, VocalColor),
        TabItem("Presets", Icons.Default.Bookmark, VioletLight),
        TabItem("Media", Icons.Default.VideoLibrary, CyanNeon)
    )

    TabRow(
        selectedTabIndex = activeTab,
        containerColor = SurfaceDark,
        contentColor = CyanNeon,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                color = tabs[activeTab].color,
                height = 3.dp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_navigation_tabs")
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = activeTab == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelect(index) },
                text = {
                    Text(
                        text = tab.title,
                        color = if (isSelected) tab.color else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) tab.color else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

private data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
