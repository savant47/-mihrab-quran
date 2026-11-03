package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.OfflineQuranDatabase
import com.example.data.OfflineVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class QuranReciter(
    val id: String,
    val nameAr: String,
    val styleAr: String,
    val mp3QuranServerBaseUrl: String,
    val ayahStreamSubfolder: String
) {
    /**
     * Full Surah streaming URL directly from mp3quran.net servers (https://www.mp3quran.net/ar)
     */
    fun getMp3QuranSurahUrl(surahNumber: Int): String {
        val surah3 = String.format(Locale.US, "%03d", surahNumber.coerceIn(1, 114))
        return "$mp3QuranServerBaseUrl/$surah3.mp3"
    }

    /**
     * Exact Ayah online streaming URL (without downloading to storage)
     */
    fun getAyahStreamUrl(surahNumber: Int, ayahNumber: Int): String {
        val surah3 = String.format(Locale.US, "%03d", surahNumber.coerceIn(1, 114))
        val ayah3 = String.format(Locale.US, "%03d", ayahNumber.coerceAtLeast(1))
        return "https://everyayah.com/data/$ayahStreamSubfolder/$surah3$ayah3.mp3"
    }
}

data class QuranAudioPlaybackState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isFullSurahMode: Boolean = false,
    val currentSurahNumber: Int = 1,
    val currentSurahNameAr: String = "الفاتحة",
    val currentAyahNumber: Int = 1,
    val currentPage: Int = 1,
    val selectedReciter: QuranReciter = QuranAudioPlayerManager.reciters.first(),
    val targetRepeatCount: Int = 1, // 1, 2, 3, 5, 7, or -1 for infinite loop
    val currentRepeatIteration: Int = 1,
    val autoAdvanceNextAyah: Boolean = true,
    val errorMessage: String? = null
)

object QuranAudioPlayerManager {
    private const val TAG = "QuranAudioPlayerMgr"
    private const val PREFS_NAME = "mihrabi_quran_audio_prefs"
    private const val KEY_RECITER_ID = "selected_reciter_id"
    private const val KEY_REPEAT_COUNT = "ayah_repeat_count"
    private const val KEY_AUTO_ADVANCE = "auto_advance_next_ayah"
    const val MP3_QURAN_OFFICIAL_SITE = "https://www.mp3quran.net/ar"

    val reciters: List<QuranReciter> = listOf(
        QuranReciter(
            id = "alafasy",
            nameAr = "مشاري راشد العفاسي",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server8.mp3quran.net/afs",
            ayahStreamSubfolder = "Alafasy_128kbps"
        ),
        QuranReciter(
            id = "husary",
            nameAr = "محمود خليل الحصري",
            styleAr = "مرتل (المصحف المعلم) - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server13.mp3quran.net/husr",
            ayahStreamSubfolder = "Husary_128kbps"
        ),
        QuranReciter(
            id = "abdulbasit",
            nameAr = "عبد الباسط عبد الصمد",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server7.mp3quran.net/basit",
            ayahStreamSubfolder = "Abdul_Basit_Murattal_192kbps"
        ),
        QuranReciter(
            id = "minshawi",
            nameAr = "محمد صديق المنشاوي",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server10.mp3quran.net/minsh",
            ayahStreamSubfolder = "Minshawy_Murattal_128kbps"
        ),
        QuranReciter(
            id = "maher",
            nameAr = "ماهر المعيقلي",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server12.mp3quran.net/maher",
            ayahStreamSubfolder = "MaherAlMuaiqly128kbps"
        ),
        QuranReciter(
            id = "yasser",
            nameAr = "ياسر الدوسري",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server11.mp3quran.net/yasser",
            ayahStreamSubfolder = "Yasser_Ad-Dussary_128kbps"
        ),
        QuranReciter(
            id = "sudais",
            nameAr = "عبد الرحمن السديس",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server11.mp3quran.net/sds",
            ayahStreamSubfolder = "Abdurrahmaan_As-Sudais_192kbps"
        ),
        QuranReciter(
            id = "shuraym",
            nameAr = "سعود الشريم",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server7.mp3quran.net/shur",
            ayahStreamSubfolder = "Saood_ash-Shuraym_128kbps"
        ),
        QuranReciter(
            id = "ajamy",
            nameAr = "أحمد بن علي العجمي",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server10.mp3quran.net/ajm",
            ayahStreamSubfolder = "ahmed_ibn_ali_al_ajamy_128kbps"
        ),
        QuranReciter(
            id = "qatami",
            nameAr = "ناصر القطامي",
            styleAr = "مرتل - mp3quran.net",
            mp3QuranServerBaseUrl = "https://server6.mp3quran.net/qtm",
            ayahStreamSubfolder = "Nasser_Alqatami_128kbps"
        )
    )

    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(QuranAudioPlaybackState())
    val playbackState: StateFlow<QuranAudioPlaybackState> = _playbackState.asStateFlow()

