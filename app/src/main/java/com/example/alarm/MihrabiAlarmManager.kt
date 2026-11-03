package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

enum class ToneType {
    ISLAMIC_BUNDLED,
    SYSTEM_RINGTONE,
    CUSTOM_AUDIO_FILE
}

data class AlarmConfig(
    val targetId: String,
    val title: String,
    val subtitle: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val toneType: ToneType = ToneType.ISLAMIC_BUNDLED,
    val customToneUri: String = "",
    val customToneName: String = "نغمة إسلامية (يا عباد الله جدوا)"
) {
    fun formattedTime(): String {
        val amPm = if (hour < 12) "ص" else "م"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale("ar"), "%02d:%02d %s", h12, minute, amPm)
    }
}

object MihrabiAlarmManager {
    private const val TAG = "MihrabiAlarmManager"
    private const val PREFS_NAME = "mihrabi_alarms_prefs"
    private const val KEY_ALARMS_JSON = "alarms_json_list"
    const val CHANNEL_ID = "mihrabi_islamic_alarms_channel"

    const val ACTION_TRIGGER_ALARM = "com.example.ACTION_TRIGGER_MIHRABI_ALARM"
    const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_MIHRABI_ALARM"

    private var activeMediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    fun getAllAlarms(context: Context): List<AlarmConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ALARMS_JSON, null) ?: return emptyList()
        val list = mutableListOf<AlarmConfig>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val toneTypeStr = obj.optString("toneType", ToneType.ISLAMIC_BUNDLED.name)
                val toneType = try {
                    ToneType.valueOf(toneTypeStr)
                } catch (_: Exception) {
                    ToneType.ISLAMIC_BUNDLED
                }
                list.add(
                    AlarmConfig(
                        targetId = obj.getString("targetId"),
                        title = obj.optString("title", "منبه محرابي"),
                        subtitle = obj.optString("subtitle", "تذكير بالورد والذكر"),
                        hour = obj.optInt("hour", 6),
                        minute = obj.optInt("minute", 0),
                        isEnabled = obj.optBoolean("isEnabled", false),
                        toneType = toneType,
                        customToneUri = obj.optString("customToneUri", ""),
                        customToneName = obj.optString("customToneName", "نغمة إسلامية (يا عباد الله جدوا)")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse alarms JSON", e)
        }
        return list
    }

    fun getAlarmForTarget(
        context: Context,
        targetId: String,
        defaultTitle: String,
        defaultSubtitle: String = "تذكير يومي مبارك",
        defaultHour: Int = 7,
        defaultMinute: Int = 0
    ): AlarmConfig {
        return getAllAlarms(context).find { it.targetId == targetId }
            ?: AlarmConfig(
                targetId = targetId,
                title = defaultTitle,
                subtitle = defaultSubtitle,
                hour = defaultHour,
                minute = defaultMinute,
                isEnabled = false,
                toneType = ToneType.ISLAMIC_BUNDLED,
                customToneUri = "",
                customToneName = "نغمة إسلامية (يا عباد الله جدوا)"
            )
    }

    fun saveAndScheduleAlarm(context: Context, config: AlarmConfig) {
        val current = getAllAlarms(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.targetId == config.targetId }
        if (existingIdx >= 0) {
            current[existingIdx] = config
        } else {
            current.add(config)
        }
        persistAlarmsList(context, current)

        if (config.isEnabled) {
            scheduleExactOrInexactAlarm(context, config)
        } else {
            cancelScheduledAlarm(context, config.targetId)
        }
    }

    fun deleteAlarm(context: Context, targetId: String) {
        cancelScheduledAlarm(context, targetId)
        val current = getAllAlarms(context).filterNot { it.targetId == targetId }
        persistAlarmsList(context, current)
    }

    private fun persistAlarmsList(context: Context, list: List<AlarmConfig>) {
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("targetId", item.targetId)
                put("title", item.title)
                put("subtitle", item.subtitle)
                put("hour", item.hour)
                put("minute", item.minute)
                put("isEnabled", item.isEnabled)
                put("toneType", item.toneType.name)
                put("customToneUri", item.customToneUri)
                put("customToneName", item.customToneName)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS_JSON, arr.toString())
            .apply()
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleExactOrInexactAlarm(context: Context, config: AlarmConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MihrabiAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra("targetId", config.targetId)
            putExtra("title", config.title)
            putExtra("subtitle", config.subtitle)
            putExtra("toneType", config.toneType.name)
            putExtra("customToneUri", config.customToneUri)
        }

        val requestCode = config.targetId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.hour)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.i(TAG, "Scheduled alarm for ${config.targetId} at ${config.formattedTime()}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm security exception, falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    fun cancelScheduledAlarm(context: Context, targetId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MihrabiAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                targetId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling alarm $targetId", e)
        }
    }

    fun rescheduleAllEnabledAlarms(context: Context) {
        getAllAlarms(context).filter { it.isEnabled }.forEach { config ->
            scheduleExactOrInexactAlarm(context, config)
        }
    }

    fun playAlarmTone(
        context: Context,
        toneType: ToneType,
        customToneUri: String,
        durationMillis: Long = 45_000L
    ) {
        stopRingtone()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val player = when (toneType) {
                ToneType.ISLAMIC_BUNDLED -> {
                    MediaPlayer.create(context.applicationContext, R.raw.islamic_alarm_nasheed)
                }
                ToneType.SYSTEM_RINGTONE -> {
                    val uri = if (customToneUri.isNotBlank()) {
                        Uri.parse(customToneUri)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                    MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(context.applicationContext, uri)
                        prepare()
                    }
                }
                ToneType.CUSTOM_AUDIO_FILE -> {
                    if (customToneUri.isNotBlank()) {
                        MediaPlayer().apply {
                            setAudioAttributes(audioAttributes)
                            setDataSource(context.applicationContext, Uri.parse(customToneUri))
                            prepare()
                        }
                    } else {
                        MediaPlayer.create(context.applicationContext, R.raw.islamic_alarm_nasheed)
                    }
                }
            }

            if (player != null) {
                activeMediaPlayer = player
                player.isLooping = false
                player.start()

                val runnable = Runnable { stopRingtone() }
                stopRunnable = runnable
                mainHandler.postDelayed(runnable, durationMillis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play selected tone, falling back to bundled Islamic nasheed", e)
            try {
                val fallback = MediaPlayer.create(context.applicationContext, R.raw.islamic_alarm_nasheed)
                activeMediaPlayer = fallback
                fallback?.start()
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback player also failed", ex)
            }
        }
    }

    fun stopRingtone() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
        try {
            activeMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            activeMediaPlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            activeMediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "منبهات الأوراد والأذكار (محرابي)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات يومية للورد القرآني وأذكار الصباح والمساء والتسبيح"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}

class MihrabiAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            MihrabiAlarmManager.rescheduleAllEnabledAlarms(context)
            return
        }

        if (action == MihrabiAlarmManager.ACTION_STOP_ALARM) {
            MihrabiAlarmManager.stopRingtone()
            val notifId = intent.getIntExtra("notifId", 1001)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
            return
        }

        if (action == MihrabiAlarmManager.ACTION_TRIGGER_ALARM) {
            val targetId = intent.getStringExtra("targetId") ?: "general"
            val title = intent.getStringExtra("title") ?: "تذكير محرابي المبارك"
            val subtitle = intent.getStringExtra("subtitle") ?: "حان الآن موعد وردك وذكرك اليومي"
            val toneTypeStr = intent.getStringExtra("toneType") ?: ToneType.ISLAMIC_BUNDLED.name
            val customToneUri = intent.getStringExtra("customToneUri") ?: ""

            val toneType = try {
                ToneType.valueOf(toneTypeStr)
            } catch (_: Exception) {
                ToneType.ISLAMIC_BUNDLED
            }

            MihrabiAlarmManager.ensureNotificationChannel(context)
            MihrabiAlarmManager.playAlarmTone(context, toneType, customToneUri, durationMillis = 55_000L)

            val notifId = targetId.hashCode()

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                notifId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, MihrabiAlarmReceiver::class.java).apply {
                this.action = MihrabiAlarmManager.ACTION_STOP_ALARM
                putExtra("notifId", notifId)
            }
            val stopPending = PendingIntent.getBroadcast(
                context,
                notifId + 1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, MihrabiAlarmManager.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🕌 $title")
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "إيقاف الرنين",
                    stopPending
                )
                .build()

            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(notifId, notification)
            } catch (e: Exception) {
                Log.e("MihrabiAlarmReceiver", "Error posting alarm notification", e)
            }

            // Reschedule for tomorrow at the same time
            val savedConfig = MihrabiAlarmManager.getAllAlarms(context).find { it.targetId == targetId }
            if (savedConfig != null && savedConfig.isEnabled) {
                MihrabiAlarmManager.scheduleExactOrInexactAlarm(context, savedConfig)
            }
        }
    }
}
