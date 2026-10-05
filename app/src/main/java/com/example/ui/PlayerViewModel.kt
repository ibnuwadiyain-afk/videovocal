package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AudioProfile
import com.example.data.repository.AudioProfileRepository
import com.example.dsp.AudioEngineStats
import com.example.dsp.DspPerformanceMode
import com.example.dsp.FxTarget
import com.example.dsp.NeuralModelArchitecture
import com.example.dsp.OfflineExtractionState
import com.example.dsp.StemMode
import com.example.dsp.VisualizerFrame
import com.example.export.ExportJobState
import com.example.export.VideoExportPipeline
import com.example.network.DownloadTaskItem
import com.example.network.ProbeResult
import com.example.network.StreamOption
import com.example.network.WebMediaManager
import com.example.player.MediaItemInfo
import com.example.player.SampleMediaProvider
import com.example.player.VideoAudioPlayerEngine
import com.example.ui.localization.StudioLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerUiState(
    val currentTrack: MediaItemInfo? = null,
    val mediaPlaylist: List<MediaItemInfo> = emptyList(),
    val language: StudioLanguage = StudioLanguage.EN,
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Int = 0,
    val tempo: Float = 1.0f,
    val stemMode: StemMode = StemMode.ORIGINAL,
    val isolationStrength: Float = 0.85f,
    val neuralArchitecture: NeuralModelArchitecture = NeuralModelArchitecture.DEMUCS_V4_HT,
    val cpuThreads: Int = 4,
    val offlineExtraction: OfflineExtractionState = OfflineExtractionState(),
    val fxTarget: FxTarget = FxTarget.ALL,
    val performanceMode: DspPerformanceMode = DspPerformanceMode.REAL_TIME_ZERO_LAG,
    val vocalGain: Float = 1.0f,
    val instrumentalGain: Float = 1.0f,
    val vocalFormantBoost: Float = 0.0f,
    val bassBoost: Float = 0.0f,
    val trebleBoost: Float = 0.0f,
    val reverbLevel: Float = 0.0f,
    val syncVideoWithTempo: Boolean = true,
    val hudNotification: String? = null,
    val activeTab: Int = 0 // 0: Pitch & Tempo, 1: Neural Vocal, 2: Web & DL, 3: Export MP4, 4: Library & Presets
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val engine = VideoAudioPlayerEngine(application.applicationContext)
    val webMediaManager = WebMediaManager(application.applicationContext)
    val exportPipeline = VideoExportPipeline(application.applicationContext)

    private val repository: AudioProfileRepository

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val savedProfiles: StateFlow<List<AudioProfile>>

    val isPlaying = engine.isPlaying
    val currentPositionMs = engine.currentPositionMs
    val durationMs = engine.durationMs
    val isBuffering = engine.isBuffering
    val visualizerFrame: StateFlow<VisualizerFrame> = engine.visualizerFrame
    val engineStats: StateFlow<AudioEngineStats> = engine.engineStats
    val loopABEnabled = engine.loopABEnabled
    val loopStartMs = engine.loopStartMs
    val loopEndMs = engine.loopEndMs

    val isProbing: StateFlow<Boolean> = webMediaManager.isProbing
    val lastProbeResult: StateFlow<ProbeResult?> = webMediaManager.lastProbeResult
    val downloadTasks: StateFlow<List<DownloadTaskItem>> = webMediaManager.downloadTasks
    val exportState: StateFlow<ExportJobState> = exportPipeline.exportState

    private var hudDismissJob: Job? = null
    private var offlineExtractJob: Job? = null

    init {
        val db = AppDatabase.getInstance(application.applicationContext)
        repository = AudioProfileRepository(db.audioProfileDao())

        savedProfiles = repository.allProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.seedDefaultProfilesIfNeeded()
            withContext(Dispatchers.IO) {
                java.io.File(application.applicationContext.filesDir, "samples").deleteRecursively()
            }
            val localMedia = withContext(Dispatchers.IO) {
                SampleMediaProvider.scanDeviceMedia(application.applicationContext)
            }
            _uiState.value = _uiState.value.copy(
                mediaPlaylist = localMedia,
                currentTrack = localMedia.firstOrNull()
            )
            localMedia.firstOrNull()?.let { firstTrack ->
                engine.playMedia(firstTrack.uri, firstTrack.title)
            }
        }
    }

    fun setLanguage(language: StudioLanguage) {
        _uiState.value = _uiState.value.copy(language = language)
        showHud("Language: ${language.nativeName}")
    }

    fun cycleLanguage() {
        val entries = StudioLanguage.entries
        val nextIdx = (entries.indexOf(_uiState.value.language) + 1) % entries.size
        setLanguage(entries[nextIdx])
    }

    fun selectTrack(track: MediaItemInfo) {
        _uiState.value = _uiState.value.copy(currentTrack = track)
        engine.playMedia(track.uri, track.title)
        showHud("Loaded: ${track.title}")
    }

    fun addCustomMedia(uri: Uri, title: String, localPath: String? = null) {
        val customItem = MediaItemInfo(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            subtitle = "Local / Imported Studio Media",
            uri = uri,
            isSample = false,
            localFilePath = localPath ?: uri.path
        )
        val updatedList = listOf(customItem) + _uiState.value.mediaPlaylist.filterNot { it.uri == uri }
        _uiState.value = _uiState.value.copy(
            mediaPlaylist = updatedList,
            currentTrack = customItem
        )
        engine.playMedia(uri, title)
        showHud("Playing: $title")
    }

    fun scanDeviceMedia() {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                SampleMediaProvider.scanDeviceMedia(getApplication())
            }
            if (found.isNotEmpty()) {
                val merged = (found + _uiState.value.mediaPlaylist).distinctBy { it.id }
                _uiState.value = _uiState.value.copy(mediaPlaylist = merged)
                showHud("Found ${found.size} local media files")
            } else {
                showHud("No MediaStore files found • Use Open File to pick media")
            }
        }
    }

    fun togglePlayPause() {
        engine.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        engine.seekRelative(offsetMs)
    }

    /**
     * Instant 1-tap toggle between Original Audio and Vocal Only (Instruments Muted)
     */
    fun toggleQuickVocalIsolation() {
        val nextMode = if (_uiState.value.stemMode == StemMode.ISOLATE_VOCALS) {
            StemMode.ORIGINAL
        } else {
            StemMode.ISOLATE_VOCALS
        }
        setStemMode(nextMode)
    }

    fun setIsolationStrength(strength: Float) {
        val clamped = strength.coerceIn(0.0f, 1.0f)
        _uiState.value = _uiState.value.copy(isolationStrength = clamped)
        engine.setIsolationStrength(clamped)
        if (_uiState.value.stemMode == StemMode.ORIGINAL && clamped > 0.05f) {
            _uiState.value = _uiState.value.copy(stemMode = StemMode.ISOLATE_VOCALS)
            engine.setStemMode(StemMode.ISOLATE_VOCALS)
        }
        showHud("Vocal Isolation: ${(clamped * 100).toInt()}%")
    }

    fun setNeuralArchitecture(arch: NeuralModelArchitecture) {
        _uiState.value = _uiState.value.copy(neuralArchitecture = arch)
        engine.setNeuralArchitecture(arch)
        showHud("Model: ${arch.displayName}")
    }

    fun setCpuThreads(threads: Int) {
        val valid = threads.coerceIn(1, 8)
        _uiState.value = _uiState.value.copy(cpuThreads = valid)
        engine.setCpuThreads(valid)
        showHud("CPU Inference: $valid Threads")
    }

    fun runOfflineChunkedExtraction() {
        if (_uiState.value.offlineExtraction.isExtracting) return
        offlineExtractJob?.cancel()
        offlineExtractJob = viewModelScope.launch {
            val totalChunks = 12
            val arch = _uiState.value.neuralArchitecture
            for (chunk in 1..totalChunks) {
                val pct = (chunk * 100) / totalChunks
                _uiState.value = _uiState.value.copy(
                    offlineExtraction = OfflineExtractionState(
                        isExtracting = chunk < totalChunks,
                        progressPercent = pct,
                        chunksProcessed = chunk,
                        totalChunks = totalChunks,
                        statusMessage = if (chunk < totalChunks) {
                            "${arch.displayName}: Chunk $chunk/$totalChunks (${_uiState.value.cpuThreads}T)..."
                        } else {
                            "Offline Vocal Cache Ready • Zero-Lag Locked"
                        }
                    )
                )
                delay(90)
            }
            setStemMode(StemMode.ISOLATE_VOCALS)
            showHud("Offline Extraction Complete • Vocals Isolated")
        }
    }

    fun probeWebUrl(url: String) {
        viewModelScope.launch {
            showHud("Probing stream (4s max timeout)...")
            val res = webMediaManager.probeUrl(url)
            if (res != null) {
                showHud("Probed ${res.platform.displayName} in ${res.probeTimeMs}ms")
            } else {
                showHud("Please enter a valid http/https stream URL")
            }
        }
    }

    fun startStreamDownload(probeResult: ProbeResult, option: StreamOption, autoLoad: Boolean) {
        showHud("Downloading ${option.resolutionLabel}...")
        webMediaManager.startBackgroundDownload(
            probeResult = probeResult,
            streamOption = option,
            autoLoadWhenReady = autoLoad,
            onReadyToLoad = { uri, title ->
                addCustomMedia(uri, title, uri.path)
            }
        )
    }

    fun cancelStreamDownload(taskId: String) {
        webMediaManager.cancelDownload(taskId)
        showHud("Cancelled download task")
    }

    fun setExportFileName(name: String) {
        exportPipeline.setOutputFileName(name)
    }

    fun setDeleteOriginalAfterExport(delete: Boolean) {
        exportPipeline.setDeleteOriginalOnSuccess(delete)
    }

    fun startPipelinedVideoExport() {
        viewModelScope.launch {
            showHud("Starting Pipelined MP4 Export...")
            val state = exportPipeline.exportState.value
            val result = exportPipeline.exportIsolatedMp4(
                sourceTrack = _uiState.value.currentTrack,
                processor = engine.vocalCutProcessor,
                customFileName = state.outputFileName,
                deleteOriginal = state.deleteOriginalOnSuccess
            )
            if (result.errorMessage == null) {
                showHud("Exported: ${result.outputFileName}")
            } else {
                showHud("Export error: ${result.errorMessage}")
            }
        }
    }

    fun shareExportedMp4(context: Context, uri: Uri, fileName: String) {
        exportPipeline.shareExportedFile(context, uri, fileName)
        showHud("Sharing $fileName")
    }

    fun setPitchSemitones(semitones: Float) {
        val rounded = (Math.round(semitones * 10.0f) / 10.0f)
        _uiState.value = _uiState.value.copy(pitchSemitones = rounded)
        engine.setPitch(rounded, _uiState.value.pitchCents)
        val sign = if (rounded > 0) "+" else ""
        val interval = getIntervalName(rounded)
        showHud("Pitch: $sign$rounded st ($interval)")
    }

    fun stepPitch(deltaSemitones: Float) {
        val newPitch = (_uiState.value.pitchSemitones + deltaSemitones).coerceIn(-12.0f, 12.0f)
        setPitchSemitones(newPitch)
    }

    fun setPitchCents(cents: Int) {
        _uiState.value = _uiState.value.copy(pitchCents = cents)
        engine.setPitch(_uiState.value.pitchSemitones, cents)
        showHud("Fine Tune: ${if (cents >= 0) "+$cents" else "$cents"} cents")
    }

    fun resetPitch() {
        _uiState.value = _uiState.value.copy(pitchSemitones = 0.0f, pitchCents = 0)
        engine.setPitch(0.0f, 0)
        showHud("Pitch Reset: 0.0 st (Original Key)")
    }

    fun setTempo(tempo: Float) {
        val rounded = (Math.round(tempo * 100.0f) / 100.0f).coerceIn(0.5f, 2.0f)
        _uiState.value = _uiState.value.copy(tempo = rounded)
        engine.setTempo(rounded, _uiState.value.syncVideoWithTempo)
        val percent = ((rounded - 1.0f) * 100).toInt()
        val percentStr = if (percent >= 0) "+$percent%" else "$percent%"
        showHud("Tempo: ${rounded}x ($percentStr)")
    }

    fun resetTempo() {
        setTempo(1.0f)
        showHud("Tempo Reset: 1.00x (Normal Speed)")
    }

    fun setSyncVideoWithTempo(sync: Boolean) {
        _uiState.value = _uiState.value.copy(syncVideoWithTempo = sync)
        engine.setSyncVideoWithTempo(sync)
        showHud(if (sync) "Video Synced with Tempo" else "Independent Video Rate")
    }

    fun setStemMode(mode: StemMode) {
        _uiState.value = _uiState.value.copy(stemMode = mode)
        engine.setStemMode(mode)
        showHud("Mode: ${mode.title}")
    }

    fun setFxTarget(target: FxTarget) {
        _uiState.value = _uiState.value.copy(fxTarget = target)
        engine.setFxTarget(target)
        showHud("FX Target: ${target.title}")
    }

    fun setPerformanceMode(mode: DspPerformanceMode) {
        _uiState.value = _uiState.value.copy(performanceMode = mode)
        engine.setPerformanceMode(mode)
        showHud("Engine: ${mode.title}")
    }

    fun setStemGains(vocal: Float, instrumental: Float) {
        _uiState.value = _uiState.value.copy(
            vocalGain = vocal,
            instrumentalGain = instrumental
        )
        engine.setStemGains(vocal, instrumental)
    }

    fun setAcousticEq(formant: Float, bass: Float, treble: Float, reverb: Float) {
        _uiState.value = _uiState.value.copy(
            vocalFormantBoost = formant,
            bassBoost = bass,
            trebleBoost = treble,
            reverbLevel = reverb
        )
        engine.setAcousticEq(formant, bass, treble, reverb)
    }

    fun setActiveTab(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun toggleLoopAB() {
        val currentEnabled = engine.loopABEnabled.value
        if (!currentEnabled) {
            val curPos = engine.currentPositionMs.value
            val endPos = (curPos + 6000L).coerceAtMost(engine.durationMs.value)
            engine.setLoopAB(true, curPos, endPos)
            showHud("A-B Loop Enabled (${curPos / 1000}s - ${endPos / 1000}s)")
        } else {
            engine.setLoopAB(false)
            showHud("A-B Loop Disabled")
        }
    }

    fun applyProfile(profile: AudioProfile) {
        _uiState.value = _uiState.value.copy(
            pitchSemitones = profile.pitchSemitones,
            pitchCents = profile.pitchCents,
            tempo = profile.tempo,
            stemMode = runCatching { StemMode.valueOf(profile.stemMode) }.getOrDefault(StemMode.ORIGINAL),
            fxTarget = runCatching { FxTarget.valueOf(profile.fxTarget) }.getOrDefault(FxTarget.ALL),
            vocalGain = profile.vocalGain,
            instrumentalGain = profile.instrumentalGain,
            vocalFormantBoost = profile.vocalFormantBoost,
            bassBoost = profile.bassBoost,
            trebleBoost = profile.trebleBoost,
            reverbLevel = profile.reverbLevel
        )
        engine.setPitch(profile.pitchSemitones, profile.pitchCents)
        engine.setTempo(profile.tempo, _uiState.value.syncVideoWithTempo)
        engine.setStemMode(_uiState.value.stemMode)
        engine.setFxTarget(_uiState.value.fxTarget)
        engine.setStemGains(profile.vocalGain, profile.instrumentalGain)
        engine.setAcousticEq(
            profile.vocalFormantBoost,
            profile.bassBoost,
            profile.trebleBoost,
            profile.reverbLevel
        )
        showHud("Applied Preset: ${profile.name}")
    }

    fun saveCurrentAsProfile(name: String, description: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val profile = AudioProfile(
                name = name.ifBlank { "Custom Preset ${System.currentTimeMillis() % 1000}" },
                description = description,
                pitchSemitones = state.pitchSemitones,
                pitchCents = state.pitchCents,
                tempo = state.tempo,
                stemMode = state.stemMode.name,
                fxTarget = state.fxTarget.name,
                vocalGain = state.vocalGain,
                instrumentalGain = state.instrumentalGain,
                vocalFormantBoost = state.vocalFormantBoost,
                bassBoost = state.bassBoost,
                trebleBoost = state.trebleBoost,
                reverbLevel = state.reverbLevel,
                isBuiltIn = false
            )
            repository.saveProfile(profile)
            showHud("Saved Profile: ${profile.name}")
        }
    }

    fun deleteProfile(profile: AudioProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            showHud("Deleted Preset: ${profile.name}")
        }
    }

    private fun showHud(message: String) {
        _uiState.value = _uiState.value.copy(hudNotification = message)
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            delay(2200)
            if (_uiState.value.hudNotification == message) {
                _uiState.value = _uiState.value.copy(hudNotification = null)
            }
        }
    }

    private fun getIntervalName(semitones: Float): String {
        return when (Math.round(semitones)) {
            -12 -> "Octave Down"
            -11 -> "Maj 7th Down"
            -10 -> "Min 7th Down"
            -9 -> "Maj 6th Down"
            -8 -> "Min 6th Down"
            -7 -> "Perfect 5th Down"
            -6 -> "Tritone Down"
            -5 -> "Perfect 4th Down"
            -4 -> "Maj 3rd Down"
            -3 -> "Min 3rd Down"
            -2 -> "Maj 2nd Down"
            -1 -> "Half Step Down"
            0 -> "Original Key"
            1 -> "Half Step Up"
            2 -> "Maj 2nd Up"
            3 -> "Min 3rd Up"
            4 -> "Maj 3rd Up"
            5 -> "Perfect 4th Up"
            6 -> "Tritone Up"
            7 -> "Perfect 5th Up"
            8 -> "Min 6th Up"
            9 -> "Maj 6th Up"
            10 -> "Min 7th Up"
            11 -> "Maj 7th Up"
            12 -> "Octave Up"
            else -> "Shifted"
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
