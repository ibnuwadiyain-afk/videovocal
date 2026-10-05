package com.example.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class StreamPlatform(val displayName: String, val badgeColorHex: Long) {
    YOUTUBE("YouTube", 0xFFFF0033),
    TIKTOK("TikTok", 0xFF00F2FE),
    VIMEO("Vimeo", 0xFF1AB7EA),
    DAILYMOTION("Dailymotion", 0xFF0066DC),
    HLS_M3U8("HLS (.m3u8)", 0xFFF59E0B),
    DIRECT_MEDIA("Direct Stream", 0xFF10B981)
}

data class StreamOption(
    val id: String,
    val resolutionLabel: String,
    val format: String,
    val estimatedSizeMb: Float,
    val streamUrl: String = "",
    val isAudioOnly: Boolean = false
)

data class ProbeResult(
    val url: String,
    val title: String,
    val platform: StreamPlatform,
    val durationSec: Int,
    val probeTimeMs: Long,
    val streams: List<StreamOption>,
    val errorMessage: String? = null
)

enum class DownloadStatus {
    DOWNLOADING,
    VERIFYING_SECURITY,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadTaskItem(
    val id: String,
    val title: String,
    val url: String,
    val resolutionLabel: String,
    val platform: StreamPlatform,
    val downloadedMb: Float,
    val totalMb: Float,
    val speedMbps: Float,
    val progressPercent: Int,
    val status: DownloadStatus,
    val securityVerifiedNoHtml: Boolean = false,
    val localFileUri: Uri? = null,
    val errorMessage: String? = null
)

class WebMediaManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _isProbing = MutableStateFlow(false)
    val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

    private val _lastProbeResult = MutableStateFlow<ProbeResult?>(null)
    val lastProbeResult: StateFlow<ProbeResult?> = _lastProbeResult.asStateFlow()