    fun initPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reciterId = prefs.getString(KEY_RECITER_ID, reciters.first().id) ?: reciters.first().id
        val repeatCount = prefs.getInt(KEY_REPEAT_COUNT, 1)
        val autoAdvance = prefs.getBoolean(KEY_AUTO_ADVANCE, true)
        val reciter = reciters.find { it.id == reciterId } ?: reciters.first()

        _playbackState.value = _playbackState.value.copy(
            selectedReciter = reciter,
            targetRepeatCount = repeatCount,
            autoAdvanceNextAyah = autoAdvance
        )
    }

    fun selectReciter(context: Context, reciter: QuranReciter) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECITER_ID, reciter.id)
            .apply()
        val current = _playbackState.value
        _playbackState.value = current.copy(selectedReciter = reciter, errorMessage = null)

        // If currently playing or loading, restart with the newly chosen reciter
        if (current.isPlaying || current.isLoading) {
            if (current.isFullSurahMode) {
                playFullSurahFromMp3Quran(
                    context = context,
                    surahNumber = current.currentSurahNumber,
                    surahNameAr = current.currentSurahNameAr,
                    page = current.currentPage
                )
            } else {
                playAyahOnline(
                    context = context,
                    surahNumber = current.currentSurahNumber,
                    surahNameAr = current.currentSurahNameAr,
                    ayahNumber = current.currentAyahNumber,
                    page = current.currentPage
                )
            }
        }
    }

    fun setRepeatCount(context: Context, repeatCount: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REPEAT_COUNT, repeatCount)
            .apply()
        _playbackState.value = _playbackState.value.copy(
            targetRepeatCount = repeatCount,
            currentRepeatIteration = 1
        )
    }

    fun setAutoAdvanceNextAyah(context: Context, autoAdvance: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ADVANCE, autoAdvance)
            .apply()
        _playbackState.value = _playbackState.value.copy(autoAdvanceNextAyah = autoAdvance)
    }

    /**
     * Streams a specific Ayah directly online with support for repetition and auto-advancing.
     */
    fun playAyahOnline(
        context: Context,
        surahNumber: Int,
        surahNameAr: String,
        ayahNumber: Int,
        page: Int,
        repeatIteration: Int = 1
    ) {
        releaseMediaPlayer()
        val state = _playbackState.value
        val reciter = state.selectedReciter
        val streamUrl = reciter.getAyahStreamUrl(surahNumber, ayahNumber)

        _playbackState.value = state.copy(
            isPlaying = false,
            isLoading = true,
            isFullSurahMode = false,
            currentSurahNumber = surahNumber,
            currentSurahNameAr = surahNameAr,
            currentAyahNumber = ayahNumber,
            currentPage = page,
            currentRepeatIteration = repeatIteration,
            errorMessage = null
        )

        try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(streamUrl)
            mp.setOnPreparedListener { player ->
                player.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isLoading = false,
                    errorMessage = null
                )
            }
            mp.setOnCompletionListener {
                handleAyahPlaybackCompleted(context.applicationContext)
            }
            mp.setOnErrorListener { _, _, _ ->
                Log.w(TAG, "Ayah stream fallback to mp3quran.net full surah stream")
                playFullSurahFromMp3Quran(context.applicationContext, surahNumber, surahNameAr, page)
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming Ayah online", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isLoading = false,
                errorMessage = "تعذر الاتصال بمصدر الصوت الأونلاين. تأكد من اتصالك بالإنترنت."
            )
        }
    }

    /**
     * Streams the full Surah directly from mp3quran.net official server without downloading.
     */
    fun playFullSurahFromMp3Quran(
        context: Context,
        surahNumber: Int,
        surahNameAr: String,
        page: Int
    ) {
        releaseMediaPlayer()
        val state = _playbackState.value
        val reciter = state.selectedReciter
        val surahUrl = reciter.getMp3QuranSurahUrl(surahNumber)

        _playbackState.value = state.copy(
            isPlaying = false,
            isLoading = true,
            isFullSurahMode = true,
            currentSurahNumber = surahNumber,
            currentSurahNameAr = surahNameAr,
            currentPage = page,
            currentRepeatIteration = 1,
            errorMessage = null
        )

        try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(surahUrl)
            mp.setOnPreparedListener { player ->
                player.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isLoading = false,
                    errorMessage = null
                )
            }
            mp.setOnCompletionListener {
                val cur = _playbackState.value
                if (cur.targetRepeatCount == -1 || cur.currentRepeatIteration < cur.targetRepeatCount) {
                    val nextIter = if (cur.targetRepeatCount == -1) cur.currentRepeatIteration + 1 else cur.currentRepeatIteration + 1
                    playFullSurahFromMp3Quran(context, surahNumber, surahNameAr, page)
                    _playbackState.value = _playbackState.value.copy(currentRepeatIteration = nextIter)
                } else {
                    stopPlayback()
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = "تعذر تشغيل البث الصوتي من mp3quran.net. تحقق من اتصال الإنترنت."
                )
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming Surah from mp3quran.net", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isLoading = false,
                errorMessage = "تعذر تشغيل السورة من mp3quran.net"
            )
        }
    }

    private fun handleAyahPlaybackCompleted(context: Context) {
        val current = _playbackState.value
        val targetRepeats = current.targetRepeatCount

        // 1. Check if we should repeat the same Ayah again
        if (targetRepeats == -1 || current.currentRepeatIteration < targetRepeats) {
            playAyahOnline(
                context = context,
                surahNumber = current.currentSurahNumber,
                surahNameAr = current.currentSurahNameAr,
                ayahNumber = current.currentAyahNumber,
                page = current.currentPage,
                repeatIteration = current.currentRepeatIteration + 1
            )
            return
        }

        // 2. If repeat finished and autoAdvanceNextAyah is enabled, play next Ayah
        if (current.autoAdvanceNextAyah) {
            val nextVerse = findNextVerse(context, current.currentSurahNumber, current.currentAyahNumber)
            if (nextVerse != null) {
                playAyahOnline(
                    context = context,
                    surahNumber = nextVerse.surahNumber,
                    surahNameAr = nextVerse.surahNameAr,
                    ayahNumber = nextVerse.ayahNumber,
                    page = nextVerse.page,
                    repeatIteration = 1
                )
                return
            }
        }

        // Otherwise stop cleanly
        stopPlayback()
    }

    fun playNextAyah(context: Context) {
        val current = _playbackState.value
        val nextVerse = findNextVerse(context, current.currentSurahNumber, current.currentAyahNumber)
        if (nextVerse != null) {
            playAyahOnline(
                context = context,
                surahNumber = nextVerse.surahNumber,
                surahNameAr = nextVerse.surahNameAr,
                ayahNumber = nextVerse.ayahNumber,
                page = nextVerse.page,
                repeatIteration = 1
            )
        }
    }

    fun playPreviousAyah(context: Context) {
        val current = _playbackState.value
        val prevVerse = findPreviousVerse(context, current.currentSurahNumber, current.currentAyahNumber)
        if (prevVerse != null) {
            playAyahOnline(
                context = context,
                surahNumber = prevVerse.surahNumber,
                surahNameAr = prevVerse.surahNameAr,
                ayahNumber = prevVerse.ayahNumber,
                page = prevVerse.page,
                repeatIteration = 1
            )
        }
    }

    private fun findNextVerse(context: Context, surahNumber: Int, ayahNumber: Int): OfflineVerse? {
        val sameSurahNext = OfflineQuranDatabase.getVerse(context, surahNumber, ayahNumber + 1)
        if (sameSurahNext != null) return sameSurahNext
        if (surahNumber < 114) {
            return OfflineQuranDatabase.getVerse(context, surahNumber + 1, 1)
        }
        return null
    }

    private fun findPreviousVerse(context: Context, surahNumber: Int, ayahNumber: Int): OfflineVerse? {
        if (ayahNumber > 1) {
            return OfflineQuranDatabase.getVerse(context, surahNumber, ayahNumber - 1)
        }
        if (surahNumber > 1) {
            val prevSurahVerses = OfflineQuranDatabase.getVersesForSurah(context, surahNumber - 1)
            return prevSurahVerses.lastOrNull()
        }
        return null
    }

    fun togglePlayPause() {
        try {
            val player = mediaPlayer
            if (player != null) {
                if (player.isPlaying) {
                    player.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                } else {
                    player.start()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
        }
    }

    fun stopPlayback() {
        releaseMediaPlayer()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isLoading = false,
            currentRepeatIteration = 1
        )
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }
}
