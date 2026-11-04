package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.alarm.MihrabiAlarmManager
import com.example.alarm.PrayerTimesAndAdhanManager
import com.example.audio.QuranAudioPlayerManager
import com.example.data.QuranDataHelper
import com.example.data.SpiritualMessageItem
import com.example.data.SpiritualStartupMessageHelper
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
 * Persists all Khatma tracks, reading logs, startup spiritual messages,
 * and supports full local Backup & Restore (.json / clipboard) of all progress and app settings.
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

    private val _spiritualMessagesFlow = MutableStateFlow(loadSpiritualMessages())
    val spiritualMessagesFlow: StateFlow<List<SpiritualMessageItem>> = _spiritualMessagesFlow.asStateFlow()

    private val _backupRestoreVersionFlow = MutableStateFlow(0)
    val backupRestoreVersionFlow: StateFlow<Int> = _backupRestoreVersionFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "quran_companion_local_storage"
        private const val KEY_GUEST_USER = "guest_user_json"
        private const val KEY_TRACKS = "tracks_json_array"
        private const val KEY_READING_LOGS = "reading_logs_json_array"
        private const val KEY_SPIRITUAL_MESSAGES = "spiritual_messages_json_array"
        private const val KEY_SPIRITUAL_ROTATION_INDEX = "spiritual_rotation_index"

        @Volatile
        private var instance: LocalKhatmaStorage? = null

        @Volatile
        private var hasShownStartupMessageInProcess: Boolean = false

        fun getInstance(context: Context): LocalKhatmaStorage {
            return instance ?: synchronized(this) {
                instance ?: LocalKhatmaStorage(context).also { instance = it }
            }
        }
    }

    /**
     * Triggered once per app launch process. Picks the next non-repeating Quranic Dua / Hadith,
     * saves it into the Spiritual Messages History so the user can always revisit it in "السجل",
     * and returns it to display in the ornate startup dialog.
     */
    fun consumeStartupMessageForSessionIfNeeded(): SpiritualMessageItem? {
        synchronized(Companion) {
            if (hasShownStartupMessageInProcess) return null
            hasShownStartupMessageInProcess = true
            return recordNextStartupMessage()
        }
    }

    fun recordNextStartupMessage(): SpiritualMessageItem {
        val templates = SpiritualStartupMessageHelper.allMessages
        val savedIdx = prefs.getInt(KEY_SPIRITUAL_ROTATION_INDEX, 0).coerceAtLeast(0)
        val template = templates[savedIdx % templates.size]
        val nextIdx = (savedIdx + 1) % templates.size

        val now = System.currentTimeMillis()
        val item = SpiritualMessageItem(
            id = "msg_$now",
            categoryTitle = template.categoryTitle,
            arabicText = template.arabicText,
            sourceReference = template.sourceReference,
            timestampMillis = now,
            isRead = false
        )

        val currentList = loadSpiritualMessages().toMutableList()
        currentList.add(0, item)
        // Keep up to 100 recent startup messages in history
        val trimmed = currentList.take(100)
        saveSpiritualMessages(trimmed)
        prefs.edit().putInt(KEY_SPIRITUAL_ROTATION_INDEX, nextIdx).apply()
        return item
    }

    fun markStartupMessageAsRead(messageId: String) {
        val updated = loadSpiritualMessages().map { msg ->
            if (msg.id == messageId) msg.copy(isRead = true) else msg
        }
        saveSpiritualMessages(updated)
    }

    private fun loadSpiritualMessages(): List<SpiritualMessageItem> {
        val json = prefs.getString(KEY_SPIRITUAL_MESSAGES, null)
        if (!json.isNullOrBlank()) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<SpiritualMessageItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SpiritualMessageItem(
                            id = obj.optString("id", "msg_$i"),
                            categoryTitle = obj.optString("categoryTitle", "دعاء قرآني من المصحف الشريف"),
                            arabicText = obj.optString("arabicText", ""),
                            sourceReference = obj.optString("sourceReference", ""),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                            isRead = obj.optBoolean("isRead", false)
                        )
                    )
                }
                return list
            } catch (_: Exception) {
                // fall through
            }
        }
        return emptyList()
    }

    private fun saveSpiritualMessages(messages: List<SpiritualMessageItem>) {
        try {
            val arr = JSONArray()
            messages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("categoryTitle", msg.categoryTitle)
                    put("arabicText", msg.arabicText)
                    put("sourceReference", msg.sourceReference)
                    put("timestampMillis", msg.timestampMillis)
                    put("isRead", msg.isRead)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_SPIRITUAL_MESSAGES, arr.toString()).apply()
            _spiritualMessagesFlow.value = messages
        } catch (_: Exception) {
            // ignore
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

    /**
     * Exports all user progress, Khatma tracks, reading logs, startup messages history,
     * Mushaf reader & Hifz bookmarks, Quran audio reciter & repetition settings,
     * Adhan & 5-prayer notification modes & custom ringtones, custom Werd/Adhkar alarms,
     * Electronic Tasbeeh counters, and Morning/Evening Adhkar progress into a single JSON string.
     */
    fun exportFullBackupJson(context: Context): String {
        val appCtx = context.applicationContext
        val root = JSONObject()
        root.put("app", "محرابي - Mihrabi")
        root.put("backupFormatVersion", 3)
        root.put("exportedAtMillis", System.currentTimeMillis())

        // 1. Core Khatma & History Data
        prefs.getString(KEY_GUEST_USER, null)?.let { root.put("guestUserRaw", JSONObject(it)) }
        prefs.getString(KEY_TRACKS, null)?.let { root.put("tracksRaw", JSONArray(it)) }
        prefs.getString(KEY_READING_LOGS, null)?.let { root.put("readingLogsRaw", JSONArray(it)) }
        prefs.getString(KEY_SPIRITUAL_MESSAGES, null)?.let { root.put("spiritualMessagesRaw", JSONArray(it)) }
        root.put("spiritualRotationIndex", prefs.getInt(KEY_SPIRITUAL_ROTATION_INDEX, 0))
        root.put("companionStoragePrefs", sharedPrefsToJson(prefs))

        // 2. Mushaf Reader Preferences (Page bookmarks per track, Hifz memorized Ayahs set, night mode, font size)
        val quranPrefs = appCtx.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE)
        root.put("quranReaderPrefs", sharedPrefsToJson(quranPrefs))

        // 3. Quran Audio Reciter & Repetition Settings (selected_reciter_id, ayah_repeat_count, auto_advance_next_ayah)
        val quranAudioPrefs = appCtx.getSharedPreferences("mihrabi_quran_audio_prefs", Context.MODE_PRIVATE)
        root.put("quranAudioPrefs", sharedPrefsToJson(quranAudioPrefs))

        // 4. Five Daily Prayers & Adhan Settings (location, calculation method, global alert mode, custom Adhan URI/name, per-prayer notification modes)
        val prayerAdhanPrefs = appCtx.getSharedPreferences("mihrabi_prayer_adhan_prefs", Context.MODE_PRIVATE)
        root.put("prayerAdhanPrefs", sharedPrefsToJson(prayerAdhanPrefs))
        // Keep v2 alias for backwards compatibility
        root.put("prayerSettingsPrefs", sharedPrefsToJson(prayerAdhanPrefs))

        // 5. Custom Werd, Adhkar & Tasbeeh Alarms + Custom Ringtone Settings
        val alarmsPrefs = appCtx.getSharedPreferences("mihrabi_alarms_prefs", Context.MODE_PRIVATE)
        root.put("alarmsPrefs", sharedPrefsToJson(alarmsPrefs))
        // Keep v2 alias for backwards compatibility
        root.put("alarmPrefs", sharedPrefsToJson(alarmsPrefs))

        // 6. Electronic Tasbeeh Counters
        val tasbeehPrefs = appCtx.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE)
        root.put("tasbeehPrefs", sharedPrefsToJson(tasbeehPrefs))

        // 7. Morning & Evening Adhkar Progress Counters
        val adhkarPrefs = appCtx.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE)
        root.put("morningEveningAdhkarPrefs", sharedPrefsToJson(adhkarPrefs))

        return root.toString(2)
    }

    /**
     * Imports a previously exported JSON backup, restores all SharedPreferences stores,
     * reschedules all 5 Prayer Adhan notifications and custom alarms, re-initializes Quran audio
     * reciter & repetition settings, and immediately updates all active StateFlows.
     */
    fun importFullBackupJson(context: Context, jsonString: String): Result<String> {
        return try {
            val appCtx = context.applicationContext
            val cleaned = jsonString.trim()
            if (cleaned.isEmpty()) {
                return Result.failure(IllegalArgumentException("ملف النسخة الاحتياطية فارغ."))
            }
            val root = JSONObject(cleaned)

            // 1. Restore companionStoragePrefs if present, plus explicit raw keys
            if (root.has("companionStoragePrefs")) {
                jsonToSharedPrefs(root.getJSONObject("companionStoragePrefs"), prefs)
            }

            val editor = prefs.edit()
            var restoredTracksCount = 0
            var restoredLogsCount = 0

            if (root.has("guestUserRaw")) {
                editor.putString(KEY_GUEST_USER, root.getJSONObject("guestUserRaw").toString())
            }
            if (root.has("tracksRaw")) {
                val arr = root.getJSONArray("tracksRaw")
                restoredTracksCount = arr.length()
                editor.putString(KEY_TRACKS, arr.toString())
            }
            if (root.has("readingLogsRaw")) {
                val arr = root.getJSONArray("readingLogsRaw")
                restoredLogsCount = arr.length()
                editor.putString(KEY_READING_LOGS, arr.toString())
            }
            if (root.has("spiritualMessagesRaw")) {
                val arr = root.getJSONArray("spiritualMessagesRaw")
                editor.putString(KEY_SPIRITUAL_MESSAGES, arr.toString())
            }
            if (root.has("spiritualRotationIndex")) {
                editor.putInt(KEY_SPIRITUAL_ROTATION_INDEX, root.optInt("spiritualRotationIndex", 0))
            }
            editor.commit()

            // 2. Restore Mushaf Reader Preferences (bookmarks, Hifz marks, night mode, font scale)
            if (root.has("quranReaderPrefs")) {
                val quranPrefs = appCtx.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(root.getJSONObject("quranReaderPrefs"), quranPrefs)
            }

            // 3. Restore Quran Audio Reciter & Repetition Preferences
            if (root.has("quranAudioPrefs")) {
                val quranAudioPrefs = appCtx.getSharedPreferences("mihrabi_quran_audio_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(root.getJSONObject("quranAudioPrefs"), quranAudioPrefs)
            }
            QuranAudioPlayerManager.initPreferences(appCtx)

            // 4. Restore Prayer Times & Adhan Settings (location, calculation method, per-prayer notifications, custom Adhan tone)
            val prayerAdhanJson = when {
                root.has("prayerAdhanPrefs") -> root.getJSONObject("prayerAdhanPrefs")
                root.has("prayerSettingsPrefs") -> root.getJSONObject("prayerSettingsPrefs")
                else -> null
            }
            if (prayerAdhanJson != null) {
                val prayerAdhanPrefs = appCtx.getSharedPreferences("mihrabi_prayer_adhan_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(prayerAdhanJson, prayerAdhanPrefs)
                // Also mirror to legacy name if anything reads it
                val legacyPrayerPrefs = appCtx.getSharedPreferences("mihrabi_five_prayers_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(prayerAdhanJson, legacyPrayerPrefs)
            }
            PrayerTimesAndAdhanManager.scheduleAllFivePrayers(appCtx)

            // 5. Restore Custom Werd & Adhkar Alarms and Ringtones
            val alarmsJson = when {
                root.has("alarmsPrefs") -> root.getJSONObject("alarmsPrefs")
                root.has("alarmPrefs") -> root.getJSONObject("alarmPrefs")
                else -> null
            }
            if (alarmsJson != null) {
                val alarmsPrefs = appCtx.getSharedPreferences("mihrabi_alarms_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(alarmsJson, alarmsPrefs)
                val legacyAlarmPrefs = appCtx.getSharedPreferences("mihrabi_alarm_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(alarmsJson, legacyAlarmPrefs)
            }
            MihrabiAlarmManager.rescheduleAllEnabledAlarms(appCtx)

            // 6. Restore Electronic Tasbeeh Counters
            if (root.has("tasbeehPrefs")) {
                val tasbeehPrefs = appCtx.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(root.getJSONObject("tasbeehPrefs"), tasbeehPrefs)
            }

            // 7. Restore Morning & Evening Adhkar Progress
            if (root.has("morningEveningAdhkarPrefs")) {
                val adhkarPrefs = appCtx.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE)
                jsonToSharedPrefs(root.getJSONObject("morningEveningAdhkarPrefs"), adhkarPrefs)
            }

            // Refresh all StateFlows immediately so UI reflects restored progress & settings right away
            _guestUserFlow.value = loadGuestUser()
            val loadedTracks = loadTracks()
            _tracksFlow.value = loadedTracks
            val loadedLogs = loadReadingLogs()
            _readingLogsFlow.value = loadedLogs
            _spiritualMessagesFlow.value = loadSpiritualMessages()
            _backupRestoreVersionFlow.value += 1

            val mainTrack = loadedTracks.firstOrNull()
            val restoredAdhan = PrayerTimesAndAdhanManager.loadSettings(appCtx)
            val restoredAudio = QuranAudioPlayerManager.playbackState.value
            val repeatLabel = if (restoredAudio.targetRepeatCount == -1) "تكرار مستمر" else "${restoredAudio.targetRepeatCount}x"

            val summary = if (mainTrack != null) {
                "تمت استعادة النسخة الاحتياطية الكاملة بنجاح:\n" +
                    "• الختمة: سورة ${mainTrack.currentSurah} (صفحة ${mainTrack.currentPage}) • ${loadedTracks.size} ورد و ${loadedLogs.size} سجل\n" +
                    "• المصحف والتلاوة: القارئ ${restoredAudio.selectedReciter.nameAr} (التكرار: $repeatLabel)\n" +
                    "• الأذان والصلاة: ${restoredAdhan.locationName} (${restoredAdhan.globalAlertMode.titleAr}) وإعدادات الصلوات الخمس والمنبهات"
            } else {
                "تمت استعادة النسخة الاحتياطية الكاملة بنجاح ($restoredTracksCount ورد، $restoredLogsCount سجل، وإعدادات الأذان والقارئ والمنبهات)."
            }
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception("تعذر قراءة بيانات النسخة الاحتياطية. تأكد من اختيار ملف أو كود نسخ احتياطي صحيح لتطبيق محرابي."))
        }
    }

    private fun sharedPrefsToJson(sp: SharedPreferences): JSONObject {
        val obj = JSONObject()
        for ((key, value) in sp.all) {
            if (value == null) continue
            val entry = JSONObject()
            when (value) {
                is Boolean -> {
                    entry.put("type", "bool")
                    entry.put("val", value)
                }
                is Int -> {
                    entry.put("type", "int")
                    entry.put("val", value)
                }
                is Long -> {
                    entry.put("type", "long")
                    entry.put("val", value)
                }
                is Float -> {
                    entry.put("type", "float")
                    entry.put("val", value.toDouble())
                }
                is String -> {
                    entry.put("type", "string")
                    entry.put("val", value)
                }
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { item ->
                        if (item is String) arr.put(item)
                    }
                    entry.put("type", "string_set")
                    entry.put("val", arr)
                }
            }
            if (entry.has("type")) {
                obj.put(key, entry)
            }
        }
        return obj
    }

    private fun jsonToSharedPrefs(obj: JSONObject, sp: SharedPreferences) {
        val editor = sp.edit()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = obj.optJSONObject(key) ?: continue
            when (entry.optString("type")) {
                "bool" -> editor.putBoolean(key, entry.optBoolean("val"))
                "int" -> editor.putInt(key, entry.optInt("val"))
                "long" -> editor.putLong(key, entry.optLong("val"))
                "float" -> editor.putFloat(key, entry.optDouble("val").toFloat())
                "string" -> editor.putString(key, entry.optString("val"))
                "string_set" -> {
                    val arr = entry.optJSONArray("val")
                    val set = mutableSetOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            set.add(arr.optString(i))
                        }
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.commit()
    }
}