    private val _downloadTasks = MutableStateFlow<List<DownloadTaskItem>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTaskItem>> = _downloadTasks.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun detectPlatform(url: String): StreamPlatform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> StreamPlatform.YOUTUBE
            lower.contains("tiktok.com") -> StreamPlatform.TIKTOK
            lower.contains("vimeo.com") -> StreamPlatform.VIMEO
            lower.contains("dailymotion.com") || lower.contains("dai.ly") -> StreamPlatform.DAILYMOTION
            lower.contains(".m3u8") -> StreamPlatform.HLS_M3U8
            else -> StreamPlatform.DIRECT_MEDIA
        }
    }

    /**
     * Fast Web Media Probe bounded strictly by a 4-second timeout.
     */
    suspend fun probeUrl(rawUrl: String): ProbeResult? = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return@withContext null
        }

        _isProbing.value = true
        val startMs = System.currentTimeMillis()
        val platform = detectPlatform(url)
        var detectedTitle: String? = null
        var contentLengthMb: Float? = null
        var probeError: String? = null
        val parsedStreams = mutableListOf<StreamOption>()

        val probeOutcome = withTimeoutOrNull(4000L) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; VocalCutPro)")
                    .build()
                okHttpClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        probeError = "HTTP ${response.code}"
                    }
                    val len = response.header("Content-Length")?.toLongOrNull()
                    if (len != null && len > 0) {
                        contentLengthMb = (len.toFloat() / (1024f * 1024f)).coerceAtLeast(0.1f)
                    }
                    val contentType = (response.header("Content-Type") ?: "").lowercase()
                    when {
                        url.lowercase().contains(".m3u8") || contentType.contains("mpegurl") -> {
                            val manifest = response.body?.string() ?: ""
                            parseHlsManifest(url, manifest, parsedStreams)
                        }
                        contentType.contains("text/html") -> {
                            val snippet = response.body?.string()?.take(16384) ?: ""
                            val titleRegex = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                            val extracted = titleRegex.find(snippet)?.groupValues?.getOrNull(1)
                            if (!extracted.isNullOrBlank()) {
                                detectedTitle = extracted
                                    .replace("&amp;", "&")
                                    .replace("&#39;", "'")
                                    .replace("&quot;", "\"")
                                    .trim()
                            }
                        }
                        else -> {
                            // Direct binary stream (MP4, WebM, MP3, WAV, M4A)
                            response.close()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                probeError = e.localizedMessage ?: "Network probe failed"
                false
            }
        }

        if (probeOutcome == null && probeError == null) {
            probeError = "Probe timed out (> 4s)"
        }

        val elapsed = (System.currentTimeMillis() - startMs).coerceAtMost(4000L)
        val cleanSlug = url.substringAfterLast('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() } ?: url.hostOrDefault()

        val finalTitle = detectedTitle ?: cleanSlug
        val baseSize = contentLengthMb ?: 12.0f

        val options = if (parsedStreams.isNotEmpty()) {
            parsedStreams
        } else {
            listOf(
                StreamOption(
                    id = "1080p",
                    resolutionLabel = "1080p Full HD",
                    format = "MP4 • H.264 + AAC",
                    estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", baseSize * 1.5f).toFloat(),
                    streamUrl = url
                ),
                StreamOption(
                    id = "720p",
                    resolutionLabel = "720p HD",
                    format = "MP4 • H.264 + AAC",
                    estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", baseSize).toFloat(),
                    streamUrl = url
                ),
                StreamOption(
                    id = "480p",
                    resolutionLabel = "480p SD",
                    format = "MP4 • H.264 + AAC",
                    estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", baseSize * 0.65f).toFloat(),
                    streamUrl = url
                ),
                StreamOption(
                    id = "360p",
                    resolutionLabel = "360p Fast",
                    format = "MP4 • H.264 + AAC",
                    estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", baseSize * 0.4f).toFloat(),
                    streamUrl = url
                ),
                StreamOption(
                    id = "audio_only",
                    resolutionLabel = "Audio Only",
                    format = "Audio Stream • Stereo",
                    estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", baseSize * 0.25f).toFloat(),
                    streamUrl = url,
                    isAudioOnly = true
                )
            )
        }

        val result = ProbeResult(
            url = url,
            title = finalTitle,
            platform = platform,
            durationSec = 0,
            probeTimeMs = elapsed,
            streams = options,
            errorMessage = probeError
        )
        _lastProbeResult.value = result
        _isProbing.value = false
        result
    }

    private fun parseHlsManifest(baseUrl: String, manifest: String, out: MutableList<StreamOption>) {
        val lines = manifest.lines()
        var currentRes: String? = null
        var currentBw: Int = 0
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                val resMatch = Regex("RESOLUTION=(\\d+x\\d+)").find(trimmed)?.groupValues?.getOrNull(1)
                val bwMatch = Regex("BANDWIDTH=(\\d+)").find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()
                currentRes = resMatch
                currentBw = bwMatch ?: 1500000
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentRes != null) {
                val height = currentRes.substringAfter('x')
                val resolvedUrl = if (trimmed.startsWith("http")) trimmed else {
                    baseUrl.substringBeforeLast('/') + "/" + trimmed
                }
                val estMb = ((currentBw / 8f) * 60f) / (1024f * 1024f)
                out.add(
                    StreamOption(
                        id = "${height}p_${out.size}",
                        resolutionLabel = "${height}p (${currentRes})",
                        format = "HLS .m3u8 Stream",
                        estimatedSizeMb = String.format(java.util.Locale.US, "%.1f", estMb.coerceAtLeast(1.0f)).toFloat(),
                        streamUrl = resolvedUrl
                    )
                )
                currentRes = null
            }
        }
    }

    private fun String.hostOrDefault(): String {
        return runCatching { Uri.parse(this).host ?: "Web Media" }.getOrDefault("Web Media")
    }

    /**
     * Enqueues a concurrent background download task with real-time bandwidth & MB tracking,
     * HTML masquerade security inspection, and optional auto-load into the player on completion.
     */
    fun startBackgroundDownload(
        probeResult: ProbeResult,
        streamOption: StreamOption,
        autoLoadWhenReady: Boolean,
        onReadyToLoad: (Uri, String) -> Unit
    ): String {
        val targetUrl = streamOption.streamUrl.ifEmpty { probeResult.url }
        val taskId = "dl_${System.currentTimeMillis()}_${streamOption.id}"
        val initialTask = DownloadTaskItem(
            id = taskId,
            title = "${probeResult.title} [${streamOption.resolutionLabel}]",
            url = targetUrl,
            resolutionLabel = streamOption.resolutionLabel,
            platform = probeResult.platform,
            downloadedMb = 0f,
            totalMb = streamOption.estimatedSizeMb,
            speedMbps = 0f,
            progressPercent = 0,
            status = DownloadStatus.DOWNLOADING
        )

        _downloadTasks.update { listOf(initialTask) + it }

        val job = scope.launch {
            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val ext = when {
                targetUrl.lowercase().contains(".mp3") -> "mp3"
                targetUrl.lowercase().contains(".wav") -> "wav"
                targetUrl.lowercase().contains(".m4a") -> "m4a"
                else -> "mp4"
            }
            val safeName = probeResult.title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(40)
                .trim()
                .ifEmpty { "media_stream" }
            val outFile = File(downloadsDir, "${safeName}_${streamOption.id}_${System.currentTimeMillis() % 10000}.$ext")

            try {
                val req = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; VocalCutPro)")
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        updateTask(taskId) {
                            it.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = "HTTP ${resp.code}: ${resp.message}"
                            )
                        }
                        return@use
                    }

                    val body = resp.body
                    if (body == null) {
                        updateTask(taskId) {
                            it.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = "Empty response stream"
                            )
                        }
                        return@use
                    }

                    val contentLength = body.contentLength()
                    val totalBytes = if (contentLength > 0) contentLength else (streamOption.estimatedSizeMb * 1024 * 1024).toLong()
                    val totalMb = (totalBytes.toFloat() / (1024f * 1024f)).coerceAtLeast(0.1f)

                    val inputStream = body.byteStream()
                    val buffer = ByteArray(8192)
                    var bytesReadTotal = 0L
                    val startTime = System.currentTimeMillis()
                    var firstChunkChecked = false

                    FileOutputStream(outFile).use { fos ->
                        while (isActive) {
                            val read = inputStream.read(buffer)
                            if (read == -1) break

                            if (!firstChunkChecked) {
                                firstChunkChecked = true
                                val headerSlice = buffer.copyOfRange(0, minOf(read, 256))
                                if (isHtmlMasquerading(headerSlice)) {
                                    fos.close()
                                    outFile.delete()
                                    updateTask(taskId) {
                                        it.copy(
                                            status = DownloadStatus.FAILED,
                                            errorMessage = "Blocked HTML page masquerading as media stream. Provide a direct media URL."
                                        )
                                    }
                                    return@use
                                }
                            }

                            fos.write(buffer, 0, read)
                            bytesReadTotal += read

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                            val curMb = bytesReadTotal.toFloat() / (1024f * 1024f)
                            val speed = curMb / elapsedSec
                            val pct = if (contentLength > 0) {
                                ((bytesReadTotal * 100L) / contentLength).toInt().coerceIn(1, 99)
                            } else {
                                50
                            }

                            updateTask(taskId) {
                                it.copy(
                                    downloadedMb = curMb,
                                    totalMb = maxOf(totalMb, curMb),
                                    speedMbps = speed,
                                    progressPercent = pct,
                                    status = DownloadStatus.DOWNLOADING
                                )
                            }
                        }
                    }

                    if (!isActive) {
                        outFile.delete()
                        return@use
                    }

                    updateTask(taskId) {
                        it.copy(
                            progressPercent = 98,
                            status = DownloadStatus.VERIFYING_SECURITY
                        )
                    }

                    if (!outFile.exists() || outFile.length() == 0L) {
                        outFile.delete()
                        updateTask(taskId) {
                            it.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = "Downloaded 0 bytes"
                            )
                        }
                        return@use
                    }

                    val finalMb = (outFile.length().toFloat() / (1024f * 1024f)).coerceAtLeast(0.01f)
                    val uri = Uri.fromFile(outFile)
                    updateTask(taskId) {
                        it.copy(
                            downloadedMb = finalMb,
                            totalMb = finalMb,
                            speedMbps = 0f,
                            progressPercent = 100,
                            status = DownloadStatus.COMPLETED,
                            securityVerifiedNoHtml = true,
                            localFileUri = uri
                        )
                    }

                    if (autoLoadWhenReady) {
                        withContext(Dispatchers.Main) {
                            onReadyToLoad(uri, "${probeResult.title} (${streamOption.resolutionLabel})")
                        }
                    }
                }
            } catch (e: Exception) {
                outFile.delete()
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Download failed"
                    )
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }

        activeJobs[taskId] = job
        return taskId
    }

    fun cancelDownload(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        updateTask(taskId) {
            it.copy(
                status = DownloadStatus.CANCELLED,
                speedMbps = 0f
            )
        }
    }

    /**
     * Security check that inspects downloaded file headers to reject HTML pages
     * (<!DOCTYPE html>, <html>, <head>) masquerading as binary media streams.
     */
    fun isHtmlMasquerading(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val prefix = String(bytes, 0, minOf(bytes.size, 256), Charsets.UTF_8).trimStart().lowercase()
        return prefix.startsWith("<!doctype html") ||
            prefix.startsWith("<html") ||
            prefix.startsWith("<head") ||
            prefix.startsWith("<script")
    }

    private fun updateTask(taskId: String, transform: (DownloadTaskItem) -> DownloadTaskItem) {
        _downloadTasks.update { list ->
            list.map { if (it.id == taskId) transform(it) else it }
        }
    }
}
