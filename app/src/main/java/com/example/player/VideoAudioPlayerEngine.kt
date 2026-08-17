package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.dsp.AudioEngineStats
import com.example.dsp.DspPerformanceMode
import com.example.dsp.FxTarget
import com.example.dsp.StemMode
import com.example.dsp.VisualizerFrame
import com.example.dsp.VocalCutAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

@OptIn(UnstableApi::class)
class VideoAudioPlayerEngine(private val context: Context) {

    val vocalCutProcessor = VocalCutAudioProcessor()

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    private val _loopABEnabled = MutableStateFlow(false)
    val loopABEnabled = _loopABEnabled.asStateFlow()

    private val _loopStartMs = MutableStateFlow(0L)
    val loopStartMs = _loopStartMs.asStateFlow()

    private val _loopEndMs = MutableStateFlow(0L)
    val loopEndMs = _loopEndMs.asStateFlow()

    private val _visualizerFrame = MutableStateFlow(VisualizerFrame())
    val visualizerFrame = _visualizerFrame.asStateFlow()

    private val _engineStats = MutableStateFlow(AudioEngineStats())
    val engineStats = _engineStats.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Active DSP variables
    private var currentPitchSemitones: Float = 0.0f
    private var currentPitchCents: Int = 0
    private var currentTempo: Float = 1.0f
    private var syncVideoWithTempo: Boolean = true

    init {
        initExoPlayer()
        vocalCutProcessor.visualizerListener = { frame ->
            _visualizerFrame.value = frame
        }
    }

    private fun initExoPlayer() {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf<AudioProcessor>(vocalCutProcessor))
                    .build()
            }
        }

        exoPlayer = ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) startProgressTracking() else stopProgressTracking()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = exoPlayer?.duration?.coerceAtLeast(1L) ?: 1L
                        if (_loopEndMs.value == 0L) {
                            _loopEndMs.value = _durationMs.value
                        }
                    }
                }
            })
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun playMedia(mediaUri: Uri, title: String) {
        val player = exoPlayer ?: return
        val mediaItem = MediaItem.fromUri(mediaUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        _loopStartMs.value = 0L
        _loopEndMs.value = 0L
        _loopABEnabled.value = false
        applyDspParams()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun seekRelative(offsetMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
        _currentPositionMs.value = newPos
    }

    /**
     * Pitch Shift Control:
     * semitones: -12.0f to +12.0f
     * cents: -50 to +50
     */
    fun setPitch(semitones: Float, cents: Int = 0) {
        currentPitchSemitones = semitones
        currentPitchCents = cents
        applyDspParams()
    }

    /**
     * Tempo / Speed Control:
     * tempo: 0.5f to 2.0f
     */
    fun setTempo(tempo: Float, syncVideo: Boolean = true) {
        currentTempo = tempo.coerceIn(0.5f, 2.0f)
        syncVideoWithTempo = syncVideo
        applyDspParams()
    }

    fun setSyncVideoWithTempo(sync: Boolean) {
        syncVideoWithTempo = sync
        applyDspParams()
    }

    fun setStemMode(mode: StemMode) {
        vocalCutProcessor.stemMode = mode
    }

    fun setFxTarget(target: FxTarget) {
        vocalCutProcessor.fxTarget = target
    }

    fun setPerformanceMode(mode: DspPerformanceMode) {
        vocalCutProcessor.performanceMode = mode
        _engineStats.value = _engineStats.value.copy(
            latencyMs = if (mode == DspPerformanceMode.FAST_WSOLA) 8 else 16,
            cpuLoadPercent = if (mode == DspPerformanceMode.FAST_WSOLA) 3 else 7
        )
    }

    fun setStemGains(vocal: Float, instrumental: Float) {
        vocalCutProcessor.vocalGain = vocal
        vocalCutProcessor.instrumentalGain = instrumental
    }

    fun setAcousticEq(formant: Float, bass: Float, treble: Float, reverb: Float) {
        vocalCutProcessor.vocalFormantBoost = formant
        vocalCutProcessor.bassBoost = bass
        vocalCutProcessor.trebleBoost = treble
        vocalCutProcessor.reverbLevel = reverb
    }

    fun setLoopAB(enabled: Boolean, startMs: Long? = null, endMs: Long? = null) {
        _loopABEnabled.value = enabled
        startMs?.let { _loopStartMs.value = it }
        endMs?.let { _loopEndMs.value = it }
    }

    private fun applyDspParams() {
        val player = exoPlayer ?: return
        // Total semitones including cents fine tuning
        val totalSemitones = currentPitchSemitones + (currentPitchCents / 100.0f)
        // Pitch factor = 2 ^ (semitones / 12)
        val pitchMultiplier = 2.0.pow(totalSemitones.toDouble() / 12.0).toFloat()

        // ExoPlayer PlaybackParameters adjusts speed and pitch in real-time
        player.playbackParameters = PlaybackParameters(currentTempo, pitchMultiplier)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos

                    // Check A-B repeat
                    if (_loopABEnabled.value && _loopEndMs.value > _loopStartMs.value) {
                        if (pos >= _loopEndMs.value) {
                            player.seekTo(_loopStartMs.value)
                        }
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}
