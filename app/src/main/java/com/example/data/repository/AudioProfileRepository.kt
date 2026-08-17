package com.example.data.repository

import com.example.data.db.AudioProfileDao
import com.example.data.model.AudioProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AudioProfileRepository(private val dao: AudioProfileDao) {

    val allProfiles: Flow<List<AudioProfile>> = dao.getAllProfiles()

    fun getProfilesForVideo(videoKey: String): Flow<List<AudioProfile>> {
        return dao.getProfilesForVideo(videoKey)
    }

    suspend fun saveProfile(profile: AudioProfile): Long {
        return dao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: AudioProfile) {
        dao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(id: Int) {
        dao.deleteProfileById(id)
    }

    suspend fun seedDefaultProfilesIfNeeded() {
        val existing = dao.getAllProfiles()
        val list = existing.firstOrNull() ?: emptyList()
        if (list.isEmpty()) {
            val defaults = listOf(
                AudioProfile(
                    name = "Original Sound",
                    description = "Default bypass with no DSP alteration",
                    pitchSemitones = 0.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "ORIGINAL",
                    fxTarget = "ALL",
                    vocalGain = 1.0f,
                    instrumentalGain = 1.0f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Karaoke Mode (Vocals Removed)",
                    description = "Attenuates center vocals for sing-along",
                    pitchSemitones = 0.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "REMOVE_VOCALS",
                    fxTarget = "ALL",
                    vocalGain = 0.0f,
                    instrumentalGain = 1.2f,
                    bassBoost = 0.2f,
                    trebleBoost = 0.15f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Acapella Vocal Isolator",
                    description = "Extracts human vocals / speech cleanly",
                    pitchSemitones = 0.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "ISOLATE_VOCALS",
                    fxTarget = "VOCALS_ONLY",
                    vocalGain = 1.3f,
                    instrumentalGain = 0.0f,
                    vocalFormantBoost = 0.3f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Male to Female Voice (+4 st)",
                    description = "Raises pitch by major 3rd with vocal formant clarity",
                    pitchSemitones = 4.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "ORIGINAL",
                    fxTarget = "VOCALS_ONLY",
                    vocalFormantBoost = 0.4f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Female to Male Voice (-4 st)",
                    description = "Lowers pitch by major 3rd with bass richness",
                    pitchSemitones = -4.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "ORIGINAL",
                    fxTarget = "VOCALS_ONLY",
                    bassBoost = 0.35f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Vocal Practice (0.8x, -2 st)",
                    description = "Slow tempo and lowered pitch for learning songs",
                    pitchSemitones = -2.0f,
                    pitchCents = 0,
                    tempo = 0.8f,
                    stemMode = "ORIGINAL",
                    fxTarget = "ALL",
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Nightcore Dance (+3 st, 1.25x)",
                    description = "High energy speed & pitch up",
                    pitchSemitones = 3.0f,
                    pitchCents = 0,
                    tempo = 1.25f,
                    stemMode = "ORIGINAL",
                    fxTarget = "ALL",
                    trebleBoost = 0.25f,
                    isBuiltIn = true
                ),
                AudioProfile(
                    name = "Instrumental Transpose (+2 st)",
                    description = "Transposes backing track key while keeping vocals normal",
                    pitchSemitones = 2.0f,
                    pitchCents = 0,
                    tempo = 1.0f,
                    stemMode = "CUSTOM_MIX",
                    fxTarget = "INSTRUMENTAL_ONLY",
                    vocalGain = 1.0f,
                    instrumentalGain = 1.0f,
                    isBuiltIn = true
                )
            )
            dao.insertProfiles(defaults)
        }
    }
}
