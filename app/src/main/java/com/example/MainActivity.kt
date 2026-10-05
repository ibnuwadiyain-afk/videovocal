package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dsp.StemMode
import com.example.ui.PlayerViewModel
import com.example.ui.components.AudioProfilesSheet
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.MediaPlaylistSheet
import com.example.ui.components.PitchTempoControlPanel
import com.example.ui.components.SaveProfileDialog
import com.example.ui.components.StemSeparationPanel
import com.example.ui.components.VideoExportPanel
import com.example.ui.components.VideoPlayerSection
import com.example.ui.components.WebImportDownloadPanel
import com.example.ui.localization.LocalStudioStrings
import com.example.ui.localization.StudioLanguage
import com.example.ui.localization.StudioTranslations
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PitchColor
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val visualizerFrame by viewModel.visualizerFrame.collectAsState()
    val engineStats by viewModel.engineStats.collectAsState()
    val loopABEnabled by viewModel.loopABEnabled.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()

    val isProbing by viewModel.isProbing.collectAsState()
    val lastProbeResult by viewModel.lastProbeResult.collectAsState()
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }

    val studioStrings = remember(uiState.language) {
        StudioTranslations.forLanguage(uiState.language)
    }
    val layoutDirection = if (uiState.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalStudioStrings provides studioStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
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
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // Studio Header Bar with 6-Language Switcher
                AppHeader(
                    currentLanguage = uiState.language,
                    onSelectLanguage = { viewModel.setLanguage(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

                // Instant Original vs Vocal Only Toggle + Vocal Isolation Strength Slider (0% - 100%)
                QuickVocalIsolationBar(
                    stemMode = uiState.stemMode,
                    isolationStrength = uiState.isolationStrength,
                    onToggleVocalOnly = { viewModel.toggleQuickVocalIsolation() },
                    onIsolationStrengthChange = { viewModel.setIsolationStrength(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live Spectrum VU Visualizer
                AudioVisualizer(
                    frame = visualizerFrame,
                    isPlaying = isPlaying,
                    modifier = Modifier.testTag("audio_visualizer_bar")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Studio Navigation Tabs
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
                            // Neural Vocal Isolation, Multi-Model & Stem Mixer View
                            StemSeparationPanel(
                                stemMode = uiState.stemMode,
                                isolationStrength = uiState.isolationStrength,
                                neuralArchitecture = uiState.neuralArchitecture,
                                cpuThreads = uiState.cpuThreads,
                                offlineExtractionState = uiState.offlineExtraction,
                                vocalGain = uiState.vocalGain,
                                instrumentalGain = uiState.instrumentalGain,
                                vocalFormantBoost = uiState.vocalFormantBoost,
                                bassBoost = uiState.bassBoost,
                                trebleBoost = uiState.trebleBoost,
                                reverbLevel = uiState.reverbLevel,
                                performanceMode = uiState.performanceMode,
                                engineStats = engineStats,
                                onStemModeChange = { viewModel.setStemMode(it) },
                                onIsolationStrengthChange = { viewModel.setIsolationStrength(it) },
                                onNeuralArchitectureChange = { viewModel.setNeuralArchitecture(it) },
                                onCpuThreadsChange = { viewModel.setCpuThreads(it) },
                                onRunOfflineExtraction = { viewModel.runOfflineChunkedExtraction() },
                                onStemGainsChange = { voc, inst -> viewModel.setStemGains(voc, inst) },
                                onAcousticEqChange = { fmt, bass, trb, rev -> viewModel.setAcousticEq(fmt, bass, trb, rev) },
                                onPerformanceModeChange = { viewModel.setPerformanceMode(it) },
                                modifier = Modifier.testTag("stem_separation_panel")
                            )
                        }
                        2 -> {
                            // Web Media Import & Multitask Background Downloader
                            WebImportDownloadPanel(
                                isProbing = isProbing,
                                probeResult = lastProbeResult,
                                downloadTasks = downloadTasks,
                                onProbeUrl = { viewModel.probeWebUrl(it) },
                                onStartDownload = { probe, opt, autoLoad ->
                                    viewModel.startStreamDownload(probe, opt, autoLoad)
                                },
                                onCancelDownload = { viewModel.cancelStreamDownload(it) },
                                onLoadDownloadedTrack = { uri, title ->
                                    viewModel.addCustomMedia(uri, title, uri.path)
                                },
                                modifier = Modifier.testTag("web_import_panel")
                            )
                        }
                        3 -> {
                            // Pipelined MP4 Video Export (Instruments Muted + Arabic/Unicode Naming)
                            VideoExportPanel(
                                currentTrack = uiState.currentTrack,
                                exportState = exportState,
                                isolationStrength = uiState.isolationStrength,
                                neuralArchitecture = uiState.neuralArchitecture,
                                pitchSemitones = uiState.pitchSemitones,
                                tempo = uiState.tempo,
                                onFileNameChange = { viewModel.setExportFileName(it) },
                                onDeleteOriginalToggle = { viewModel.setDeleteOriginalAfterExport(it) },
                                onStartExport = { viewModel.startPipelinedVideoExport() },
                                onShareExported = { uri, name ->
                                    viewModel.shareExportedMp4(context, uri, name)
                                },
                                onPlayExportedFile = { uri, name ->
                                    viewModel.addCustomMedia(uri, name, uri.path)
                                },
                                modifier = Modifier.testTag("video_export_panel")
                            )
                        }
                        4 -> {
                            // Media Library, Device Storage Scan & Saved Audio Presets
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MediaPlaylistSheet(
                                    mediaList = uiState.mediaPlaylist,
                                    currentTrack = uiState.currentTrack,
                                    onSelectTrack = { viewModel.selectTrack(it) },
                                    onMediaPicked = { uri, name -> viewModel.addCustomMedia(uri, name) },
                                    onScanDeviceMedia = { viewModel.scanDeviceMedia() },
                                    modifier = Modifier.testTag("media_playlist_panel")
                                )

                                AudioProfilesSheet(
                                    profiles = savedProfiles,
                                    onApplyProfile = { viewModel.applyProfile(it) },
                                    onDeleteProfile = { viewModel.deleteProfile(it) },
                                    onOpenSaveDialog = { showSaveDialog = true },
                                    modifier = Modifier.testTag("audio_profiles_panel")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSaveDialog) {
            SaveProfileDialog(
                onDismiss = { showSaveDialog = false },
                onSave = { name, desc ->
                    viewModel.saveCurrentAsProfile(name, desc)
                }
            )
        }
    }
}

@Composable
fun AppHeader(
    currentLanguage: StudioLanguage = StudioLanguage.EN,
    onSelectLanguage: (StudioLanguage) -> Unit = {}
) {
    val strings = LocalStudioStrings.current
    var showLangMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = VioletPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "VocalCut Pro+",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VocalCut",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " Pro+",
                        color = CyanNeon,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = strings.appSubtitle,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 6-Language Switcher Pill
            Box {
                Surface(
                    onClick = { showLangMenu = true },
                    color = SurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("language_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLanguage.flagLabel,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    StudioLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = lang.nativeName,
                                        color = if (lang == currentLanguage) CyanNeon else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = lang.flagLabel,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            onClick = {
                                onSelectLanguage(lang)
                                showLangMenu = false
                            },
                            modifier = Modifier.testTag("lang_option_${lang.code}")
                        )
                    }
                }
            }

            // Live Zero-Lag DSP Indicator
            Surface(
                color = Color(0xFF0E1522),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(EmeraldAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.lowLatencyBadge,
                        color = EmeraldAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickVocalIsolationBar(
    stemMode: StemMode,
    isolationStrength: Float,
    onToggleVocalOnly: () -> Unit,
    onIsolationStrengthChange: (Float) -> Unit
) {
    val strings = LocalStudioStrings.current
    val isVocalIsolated = stemMode == StemMode.ISOLATE_VOCALS

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = BorderStroke(
            1.dp,
            if (isVocalIsolated) VocalColor.copy(alpha = 0.6f) else SurfaceBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_vocal_isolation_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Instant Toggle Button: Original Audio <-> Vocal Only
            Surface(
                onClick = onToggleVocalOnly,
                shape = RoundedCornerShape(8.dp),
                color = if (isVocalIsolated) VocalColor else Color(0xFF0F172A),
                border = BorderStroke(1.dp, if (isVocalIsolated) VocalColor else CyanNeon),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("quick_vocal_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVocalIsolated) Icons.Default.Mic else Icons.Default.MusicNote,
                        contentDescription = "Toggle Vocal Isolation",
                        tint = if (isVocalIsolated) Color.White else CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVocalIsolated) strings.vocalOnly else strings.originalAudio,
                        color = if (isVocalIsolated) Color.White else CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Vocal Isolation Strength Slider (0% - 100%)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.isolationStrength,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${(isolationStrength * 100).toInt()}%",
                        color = VocalColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = isolationStrength,
                    onValueChange = onIsolationStrengthChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = VocalColor,
                        activeTrackColor = VocalColor,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .testTag("quick_isolation_slider")
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
    val strings = LocalStudioStrings.current
    val tabs = listOf(
        TabItem(strings.tabPitchTempo, Icons.Default.Speed, PitchColor, "tab_pitch_tempo"),
        TabItem(strings.tabNeuralVocal, Icons.Default.Headphones, VocalColor, "tab_neural_vocal"),
        TabItem(strings.tabWebImport, Icons.Default.CloudDownload, CyanNeon, "tab_web_import"),
        TabItem(strings.tabExportMp4, Icons.Default.MovieCreation, EmeraldAccent, "tab_export_mp4"),
        TabItem(strings.tabLibraryPresets, Icons.Default.VideoLibrary, VioletLight, "tab_library_presets")
    )

    ScrollableTabRow(
        selectedTabIndex = activeTab,
        edgePadding = 0.dp,
        containerColor = SurfaceDark,
        contentColor = CyanNeon,
        indicator = { tabPositions ->
            if (activeTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = tabs[activeTab].color,
                    height = 3.dp
                )
            }
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
                modifier = Modifier.testTag(tab.tag),
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
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
        }
    }
}

private data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val tag: String
)
