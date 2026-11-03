package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.QuranDataHelper
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local device storage for Mihrabi.
 * Persists all Khatma tracks, reading logs, and profile info strictly to device storage.
 */
class LocalKhatmaStorage private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _guestUserFlow = MutableStateFlow(loadGuestUser())
    val guestUserFlow: StateFlow<User> = _guestUserFlow.asStateFlow()

    private val _tracksFlow = MutableStateFlow(loadTracks())
    val tracksFlow: StateFlow<List<KhatmaTrack>> = _tracksFlow.asStateFlow()

    private val _readingLogsFlow = MutableStateFlow(loadReadingLogs())
    val readingLogsFlow: StateFlow<List<ReadingLog>> = _readingLogsFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "quran_companion_local_storage"
        private const val KEY_GUEST_USER = "guest_user_json"
        private const val KEY_TRACKS = "tracks_json_array"
        private const val KEY_READING_LOGS = "reading_logs_json_array"

        @Volatile
        private var instance: LocalKhatmaStorage? = null

        fun getInstance(context: Context): LocalKhatmaStorage {
            return instance ?: synchronized(this) {
                instance ?: LocalKhatmaStorage(context).also { instance = it }
            }
        }
    }

    private fun loadGuestUser(): User {
        val json = prefs.getString(KEY_GUEST_USER, null)
        if (!json.isNullOrBlank()) {
            try {
                val obj = JSONObject(json)
                return User(
                    uid = obj.optString("uid", "local_quran_reader"),
                    email = obj.optString("email", ""),
                    displayName = obj.optString("displayName", "القارئ المبارك"),
                    role = "user",
                    currentKhatmaNumber = obj.optInt("currentKhatmaNumber", 1),
                    currentSurah = obj.optString("currentSurah", "الفاتحة"),
                    currentAyah = obj.optInt("currentAyah", 1),
                    currentPage = obj.optInt("currentPage", 1),
                    completedRubCount = obj.optInt("completedRubCount", 0),
                    progressPercentage = obj.optDouble("progressPercentage", 0.0)
                )
            } catch (e: Exception) {
                // fall through
            }
        }
        return User(
            uid = "local_quran_reader",
            email = "",
            displayName = "القارئ المبارك",
            role = "user",
            currentKhatmaNumber = 1,
            currentSurah = "الفاتحة",
            currentAyah = 1,
            currentPage = 1,
            completedRubCount = 0,
            progressPercentage = 0.0
        )
    }

    fun saveGuestUser(user: User) {
        try {
            val obj = JSONObject().apply {
                put("uid", user.uid)
                put("email", user.email)
                put("displayName", user.displayName)
                put("role", user.role)
                put("currentKhatmaNumber", user.currentKhatmaNumber)
                put("currentSurah", user.currentSurah)
                put("currentAyah", user.currentAyah)
                put("currentPage", user.currentPage)
                put("completedRubCount", user.completedRubCount)
                put("progressPercentage", user.progressPercentage)
            }
            prefs.edit().putString(KEY_GUEST_USER, obj.toString()).apply()
            _guestUserFlow.value = user
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun loadTracks(): List<KhatmaTrack> {
        val json = prefs.getString(KEY_TRACKS, null)
        if (!json.isNullOrBlank()) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<KhatmaTrack>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        KhatmaTrack(
                            trackId = obj.optString("trackId", "main"),
                            trackName = obj.optString("trackName", "الختمة الرئيسية"),
                            khatmaNumber = obj.optInt("khatmaNumber", 1),
                            currentSurah = obj.optString("currentSurah", "الفاتحة"),
                            currentAyah = obj.optInt("currentAyah", 1),
                            currentPage = obj.optInt("currentPage", 1),
                            completedRubCount = obj.optInt("completedRubCount", 0),
                            progressPercentage = obj.optDouble("progressPercentage", 0.0),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // fall through
            }
        }
        val defaultTrack = KhatmaTrack(
            trackId = "main",
            trackName = "الختمة الرئيسية",
            khatmaNumber = 1,
            currentSurah = "الفاتحة",
            currentAyah = 1,
            currentPage = 1,
            completedRubCount = 0,
            progressPercentage = 0.0,
            isCompleted = false
        )
        return listOf(defaultTrack)
    }

    fun saveTracks(tracks: List<KhatmaTrack>) {
        try {
            val arr = JSONArray()
            tracks.forEach { track ->
                val obj = JSONObject().apply {
                    put("trackId", track.trackId)
                    put("trackName", track.trackName)
                    put("khatmaNumber", track.khatmaNumber)
                    put("currentSurah", track.currentSurah)
                    put("currentAyah", track.currentAyah)
                    put("currentPage", track.currentPage)
                    put("completedRubCount", track.completedRubCount)
                    put("progressPercentage", track.progressPercentage)
                    put("isCompleted", track.isCompleted)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_TRACKS, arr.toString()).apply()
            _tracksFlow.value = tracks
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun loadReadingLogs(): List<ReadingLog> {
        val json = prefs.getString(KEY_READING_LOGS, null)
        if (!json.isNullOrBlank()) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<ReadingLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val millis = if (obj.has("timestampMillis")) {
                        obj.optLong("timestampMillis", System.currentTimeMillis())
                    } else {
                        obj.optLong("timestampSeconds", System.currentTimeMillis() / 1000) * 1000L
                    }
                    list.add(
                        ReadingLog(
                            logId = obj.optString("logId", "log_$i"),
                            timestampMillis = millis,
                            surah = obj.optString("surah", "الفاتحة"),
                            ayah = obj.optInt("ayah", 1),
                            pagesReadToday = obj.optInt("pagesReadToday", 1),
                            reflectionSummary = obj.optString("reflectionSummary", "")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    return list
                }
            } catch (e: Exception) {
                // fall through
            }
        }
        val initialLog = ReadingLog(
            logId = "initial_checkpoint",
            timestampMillis = System.currentTimeMillis(),
            surah = "الفاتحة",
            ayah = 1,
            pagesReadToday = 1,
            reflectionSummary = "نقطة التوقف الحالية في بداية الختمة المباركة لمحرابي. نسأل الله التوفيق والقبول."
        )
        return listOf(initialLog)
    }

    fun addReadingLog(
        surah: String,
        ayah: Int,
        page: Int,
        pagesReadToday: Int,
        reflectionSummary: String,
        trackId: String = "main"
    ) {
        val currentLogs = loadReadingLogs().toMutableList()
        val nowMillis = System.currentTimeMillis()
        val newLog = ReadingLog(
            logId = "log_$nowMillis",
            timestampMillis = nowMillis,
            surah = surah,
            ayah = ayah,
            pagesReadToday = pagesReadToday,
            reflectionSummary = reflectionSummary
        )
        currentLogs.add(0, newLog) // prepend newest first

        try {
            val arr = JSONArray()
            currentLogs.forEach { log ->
                val obj = JSONObject().apply {
                    put("logId", log.logId)
                    put("timestampMillis", log.timestampMillis)
                    put("timestampSeconds", log.timestampMillis / 1000L)
                    put("surah", log.surah)
                    put("ayah", log.ayah)
                    put("pagesReadToday", log.pagesReadToday)
                    put("reflectionSummary", log.reflectionSummary)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_READING_LOGS, arr.toString()).apply()
            _readingLogsFlow.value = currentLogs
        } catch (e: Exception) {
            // ignore
        }

        // Also update the active track in local storage
        val currentTracks = loadTracks().toMutableList()
        val activeIndex = currentTracks.indexOfFirst { it.trackId == trackId }
        val rubCount = QuranDataHelper.pageToRub(page)
        val progressPercentage = (page.toDouble() / QuranDataHelper.TOTAL_PAGES.toDouble()) * 100.0

        if (activeIndex >= 0) {
            val updated = currentTracks[activeIndex].copy(
                currentSurah = surah,
                currentAyah = ayah,
                currentPage = page,
                completedRubCount = rubCount,
                progressPercentage = progressPercentage,
                isCompleted = page >= QuranDataHelper.TOTAL_PAGES
            )
            currentTracks[activeIndex] = updated
        } else {
            currentTracks.add(
                KhatmaTrack(
                    trackId = trackId,
                    trackName = if (trackId == "main") "الختمة الرئيسية" else "ورد مخصص",
                    currentSurah = surah,
                    currentAyah = ayah,
                    currentPage = page,
                    completedRubCount = rubCount,
                    progressPercentage = progressPercentage
                )
            )
        }
        saveTracks(currentTracks)

        // Update user profile as well
        val currentUser = loadGuestUser()
        saveGuestUser(
            currentUser.copy(
                currentSurah = surah,
                currentAyah = ayah,
                currentPage = page,
                completedRubCount = rubCount,
                progressPercentage = progressPercentage
            )
        )
    }

    /**
     * Silently updates the stop-point (Surah, Ayah, Page) of a track when the user flips pages
     * or exits the Mushaf without pressing the manual Save button.
     */
    fun updateTrackCheckpointSilently(
        trackId: String,
        surah: String,
        ayah: Int,
        page: Int
    ) {
        val clampedPage = page.coerceIn(1, QuranDataHelper.TOTAL_PAGES)
        val rubCount = QuranDataHelper.pageToRub(clampedPage)
        val progressPercentage = (clampedPage.toDouble() / QuranDataHelper.TOTAL_PAGES.toDouble()) * 100.0

        val currentTracks = loadTracks().toMutableList()
        val activeIndex = currentTracks.indexOfFirst { it.trackId == trackId }
        if (activeIndex >= 0) {
            currentTracks[activeIndex] = currentTracks[activeIndex].copy(
                currentSurah = surah,
                currentAyah = ayah,
                currentPage = clampedPage,
                completedRubCount = rubCount,
                progressPercentage = progressPercentage,
                isCompleted = clampedPage >= QuranDataHelper.TOTAL_PAGES
            )
            saveTracks(currentTracks)
        }

        val currentUser = loadGuestUser()
        saveGuestUser(
            currentUser.copy(
                currentSurah = surah,
                currentAyah = ayah,
                currentPage = clampedPage,
                completedRubCount = rubCount,
                progressPercentage = progressPercentage
            )
        )
    }
}

