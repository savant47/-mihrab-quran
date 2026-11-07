package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalKhatmaStorage
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class KhatmaRepository(context: Context) {

    private val localStorage: LocalKhatmaStorage = LocalKhatmaStorage.getInstance(context.applicationContext)

    fun observeUserProfile(): Flow<User> {
        return localStorage.guestUserFlow
    }

    fun saveUserProfile(user: User) {
        localStorage.saveGuestUser(user)
    }

    fun observeKhatmaTracks(): Flow<List<KhatmaTrack>> {
        return localStorage.tracksFlow
    }

    fun createKhatmaTrack(trackName: String): String {
        val newId = "track_${System.currentTimeMillis()}"
        val newTrack = KhatmaTrack(
            trackId = newId,
            trackName = trackName,
            khatmaNumber = 1,
            currentSurah = "الفاتحة",
            currentAyah = 1,
            currentPage = 1,
            completedRubCount = 0,
            progressPercentage = 0.0,
            isCompleted = false,
            isFixedSurahWerd = false
        )
        val currentTracks = localStorage.tracksFlow.value.toMutableList()
        currentTracks.add(newTrack)
        localStorage.saveTracks(currentTracks)
        return newId
    }

    fun createFixedSurahTrack(trackName: String, surahName: String, startPage: Int): String {
        val newId = "surah_werd_${System.currentTimeMillis()}"
        val clampedStartPage = startPage.coerceIn(1, 604)
        val newTrack = KhatmaTrack(
            trackId = newId,
            trackName = trackName,
            khatmaNumber = 1,
            currentSurah = surahName,
            currentAyah = 1,
            currentPage = clampedStartPage,
            completedRubCount = 0,
            progressPercentage = 0.0,
            isCompleted = false,
            isFixedSurahWerd = true,
            fixedSurahName = surahName,
            fixedSurahStartPage = clampedStartPage
        )
        val currentTracks = localStorage.tracksFlow.value.toMutableList()
        currentTracks.add(newTrack)
        localStorage.saveTracks(currentTracks)
        return newId
    }

    fun renameKhatmaTrack(trackId: String, newName: String) {
        val currentTracks = localStorage.tracksFlow.value.toMutableList()
        val index = currentTracks.indexOfFirst { it.trackId == trackId }
        if (index >= 0) {
            currentTracks[index] = currentTracks[index].copy(trackName = newName)
            localStorage.saveTracks(currentTracks)
        }
    }

    fun deleteKhatmaTrack(trackId: String) {
        if (trackId == "main") return
        val currentTracks = localStorage.tracksFlow.value.toMutableList()
        val index = currentTracks.indexOfFirst { it.trackId == trackId }
        if (index >= 0) {
            currentTracks.removeAt(index)
            localStorage.saveTracks(currentTracks)
        }
    }

    fun logReadingProgressForTrack(
        trackId: String,
        surah: String,
        ayah: Int,
        page: Int,
        pagesReadToday: Int,
        reflectionSummary: String
    ) {
        localStorage.addReadingLog(
            surah = surah,
            ayah = ayah,
            page = page,
            pagesReadToday = pagesReadToday,
            reflectionSummary = reflectionSummary,
            trackId = trackId
        )
    }

    fun updateTrackCheckpointSilently(
        trackId: String,
        surah: String,
        ayah: Int,
        page: Int
    ) {
        localStorage.updateTrackCheckpointSilently(
            trackId = trackId,
            surah = surah,
            ayah = ayah,
            page = page
        )
    }

    fun startNextKhatmaForTrack(trackId: String, currentKhatmaNum: Int) {
        val currentTracks = localStorage.tracksFlow.value.toMutableList()
        val index = currentTracks.indexOfFirst { it.trackId == trackId }
        if (index >= 0) {
            currentTracks[index] = currentTracks[index].copy(
                khatmaNumber = currentKhatmaNum + 1,
                currentSurah = "الفاتحة",
                currentAyah = 1,
                currentPage = 1,
                completedRubCount = 0,
                progressPercentage = 0.0,
                isCompleted = false
            )
            localStorage.saveTracks(currentTracks)
        }
    }

    fun observeReadingLogs(): Flow<List<ReadingLog>> {
        return localStorage.readingLogsFlow
    }
}

