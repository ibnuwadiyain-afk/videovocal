package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AudioProfile
import com.example.data.repository.AudioProfileRepository
import com.example.dsp.AudioEngineStats
import com.example.dsp.DspPerformanceMode
import com.example.dsp.FxTarget
import com.example.dsp.StemMode
import com.example.dsp.VisualizerFrame
import com.example.player.MediaItemInfo
import com.example.player.SampleMediaProvider
import com.example.player.VideoAudioPlayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrack: MediaItemInfo? = null,
    val mediaPlaylist: List<MediaItemInfo> = emptyList(),
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Int = 0,
    val tempo: Float = 1.0f,
    val stemMode: StemMode = StemMode.ORIGINAL,
    val fxTarget: FxTarget = FxTarget.ALL,
    val performanceMode: DspPerformanceMode = DspPerformanceMode.HIGH_QUALITY,
    val vocalGain: Float = 1.0f,
    val instrumentalGain: Float = 1.0f,
    val vocalFormantBoost: Float = 0.0f,
    val bassBoost: Float = 0.0f,
    val trebleBoost: Float = 0.0f,
    val reverbLevel: Float = 0.0f,
    val syncVideoWithTempo: Boolean = true,
    val hudNotification: String? = null,
    val activeTab: Int = 0 // 0: FX (Pitch/Tempo), 1: Stem Mixer, 2: EQ, 3: Presets
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val engine = VideoAudioPlayerEngine(application.applicationContext)

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

    private var hudDismissJob: Job? = null

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
            val samples = SampleMediaProvider.getSampleTracks(application.applicationContext)
            _uiState.value = _uiState.value.copy(
                mediaPlaylist = samples,
                currentTrack = samples.firstOrNull()
            )
            samples.firstOrNull()?.let { firstTrack ->
                engine.playMedia(firstTrack.uri, firstTrack.title)
            }
        }
    }

    fun selectTrack(track: MediaItemInfo) {
        _uiState.value = _uiState.value.copy(currentTrack = track)
        engine.playMedia(track.uri, track.title)
        showHud("Loaded: ${track.title}")
    }

    fun addCustomMedia(uri: Uri, title: String) {
        val customItem = MediaItemInfo(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            subtitle = "Device Offline Media",
            uri = uri,
            isSample = false
        )
        val updatedList = listOf(customItem) + _uiState.value.mediaPlaylist
        _uiState.value = _uiState.value.copy(
            mediaPlaylist = updatedList,
            currentTrack = customItem
        )
        engine.playMedia(uri, title)
        showHud("Opened: $title")
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
