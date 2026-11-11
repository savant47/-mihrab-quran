package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.audio.QuranAudioPlayerManager
import com.example.data.local.AdhkarCustomizationManager
import com.example.data.local.LocalKhatmaStorage
import com.example.data.local.UnifiedDhikrItem
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.RoyalNavyDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdhkarAudioState(
    val activeTrack: String? = null, // "morning" or "evening"
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

/**
 * Offline Audio Player Manager for Morning & Evening Short Adhkar (أذكار الصباح والمساء المسموعة بهدوء)
 */
object AdhkarAudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(AdhkarAudioState())
    val state: StateFlow<AdhkarAudioState> = _state.asStateFlow()

    fun togglePlay(context: Context, trackType: String) {
        val current = _state.value
        if (current.activeTrack == trackType && mediaPlayer != null) {
            val mp = mediaPlayer!!
            if (mp.isPlaying) {
                mp.pause()
                _state.value = current.copy(isPlaying = false, currentPositionMs = mp.currentPosition)
            } else {
                QuranAudioPlayerManager.stopPlayback()
                mp.start()
                _state.value = current.copy(isPlaying = true, durationMs = mp.duration.coerceAtLeast(1))
            }
            return
        }

        stop()
        QuranAudioPlayerManager.stopPlayback()

        val rawResId = if (trackType == "morning") {
            R.raw.adhkar_morning_short
        } else {
            R.raw.adhkar_evening_short
        }

        try {
            val mp = MediaPlayer.create(context.applicationContext, rawResId)
            if (mp != null) {
                mp.isLooping = _state.value.isLooping
                mp.setOnCompletionListener { player ->
                    if (!player.isLooping) {
                        _state.value = _state.value.copy(
                            isPlaying = false,
                            currentPositionMs = 0
                        )
                    }
                }
                mp.start()
                mediaPlayer = mp
                _state.value = AdhkarAudioState(
                    activeTrack = trackType,
                    isPlaying = true,
                    isLooping = mp.isLooping,
                    currentPositionMs = 0,
                    durationMs = mp.duration.coerceAtLeast(1)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleLoop() {
        val nextLoop = !_state.value.isLooping
        try {
            mediaPlayer?.isLooping = nextLoop
        } catch (_: Exception) {}
        _state.value = _state.value.copy(isLooping = nextLoop)
    }

    fun seekTo(positionMs: Int) {
        try {
            val mp = mediaPlayer ?: return
            val target = positionMs.coerceIn(0, mp.duration.coerceAtLeast(1))
            mp.seekTo(target)
            _state.value = _state.value.copy(currentPositionMs = target)
        } catch (_: Exception) {}
    }

    fun seekBy(deltaMs: Int) {
        try {
            val mp = mediaPlayer ?: return
            val target = (mp.currentPosition + deltaMs).coerceIn(0, mp.duration.coerceAtLeast(1))
            mp.seekTo(target)
            _state.value = _state.value.copy(currentPositionMs = target)
        } catch (_: Exception) {}
    }

    fun refreshProgress() {
        try {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                _state.value = _state.value.copy(
                    isPlaying = true,
                    currentPositionMs = mp.currentPosition,
                    durationMs = mp.duration.coerceAtLeast(1)
                )
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _state.value = AdhkarAudioState(isLooping = _state.value.isLooping)
    }
}

private fun formatAudioTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun AdhkarActiveAudioPlayerBar(
    audioState: AdhkarAudioState,
    context: Context,
    modifier: Modifier = Modifier
) {
    val isMorningTrack = audioState.activeTrack == "morning"
    val trackTitle = if (isMorningTrack) {
        "🎧 استماع أذكار الصباح مختصرة"
    } else {
        "🎧 استماع أذكار المساء مختصرة"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF3E9D2),
        border = BorderStroke(1.2.dp, GoldAccent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = trackTitle,
                    fontFamily = AmiriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (audioState.isLooping) EmeraldGreen else Color(0xFFEBE1C9),
                    border = BorderStroke(1.dp, if (audioState.isLooping) EmeraldGreen else GoldBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { AdhkarAudioPlayerManager.toggleLoop() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "تكرار مستمر",
                            tint = if (audioState.isLooping) Color.White else GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (audioState.isLooping) "تكرار مفعل" else "تكرار",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (audioState.isLooping) Color.White else GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = audioState.currentPositionMs.toFloat(),
                onValueChange = { AdhkarAudioPlayerManager.seekTo(it.toInt()) },
                valueRange = 0f..audioState.durationMs.coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = EmeraldGreen,
                    inactiveTrackColor = GoldBorder.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAudioTime(audioState.currentPositionMs),
                    fontSize = 11.sp,
                    color = Color(0xFF1A110B).copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { AdhkarAudioPlayerManager.seekBy(-10_000) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "رجوع 10 ثواني",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            audioState.activeTrack?.let {
                                AdhkarAudioPlayerManager.togglePlay(context, it)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (audioState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { AdhkarAudioPlayerManager.seekBy(10_000) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "تقديم 10 ثواني",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { AdhkarAudioPlayerManager.stop() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "إيقاف",
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = formatAudioTime(audioState.durationMs),
                    fontSize = 11.sp,
                    color = Color(0xFF1A110B).copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private const val KEY_LAST_DAILY_ADHKAR_RESET_DATE = "last_daily_reset_date_yyyy_mm_dd"

/**
 * Automatically resets ALL Adhkar daily progress (Morning, Evening, all 132 Hisn Al-Muslim chapters,
 * Favorite Adhkar, custom Adhkar, and daily Electronic Tasbeeh session counters) to 0 whenever
 * the calendar date changes at 12:00 AM midnight, while preserving the permanent cumulative total in History.
 */
fun ensureDailyAdhkarResetIfNewDay(
    prefs: android.content.SharedPreferences,
    context: Context? = null
): Boolean {
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val lastResetDate = prefs.getString(KEY_LAST_DAILY_ADHKAR_RESET_DATE, null)
    if (lastResetDate != todayKey) {
        val editor = prefs.edit()
        if (lastResetDate != null) {
            AuthenticAdhkarData.morningAdhkar.forEach { editor.putInt(it.id, 0) }
            AuthenticAdhkarData.eveningAdhkar.forEach { editor.putInt(it.id, 0) }
            prefs.all.forEach { (key, value) ->
                if (key != KEY_LAST_DAILY_ADHKAR_RESET_DATE && value is Int) {
                    editor.putInt(key, 0)
                }
            }
            if (context != null) {
                val tasbeehPrefs = context.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE)
                val tEdit = tasbeehPrefs.edit()
                tasbeehPrefs.all.forEach { (k, v) ->
                    if (v is Int) {
                        tEdit.putInt(k, 0)
                    }
                }
                tEdit.apply()
            }
        }
        editor.putString(KEY_LAST_DAILY_ADHKAR_RESET_DATE, todayKey)
        editor.apply()
        return lastResetDate != null
    }
    return false
}

fun resetAllAdhkarCountersManually(
    prefs: android.content.SharedPreferences,
    context: Context? = null
) {
    val editor = prefs.edit()
    AuthenticAdhkarData.morningAdhkar.forEach { editor.putInt(it.id, 0) }
    AuthenticAdhkarData.eveningAdhkar.forEach { editor.putInt(it.id, 0) }
    prefs.all.forEach { (key, value) ->
        if (key != KEY_LAST_DAILY_ADHKAR_RESET_DATE && value is Int) {
            editor.putInt(key, 0)
        }
    }
    editor.apply()
    if (context != null) {
        val tasbeehPrefs = context.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE)
        val tEdit = tasbeehPrefs.edit()
        tasbeehPrefs.all.forEach { (k, v) ->
            if (v is Int) {
                tEdit.putInt(k, 0)
            }
        }
        tEdit.apply()
    }
}

data class HisnDhikrItem(
    val id: String,
    val title: String,
    val arabicText: String,
    val benefit: String,
    val targetCount: Int,
    val isCustomAdded: Boolean = false
)

object AuthenticAdhkarData {

    fun getEffectiveItems(context: Context, trackType: String): List<HisnDhikrItem> {
        val baseList = if (trackType == "morning") morningAdhkar else eveningAdhkar
        val overrides = AdhkarCustomizationManager.getOverridesMap(context)
        val deletedKeys = AdhkarCustomizationManager.getDeletedKeys(context)
        val customAdded = AdhkarCustomizationManager.getCustomAddedAdhkar(context, scopeFilter = trackType)

        val customMapped = customAdded
            .filter { !deletedKeys.contains(it.uniqueKey) }
            .map { c ->
                val ov = overrides[c.uniqueKey]
                HisnDhikrItem(
                    id = c.uniqueKey,
                    title = ov?.title?.ifBlank { c.title } ?: c.title,
                    arabicText = ov?.arabicText?.ifBlank { c.arabicText } ?: c.arabicText,
                    benefit = ov?.benefit ?: c.benefit,
                    targetCount = ov?.targetCount ?: c.targetCount,
                    isCustomAdded = true
                )
            }

        val builtInMapped = baseList
            .filter { !deletedKeys.contains(it.id) }
            .map { item ->
                val ov = overrides[item.id]
                if (ov != null) {
                    item.copy(
                        title = ov.title.ifBlank { item.title },
                        arabicText = ov.arabicText.ifBlank { item.arabicText },
                        benefit = ov.benefit.ifBlank { item.benefit },
                        targetCount = ov.targetCount.coerceAtLeast(1)
                    )
                } else {
                    item
                }
            }

        return customMapped + builtInMapped
    }

    val morningAdhkar: List<HisnDhikrItem> = listOf(
        HisnDhikrItem(
            id = "m_ayat_kursi",
            title = "آية الكرسي",
            arabicText = "﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ﴾",
            benefit = "من قالها حين يصبح أجير من الجن حتى يمسي.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_ikhlas",
            title = "سورة الإخلاص",
            arabicText = "﴿قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ﴾",
            benefit = "من قالها مع المعوذتين حين يصبح وحين يمسي ثلاث مرات كفته من كل شيء.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_falaq",
            title = "سورة الفلق",
            arabicText = "﴿قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ﴾",
            benefit = "تُقرأ ثلاث مرات لحفظ المسلم وكفايته من كل شر.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_nas",
            title = "سورة الناس",
            arabicText = "﴿قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ﴾",
            benefit = "تُقرأ ثلاث مرات للتحصين الشامل من الوساوس والشرور.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_asbahna",
            title = "دعاء الصباح الجامع",
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَٰذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَٰذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
            benefit = "إعلان التوحيد وسؤال الله خير اليوم والاستعاذة من شره.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_sayyid_istighfar",
            title = "سيد الاستغفار",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            benefit = "من قالها موقناً بها حين يصبح فمات من يومه دخل الجنة.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_bika_asbahna",
            title = "اللهم بك أصبحنا",
            arabicText = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
            benefit = "من هدي النبي ﷺ في تفويض الأمر لله كل صباح.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_fitrah",
            title = "أصبحنا على فطرة الإسلام",
            arabicText = "أَصْبَحْنَا عَلَىٰ فِطْرَةِ الْإِسْلَامِ، وَعَلَىٰ كَلِمَةِ الْإِخْلَاصِ، وَعَلَىٰ دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَىٰ مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.",
            benefit = "تجديد العهد مع الله على الفطرة والتوحيد كل صباح.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_radheetu",
            title = "الرضا بالله رباً",
            arabicText = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.",
            benefit = "من قالها ثلاثاً حين يصبح وحين يمسي كان حقاً على الله أن يرضيه يوم القيامة.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_hasbiya",
            title = "حسبي الله لا إله إلا هو",
            arabicText = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ ۖ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
            benefit = "من قالها سبع مرات كفاه الله ما أهمه من أمر الدنيا والآخرة.",
            targetCount = 7
        ),
        HisnDhikrItem(
            id = "m_bismillah",
            title = "بسم الله الذي لا يضر مع اسمه شيء",
            arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            benefit = "من قالها ثلاث مرات لم تصبه فجأة بلاء ولم يضره شيء.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_afiyah",
            title = "دعاء العفو والعافية والحفظ",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.",
            benefit = "حفظ شامل للمسلم في دينه ودنياه وأهله وماله من جميع الجهات.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_ya_hayyu",
            title = "يا حي يا قيوم برحمتك أستغيث",
            arabicText = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَىٰ نَفْسِي طَرْفَةَ عَيْنٍ.",
            benefit = "وصية النبي ﷺ لفاطمة رضي الله عنها في الصباح والمساء.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_subhan_adada",
            title = "سبحان الله وبحمده عدد خلقه",
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            benefit = "أربع كلمات تعدل أضعافاً مضاعفة من الذكر والوزن في الميزان.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "m_ilm_nafi",
            title = "سؤال العلم النافع والرزق الطيب",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.",
            benefit = "يقال كل صباح لبركة العلم والرزق والعمل.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "m_salawat",
            title = "الصلاة والسلام على النبي ﷺ",
            arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَىٰ نَبِيِّنَا مُحَمَّدٍ.",
            benefit = "من صلى على النبي ﷺ حين يصبح وحين يمسي عشراً أدركته شفاعته يوم القيامة.",
            targetCount = 10
        )
    )

    val eveningAdhkar: List<HisnDhikrItem> = listOf(
        HisnDhikrItem(
            id = "e_ayat_kursi",
            title = "آية الكرسي",
            arabicText = "﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ﴾",
            benefit = "من قالها حين يمسي أجير من الجن حتى يصبح.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_ikhlas",
            title = "سورة الإخلاص",
            arabicText = "﴿قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ﴾",
            benefit = "من قالها مع المعوذتين حين يمسي ثلاث مرات كفته من كل شيء.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_falaq",
            title = "سورة الفلق",
            arabicText = "﴿قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ﴾",
            benefit = "تُقرأ ثلاث مرات في المساء للحفظ من شر غاسق إذا وقب ومن كل سوء.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_nas",
            title = "سورة الناس",
            arabicText = "﴿قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ﴾",
            benefit = "تُقرأ ثلاث مرات للتحصين في الليل من شياطين الإنس والجن.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_amsayna",
            title = "دعاء المساء الجامع",
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَٰذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَٰذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.",
            benefit = "سؤال الله خير هذه الليلة وما بعدها والاستعاذة من شرها.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_sayyid_istighfar",
            title = "سيد الاستغفار",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            benefit = "من قالها موقناً بها حين يمسي فمات من ليلته دخل الجنة.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_bika_amsayna",
            title = "اللهم بك أمسينا",
            arabicText = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.",
            benefit = "تفويض الأمر لله سبحانه عند حلول المساء.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_fitrah",
            title = "أمسينا على فطرة الإسلام",
            arabicText = "أَمْسَيْنَا عَلَىٰ فِطْرَةِ الْإِسْلَامِ، وَعَلَىٰ كَلِمَةِ الْإِخْلَاصِ، وَعَلَىٰ دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَىٰ مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.",
            benefit = "ختام اليوم بتجديد التوحيد والإخلاص لله عز وجل.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_kalimat_allah",
            title = "أعوذ بكلمات الله التامات",
            arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
            benefit = "من قالها حين يمسي ثلاث مرات لم تضره حُمَة تلك الليلة.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_radheetu",
            title = "الرضا بالله رباً",
            arabicText = "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.",
            benefit = "من قالها ثلاثاً حين يمسي كان حقاً على الله أن يرضيه يوم القيامة.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_hasbiya",
            title = "حسبي الله لا إله إلا هو",
            arabicText = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ ۖ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
            benefit = "من قالها سبع مرات كفاه الله ما أهمه من أمر الدنيا والآخرة.",
            targetCount = 7
        ),
        HisnDhikrItem(
            id = "e_bismillah",
            title = "بسم الله الذي لا يضر مع اسمه شيء",
            arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            benefit = "من قالها ثلاث مرات حين يمسي لم تصبه فجأة بلاء حتى يصبح.",
            targetCount = 3
        ),
        HisnDhikrItem(
            id = "e_afiyah",
            title = "دعاء العفو والعافية والحفظ",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.",
            benefit = "حفظ شامل للمسلم في ليلته وأهله وماله.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_ya_hayyu",
            title = "يا حي يا قيوم برحمتك أستغيث",
            arabicText = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَىٰ نَفْسِي طَرْفَةَ عَيْنٍ.",
            benefit = "الاستغاثة برحمة الله لإصلاح الشأن كله.",
            targetCount = 1
        ),
        HisnDhikrItem(
            id = "e_salawat",
            title = "الصلاة والسلام على النبي ﷺ",
            arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَىٰ نَبِيِّنَا مُحَمَّدٍ.",
            benefit = "من صلى على النبي ﷺ حين يمسي عشراً أدركته شفاعته يوم القيامة.",
            targetCount = 10
        )
    )
}

/**
 * Box containing the two requested buttons:
 * 1. أذكار الصباح (Morning Adhkar)
 * 2. أذكار المساء (Evening Adhkar)
 * Persists daily completion counts in device local cache.
 */
@Composable
fun MorningEveningAdhkarBox() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }
    val backupVersion by remember {
        LocalKhatmaStorage.getInstance(context).backupRestoreVersionFlow
    }.collectAsState()
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()

    val audioState by AdhkarAudioPlayerManager.state.collectAsState()

    LaunchedEffect(audioState.isPlaying) {
        while (audioState.isPlaying) {
            AdhkarAudioPlayerManager.refreshProgress()
            delay(400L)
        }
    }

    var activeDialogType by remember { mutableStateOf<String?>(null) } // "morning" or "evening"
    var showHisnCompleteDialog by remember { mutableStateOf(false) }
    var initialHisnGroupKey by remember { mutableStateOf(HisnSectionGroup.ALL.key) }
    var initialHisnChapterId by remember { mutableStateOf<Int?>(null) }
    var showFavoriteAdhkarDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Automatic Daily Reset at 12:00 AM midnight (checks on launch and every 20 seconds while open)
    LaunchedEffect(Unit) {
        if (ensureDailyAdhkarResetIfNewDay(prefs, context)) {
            refreshTrigger++
        }
        while (true) {
            delay(20_000L)
            if (ensureDailyAdhkarResetIfNewDay(prefs, context)) {
                refreshTrigger++
            }
        }
    }

    val effectiveMorningItems = remember(refreshTrigger, activeDialogType, backupVersion, customizationVersion) {
        AuthenticAdhkarData.getEffectiveItems(context, "morning")
    }
    val effectiveEveningItems = remember(refreshTrigger, activeDialogType, backupVersion, customizationVersion) {
        AuthenticAdhkarData.getEffectiveItems(context, "evening")
    }
    val totalFavoritesCount = remember(refreshTrigger, backupVersion, customizationVersion) {
        AdhkarCustomizationManager.getTotalFavoritesCount(context)
    }

    val morningCompleted = remember(refreshTrigger, activeDialogType, backupVersion, effectiveMorningItems) {
        ensureDailyAdhkarResetIfNewDay(prefs, context)
        effectiveMorningItems.count { item ->
            prefs.getInt(item.id, 0) >= item.targetCount
        }
    }
    val eveningCompleted = remember(refreshTrigger, activeDialogType, backupVersion, effectiveEveningItems) {
        ensureDailyAdhkarResetIfNewDay(prefs, context)
        effectiveEveningItems.count { item ->
            prefs.getInt(item.id, 0) >= item.targetCount
        }
    }

    val isMorningAudioPlaying = audioState.activeTrack == "morning" && audioState.isPlaying
    val isEveningAudioPlaying = audioState.activeTrack == "evening" && audioState.isPlaying

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
            .testTag("morning_evening_adhkar_box"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RoyalNavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أذكار الصباح والمساء وحصن المسلم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "تصفير تلقائي يومياً 12:00 ص • تصفير يدوي لكل قسم وباب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Manual Reset Button for All Adhkar on the main box
                OutlinedButton(
                    onClick = {
                        resetAllAdhkarCountersManually(prefs, context)
                        refreshTrigger++
                        android.widget.Toast.makeText(
                            context,
                            "تم تصفير وإعادة ضبط عدادات كافة الأذكار بنجاح",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("reset_all_adhkar_main_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تصفير كافة الأذكار",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "تصفير الكل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1: أذكار الصباح + استماع الصباح + منبه الصباح
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { activeDialogType = "morning" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 62.dp)
                            .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
                            .testTag("morning_adhkar_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalNavyDark,
                            contentColor = Color(0xFF1A110B)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "أذكار الصباح",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = EmeraldGreen,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إنجاز $morningCompleted / ${effectiveMorningItems.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A110B).copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Play Morning Adhkar Audio Button
                    Button(
                        onClick = { AdhkarAudioPlayerManager.togglePlay(context, "morning") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .testTag("play_morning_adhkar_audio_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMorningAudioPlaying) GoldAccent else Color(0xFFF3E9D2),
                            contentColor = if (isMorningAudioPlaying) Color.White else EmeraldGreen
                        ),
                        border = BorderStroke(1.dp, GoldAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isMorningAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "استماع أذكار الصباح",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMorningAudioPlaying) "إيقاف الصباح" else "استماع الصباح",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    AlarmTriggerChip(
                        targetId = "adhkar_morning",
                        defaultTitle = "منبه أذكار الصباح",
                        defaultSubtitle = "حان الآن موعد قراءة أذكار الصباح المباركة",
                        defaultHour = 6,
                        defaultMinute = 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Column 2: أذكار المساء + استماع المساء + منبه المساء
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { activeDialogType = "evening" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 62.dp)
                            .border(1.dp, EmeraldGreen, RoundedCornerShape(14.dp))
                            .testTag("evening_adhkar_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "أذكار المساء",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إنجاز $eveningCompleted / ${effectiveEveningItems.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    // Play Evening Adhkar Audio Button
                    Button(
                        onClick = { AdhkarAudioPlayerManager.togglePlay(context, "evening") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .testTag("play_evening_adhkar_audio_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEveningAudioPlaying) GoldAccent else EmeraldGreen.copy(alpha = 0.9f),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (isEveningAudioPlaying) GoldAccent else EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isEveningAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "استماع أذكار المساء",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEveningAudioPlaying) "إيقاف المساء" else "استماع المساء",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    AlarmTriggerChip(
                        targetId = "adhkar_evening",
                        defaultTitle = "منبه أذكار المساء",
                        defaultSubtitle = "حان الآن موعد قراءة أذكار المساء المباركة",
                        defaultHour = 17,
                        defaultMinute = 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Active Car-Friendly Audio Player Bar when Morning or Evening Adhkar Audio is active
            AnimatedVisibility(visible = audioState.activeTrack != null) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    AdhkarActiveAudioPlayerBar(
                        audioState = audioState,
                        context = context
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            androidx.compose.material3.HorizontalDivider(color = GoldBorder.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(12.dp))

            // Complete Offline Hisn Al-Muslim Section (132 Chapters + Instant Search + External Favorites Button)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3E9D2),
                border = BorderStroke(1.2.dp, GoldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📖 حصن المسلم الكامل (١٣٢ باباً)",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "جميع الأبواب والأدعية أوفلاين مع البحث والمفضلة",
                                fontSize = 11.sp,
                                color = Color(0xFF1A110B).copy(alpha = 0.78f)
                            )
                        }

                        // Heart Icon Badge on the top-left of Hisn Al-Muslim Card (marked in Screenshot 3)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDF8EC),
                            border = BorderStroke(1.dp, FavoriteHeartCrimson),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFavoriteAdhkarDialog = true }
                                .testTag("hisn_card_top_favorite_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "الأذكار المفضلة",
                                    tint = FavoriteHeartCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                if (totalFavoritesCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$totalFavoritesCount",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FavoriteHeartCrimson
                                    )
                                }
                            }
                        }
                    }

                    // Quick Section Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HisnSectionGroup.values().forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFDF8EC),
                                border = BorderStroke(1.dp, GoldBorder),
                                modifier = Modifier.clickable {
                                    initialHisnGroupKey = group.key
                                    initialHisnChapterId = null
                                    showHisnCompleteDialog = true
                                }
                            ) {
                                Text(
                                    text = group.labelAr,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // 1. Primary Button: Open Complete Hisn Al-Muslim Index & Search
                    Button(
                        onClick = {
                            initialHisnGroupKey = HisnSectionGroup.ALL.key
                            initialHisnChapterId = null
                            showHisnCompleteDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("hisn_al_muslim_complete_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, GoldAccent)
                    ) {
                        Text(
                            text = "فتح فهرس حصن المسلم والبحث",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    // 2. External Favorites Button directly below the Hisn Al-Muslim Index button (as requested)
                    Button(
                        onClick = { showFavoriteAdhkarDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_favorite_adhkar_external_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFDF8EC),
                            contentColor = FavoriteHeartCrimson
                        ),
                        border = BorderStroke(1.3.dp, FavoriteHeartCrimson)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "الأذكار المفضلة",
                            tint = FavoriteHeartCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (totalFavoritesCount > 0)
                                "الأذكار والأدعية المفضلة ($totalFavoritesCount)"
                            else
                                "الأذكار والأدعية المفضلة وإضافة ذكر يدوي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = FavoriteHeartCrimson,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showHisnCompleteDialog) {
        HisnAlMuslimCompleteDialog(
            initialGroupKey = initialHisnGroupKey,
            initialChapterId = initialHisnChapterId,
            onDismiss = {
                refreshTrigger++
                initialHisnChapterId = null
                showHisnCompleteDialog = false
            }
        )
    }

    if (showFavoriteAdhkarDialog) {
        FavoriteAdhkarDialog(
            onOpenHisnChapter = { chapter ->
                showFavoriteAdhkarDialog = false
                initialHisnGroupKey = HisnSectionGroup.ALL.key
                initialHisnChapterId = chapter.id
                showHisnCompleteDialog = true
            },
            onDismiss = {
                refreshTrigger++
                showFavoriteAdhkarDialog = false
            }
        )
    }

    if (activeDialogType != null) {
        val isMorning = activeDialogType == "morning"
        AdhkarReaderDialog(
            trackType = if (isMorning) "morning" else "evening",
            alarmTargetId = if (isMorning) "adhkar_morning" else "adhkar_evening",
            defaultAlarmHour = if (isMorning) 6 else 17,
            title = if (isMorning) "أذكار الصباح المباركة" else "أذكار المساء المباركة",
            subtitle = if (isMorning) "حفظ وبركة وتحصين ليومك • اضغط مطولاً لتعديل أي ذكر أو تكراره" else "سكينة وطمأنينة وحفظ لليلتك • اضغط مطولاً لتعديل أي ذكر أو تكراره",
            items = if (isMorning) effectiveMorningItems else effectiveEveningItems,
            onDismiss = {
                refreshTrigger++
                activeDialogType = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdhkarReaderDialog(
    trackType: String,
    alarmTargetId: String,
    defaultAlarmHour: Int,
    title: String,
    subtitle: String,
    items: List<HisnDhikrItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("morning_evening_adhkar_prefs", Context.MODE_PRIVATE) }
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }
    val audioState by AdhkarAudioPlayerManager.state.collectAsState()
    val customizationVersion by AdhkarCustomizationManager.versionFlow.collectAsState()
    val isThisTrackPlaying = audioState.activeTrack == trackType && audioState.isPlaying

    val favoriteItemKeys = remember(customizationVersion) {
        AdhkarCustomizationManager.getFavoriteItems(context).map { it.uniqueKey }.toSet()
    }

    var showAddDhikrDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<HisnDhikrItem?>(null) }

    val counts = remember(items) {
        ensureDailyAdhkarResetIfNewDay(prefs)
        mutableStateMapOf<String, Int>().apply {
            items.forEach { item ->
                this[item.id] = prefs.getInt(item.id, 0)
            }
        }
    }

    val completedItemsCount = items.count { (counts[it.id] ?: 0) >= it.targetCount }
    val progress = if (items.isNotEmpty()) completedItemsCount.toFloat() / items.size.toFloat() else 0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = RoyalNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = AmiriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1A110B).copy(alpha = 0.78f),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Add Custom Dhikr to Morning/Evening
                                IconButton(
                                    onClick = { showAddDhikrDialog = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFDF8EC))
                                        .border(1.dp, EmeraldGreen, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "إضافة ذكر",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AlarmTriggerChip(
                                    targetId = alarmTargetId,
                                    defaultTitle = "منبه $title",
                                    defaultSubtitle = subtitle,
                                    defaultHour = defaultAlarmHour,
                                    defaultMinute = 0,
                                    compact = true
                                )

                                OutlinedButton(
                                    onClick = {
                                        items.forEach { item ->
                                            counts[item.id] = 0
                                            prefs.edit().putInt(item.id, 0).apply()
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تصفير الأذكار",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("تصفير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = EmeraldGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Audio Play Button inside the Dialog Header
                        Button(
                            onClick = { AdhkarAudioPlayerManager.togglePlay(context, trackType) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThisTrackPlaying) GoldAccent else EmeraldGreen,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Icon(
                                imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (trackType == "morning") {
                                    if (isThisTrackPlaying) "إيقاف مؤقت (أذكار الصباح)" else "▶ استماع أذكار الصباح مختصرة"
                                } else {
                                    if (isThisTrackPlaying) "إيقاف مؤقت (أذكار المساء)" else "▶ استماع أذكار المساء مختصرة"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        if (audioState.activeTrack == trackType) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AdhkarActiveAudioPlayerBar(
                                audioState = audioState,
                                context = context
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الإنجاز: $completedItemsCount من ${items.size} ذكر",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldGreen,
                            trackColor = GoldBorder.copy(alpha = 0.35f)
                        )
                    }
                }

                // Adhkar Cards List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val currentCount = counts[item.id] ?: 0
                        val isDone = currentCount >= item.targetCount
                        val isFav = favoriteItemKeys.contains(item.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (currentCount < item.targetCount) {
                                            val next = currentCount + 1
                                            counts[item.id] = next
                                            prefs.edit().putInt(item.id, next).apply()
                                            localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        editingItem = item
                                    }
                                )
                                .border(
                                    width = if (isDone) 1.5.dp else 1.dp,
                                    color = if (isDone) EmeraldGreen else GoldBorder,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone)
                                    EmeraldGreen.copy(alpha = 0.10f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Title + Heart Favorite Icon + Edit Chip + Target Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val unified = UnifiedDhikrItem(
                                                    uniqueKey = item.id,
                                                    scopeKey = trackType,
                                                    title = item.title,
                                                    categoryTitle = title,
                                                    arabicText = item.arabicText,
                                                    benefit = item.benefit,
                                                    targetCount = item.targetCount,
                                                    isCustomAdded = item.isCustomAdded
                                                )
                                                val added = AdhkarCustomizationManager.toggleDhikrFavorite(context, unified)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(
                                                    context,
                                                    if (added) "تمت إضافة «${item.title}» إلى المفضلة ❤️"
                                                    else "تمت إزالة الذكر من المفضلة",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "إضافة للمفضلة",
                                                tint = if (isFav) FavoriteHeartCrimson else GoldAccent,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                        }
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) EmeraldGreen else GoldAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEBE1C9),
                                            border = BorderStroke(0.8.dp, GoldBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { editingItem = item }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل الذكر والتكرار",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "تعديل",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1A110B)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDone) EmeraldGreen else RoyalNavyDark
                                        ) {
                                            Text(
                                                text = formatRepeatCountArabic(item.targetCount),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                color = if (isDone) Color.White else EmeraldGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Arabic Dhikr Text
                                Text(
                                    text = item.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 20.sp,
                                    lineHeight = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (item.benefit.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "الفضل: ${item.benefit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 11.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Interactive Counter Footer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (currentCount < item.targetCount) {
                                                val next = currentCount + 1
                                                counts[item.id] = next
                                                prefs.edit().putInt(item.id, next).apply()
                                                localStorage.incrementCumulativeAdhkarTasbeeh(1)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDone) EmeraldGreen else RoyalNavyDark,
                                            contentColor = if (isDone) Color.White else EmeraldGreen
                                        ),
                                        border = BorderStroke(1.dp, if (isDone) EmeraldGreen else GoldBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 46.dp)
                                    ) {
                                        Text(
                                            text = if (isDone) "تمت القراءة بحمد الله ✓ ($currentCount/${item.targetCount})"
                                            else "اضغط للعد ($currentCount / ${item.targetCount})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }

                                    if (currentCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                counts[item.id] = 0
                                                prefs.edit().putInt(item.id, 0).apply()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "إعادة",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDhikrDialog) {
        AddOrEditDhikrDialog(
            isEditMode = false,
            scopeKey = trackType,
            categoryTitle = title,
            onDismiss = { showAddDhikrDialog = false }
        )
    }

    editingItem?.let { target ->
        AddOrEditDhikrDialog(
            isEditMode = true,
            scopeKey = trackType,
            categoryTitle = title,
            initialUniqueKey = target.id,
            initialTitle = target.title,
            initialArabicText = target.arabicText,
            initialBenefit = target.benefit,
            initialTargetCount = target.targetCount,
            onDismiss = { editingItem = null }
        )
    }
}
