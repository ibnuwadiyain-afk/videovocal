package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_profiles")
data class AudioProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val videoKey: String = "", // empty for global presets, or specific video uri
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Int = 0,
    val tempo: Float = 1.0f,
    val stemMode: String = "ORIGINAL", // ORIGINAL, ISOLATE_VOCALS, REMOVE_VOCALS, CUSTOM_MIX
    val fxTarget: String = "ALL", // ALL, VOCALS_ONLY, INSTRUMENTAL_ONLY
    val vocalGain: Float = 1.0f,
    val instrumentalGain: Float = 1.0f,
    val vocalFormantBoost: Float = 0.0f,
    val bassBoost: Float = 0.0f,
    val trebleBoost: Float = 0.0f,
    val reverbLevel: Float = 0.0f,
    val isBuiltIn: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
