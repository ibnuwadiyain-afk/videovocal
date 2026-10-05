package com.example.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class MediaItemInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val uri: Uri,
    val isSample: Boolean = false,
    val durationMs: Long = 0L,
    val localFilePath: String? = null
)

object SampleMediaProvider {

    /**
     * Scans device storage via MediaStore for local Video and Audio files.
     */
    fun scanDeviceMedia(context: Context): List<MediaItemInfo> {
        val results = mutableListOf<MediaItemInfo>()
        runCatching {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                var count = 0
                while (cursor.moveToNext() && count < 25) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id.mp4"
                    val dur = cursor.getLong(durCol).coerceAtLeast(0L)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    results.add(
                        MediaItemInfo(
                            id = "mediastore_vid_$id",
                            title = name,
                            subtitle = "Local Video • MediaStore",
                            uri = contentUri,
                            isSample = false,
                            durationMs = dur
                        )
                    )
                    count++
                }
            }
        }

        runCatching {
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
            )
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                var count = 0
                while (cursor.moveToNext() && count < 25) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Audio_$id"
                    val dur = cursor.getLong(durCol).coerceAtLeast(0L)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    results.add(
                        MediaItemInfo(
                            id = "mediastore_aud_$id",
                            title = name,
                            subtitle = "Local Audio • MediaStore",
                            uri = contentUri,
                            isSample = false,
                            durationMs = dur
                        )
                    )
                    count++
                }
            }
        }
        return results
    }
}
