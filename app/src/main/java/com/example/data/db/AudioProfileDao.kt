package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AudioProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioProfileDao {
    @Query("SELECT * FROM audio_profiles ORDER BY isBuiltIn DESC, timestamp DESC")
    fun getAllProfiles(): Flow<List<AudioProfile>>

    @Query("SELECT * FROM audio_profiles WHERE videoKey = :videoKey OR videoKey = '' ORDER BY timestamp DESC")
    fun getProfilesForVideo(videoKey: String): Flow<List<AudioProfile>>

    @Query("SELECT * FROM audio_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): AudioProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudioProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<AudioProfile>)

    @Update
    suspend fun updateProfile(profile: AudioProfile)

    @Delete
    suspend fun deleteProfile(profile: AudioProfile)

    @Query("DELETE FROM audio_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
