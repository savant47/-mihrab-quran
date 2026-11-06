package com.example.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

enum class PrayerKey(val id: String, val arabicName: String, val iconEmoji: String) {
    FAJR("fajr", "الفجر", "🌅"),
    DHUHR("dhuhr", "الظهر", "☀️"),
    ASR("asr", "العصر", "🌤️"),
    MAGHRIB("maghrib", "المغرب", "🌇"),
    ISHA("isha", "العشاء", "🌙")
}

enum class AdhanAlertMode(val titleAr: String, val descriptionAr: String) {
    FULL_ADHAN("أذان كامل (الصوت المرفق)", "تشغيل صوت الأذان الشرعي الكامل المرفق بالتطبيق"),
    SHORT_ADHAN("مختصر أذان (التكبيرات فقط)", "تشغيل تكبيرات الأذان الأولى فقط (١٢ ثانية)"),
    NOTIFICATION_ONLY("مجرد إشعار فقط (بدون أذان)", "إظهار إشعار دخول وقت الصلاة مع تنبيه خفيف بدون أذان مطول"),
    CUSTOM_UPLOADED("نغمة أذان مخصصة من الهاتف", "اختيار ملف صوتي مخصص (مثل مختصر أذان أو مؤذن آخر) من ملفات هاتفك"),
    ISLAMIC_NASHEED("النغمة الإسلامية (يا عباد الله جدوا)", "تشغيل نغمة التذكير الإسلامية المرفقة"),
    OFF("إيقاف منبه هذه الصلاة", "عدم إصدار تنبيه لهذه الصلاة")
}

enum class CalculationMethod(
    val titleAr: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int = 0
) {
    EGYPTIAN_SURVEY("الهيئة المصرية العامة للمساحة", 19.5, 17.5, 0),
    UMM_AL_QURA("تقويم أم القرى (مكة المكرمة)", 18.5, 0.0, 90),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", 18.0, 17.0, 0),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0, 0)
}

data class PrayerTimeSlot(
    val prayer: PrayerKey,
    val hour: Int,
    val minute: Int,
    val alertMode: AdhanAlertMode
) {
    fun formatted12Hour(): String {
        val isPm = hour >= 12
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val suffix = if (isPm) "م" else "ص"
        return String.format(Locale("ar"), "%02d:%02d %s", h12, minute, suffix)
    }

    fun totalMinutesFromMidnight(): Int = hour * 60 + minute
}

data class AdhanSettingsState(
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    val locationName: String = "القاهرة، مصر",
    val isAutoLocation: Boolean = true,
    val calculationMethod: CalculationMethod = CalculationMethod.EGYPTIAN_SURVEY,
    val globalAlertMode: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val customAdhanUri: String = "",
    val customAdhanFileName: String = "",
    val perPrayerModes: Map<PrayerKey, AdhanAlertMode> = PrayerKey.values().associateWith { AdhanAlertMode.FULL_ADHAN }
)

data class CityPreset(
    val nameAr: String,
    val latitude: Double,
    val longitude: Double
)

object PrayerTimesAndAdhanManager {
    private const val TAG = "PrayerTimesAdhanMgr"
    private const val PREFS_NAME = "mihrabi_prayer_adhan_prefs"
    private const val KEY_SETTINGS_JSON = "adhan_settings_json"
    const val ADHAN_CHANNEL_ID = "mihrabi_adhan_five_prayers_channel"
    const val ACTION_TRIGGER_PRAYER_ADHAN = "com.example.ACTION_TRIGGER_PRAYER_ADHAN"

    val presetCities = listOf(
        CityPreset("العريش - شمال سيناء، مصر", 31.1316, 33.7984),
        CityPreset("بئر العبد - شمال سيناء، مصر", 31.0189, 33.0100),
        CityPreset("الشيخ زويد - شمال سيناء، مصر", 31.2119, 34.1106),
        CityPreset("رفح - شمال سيناء، مصر", 31.2870, 34.2380),
        CityPreset("القاهرة، مصر", 30.0444, 31.2357),
        CityPreset("الجيزة، مصر", 30.0131, 31.2089),
        CityPreset("الإسكندرية، مصر", 31.2001, 29.9187),
        CityPreset("بورسعيد، مصر", 31.2653, 32.3019),
        CityPreset("الإسماعيلية، مصر", 30.5965, 32.2715),
        CityPreset("السويس، مصر", 29.9668, 32.5498),
        CityPreset("دمياط، مصر", 31.4175, 31.8144),
        CityPreset("المنصورة - الدقهلية، مصر", 31.0409, 31.3785),
        CityPreset("الزقازيق - الشرقية، مصر", 30.5877, 31.5020),
        CityPreset("طنطا - الغربية، مصر", 30.7865, 31.0004),
        CityPreset("شبين الكوم - المنوفية، مصر", 30.5503, 31.0106),
        CityPreset("بنها - القليوبية، مصر", 30.4660, 31.1848),
        CityPreset("دمنهور - البحيرة، مصر", 31.0341, 30.4682),
        CityPreset("كفر الشيخ، مصر", 31.1107, 30.9388),
        CityPreset("مرسى مطروح، مصر", 31.3543, 27.2373),
        CityPreset("طور سيناء - جنوب سيناء، مصر", 28.2417, 33.6222),
        CityPreset("شرم الشيخ - جنوب سيناء، مصر", 27.9158, 34.3300),
        CityPreset("الفيوم، مصر", 29.3084, 30.8428),
        CityPreset("بني سويف، مصر", 29.0661, 31.0994),
        CityPreset("المنيا، مصر", 28.1099, 30.7503),
        CityPreset("أسيوط، مصر", 27.1783, 31.1859),
        CityPreset("سوهاج، مصر", 26.5591, 31.6957),
        CityPreset("قنا، مصر", 26.1551, 32.7160),
        CityPreset("الأقصر، مصر", 25.6872, 32.6396),
        CityPreset("أسوان، مصر", 24.0889, 32.8998),
        CityPreset("الغردقة - البحر الأحمر، مصر", 27.2579, 33.8116),
        CityPreset("الخارجة - الوادي الجديد، مصر", 25.4390, 30.5586),
        CityPreset("مكة المكرمة، السعودية", 21.4225, 39.8262),
        CityPreset("المدينة المنورة، السعودية", 24.4672, 39.6112),
        CityPreset("الرياض، السعودية", 24.7136, 46.6753),
        CityPreset("جدة، السعودية", 21.5433, 39.1728),
        CityPreset("الدمام، السعودية", 26.4207, 50.0888),
        CityPreset("دبي، الإمارات", 25.2048, 55.2708),
        CityPreset("أبوظبي، الإمارات", 24.4539, 54.3773),
        CityPreset("الكويت، الكويت", 29.3759, 47.9774),
        CityPreset("الدوحة، قطر", 25.2854, 51.5310),
        CityPreset("المنامة، البحرين", 26.2285, 50.5860),
        CityPreset("مسقط، عُمان", 23.5880, 58.3829),
        CityPreset("عمان، الأردن", 31.9454, 35.9284),
        CityPreset("القدس الشريف، فلسطين", 31.7683, 35.2137),
        CityPreset("غزة، فلسطين", 31.5017, 34.4668),
        CityPreset("بغداد، العراق", 33.3152, 44.3661),
        CityPreset("دمشق، سوريا", 33.5138, 36.2765),
        CityPreset("بيروت، لبنان", 33.8938, 35.5018),
        CityPreset("صنعاء، اليمن", 15.3694, 44.1910),
        CityPreset("الخرطوم، السودان", 15.5007, 32.5599),
        CityPreset("طرابلس، ليبيا", 32.8872, 13.1913),
        CityPreset("بنغازي، ليبيا", 32.1194, 20.0868),
        CityPreset("تونس، تونس", 36.8065, 10.1815),
        CityPreset("الجزائر، الجزائر", 36.7538, 3.0588),
        CityPreset("الدار البيضاء، المغرب", 33.5731, -7.5898),
        CityPreset("الرباط، المغرب", 34.0209, -6.8416),
        CityPreset("إسطنبول، تركيا", 41.0082, 28.9784),
        CityPreset("لندن، المملكة المتحدة", 51.5074, -0.1278),
        CityPreset("باريس، فرنسا", 48.8566, 2.3522),
        CityPreset("برلين، ألمانيا", 52.5200, 13.4050),
        CityPreset("نيويورك، الولايات المتحدة", 40.7128, -74.0060),
        CityPreset("كوالالمبور، ماليزيا", 3.1390, 101.6869),
        CityPreset("جاكرتا، إندونيسيا", -6.2088, 106.8456)
    )

    /**
     * Cleans administrative police/census prefixes (such as "قسم اول العريش" or "Qism Awal Al Arish")
     * into eloquent Arabic city + governorate names ("العريش - شمال سيناء، مصر").
     */
    fun normalizeArabicCityDisplayName(
        rawCity: String,
        adminArea: String = "",
        country: String = ""
    ): String {
        val combinedCheck = "$rawCity $adminArea $country".lowercase(Locale.ROOT)
        if (combinedCheck.contains("العريش") || combinedCheck.contains("عريش") || combinedCheck.contains("arish")) {
            return "العريش - شمال سيناء، مصر"
        }
        if (combinedCheck.contains("الشيخ زويد") || combinedCheck.contains("sheikh zuweid")) {
            return "الشيخ زويد - شمال سيناء، مصر"
        }
        if (combinedCheck.contains("بئر العبد") || combinedCheck.contains("bir al-abd")) {
            return "بئر العبد - شمال سيناء، مصر"
        }

        var cleanedCity = rawCity
            .replace(Regex("^(قسم\\s*(أول|اول|ثان|ثاني|ثالث|رابع|[1-4]|١|٢|٣|٤)\\s+)"), "")
            .replace(Regex("^(مركز\\s+|حي\\s+|مدينة\\s+)"), "")
            .replace(Regex("(?i)^(qism\\s*(awal|thani|thalith)?\\s+|markaz\\s+)"), "")
            .trim()

        val cleanedAdmin = adminArea
            .replace("محافظة ", "")
            .replace("Governorate", "", ignoreCase = true)
            .trim()

        val cleanedCountry = when {
            country.equals("Egypt", ignoreCase = true) -> "مصر"
            country.equals("Saudi Arabia", ignoreCase = true) -> "السعودية"
            else -> country.trim()
        }

        if (cleanedCity.isBlank() && cleanedAdmin.isNotBlank()) {
            cleanedCity = cleanedAdmin
        }

        return when {
            cleanedCity.isNotBlank() && cleanedAdmin.isNotBlank() &&
                    !cleanedCity.contains(cleanedAdmin) && cleanedCountry.isNotBlank() &&
                    !cleanedCity.contains(cleanedCountry) -> {
                "$cleanedCity - $cleanedAdmin، $cleanedCountry"
            }
            cleanedCity.isNotBlank() && cleanedCountry.isNotBlank() && !cleanedCity.contains(cleanedCountry) -> {
                "$cleanedCity، $cleanedCountry"
            }
            cleanedCity.isNotBlank() -> cleanedCity
            cleanedCountry.isNotBlank() -> cleanedCountry
            else -> "القاهرة، مصر"
        }
    }

    fun loadSettings(context: Context): AdhanSettingsState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SETTINGS_JSON, null) ?: return AdhanSettingsState()
        return try {
            val obj = JSONObject(raw)
            val methodStr = obj.optString("calculationMethod", CalculationMethod.EGYPTIAN_SURVEY.name)
            val method = try {
                CalculationMethod.valueOf(methodStr)
            } catch (_: Exception) {
                CalculationMethod.EGYPTIAN_SURVEY
            }

            val globalModeStr = obj.optString("globalAlertMode", AdhanAlertMode.FULL_ADHAN.name)
            val globalMode = try {
                AdhanAlertMode.valueOf(globalModeStr)
            } catch (_: Exception) {
                AdhanAlertMode.FULL_ADHAN
            }

            val modesObj = obj.optJSONObject("perPrayerModes")
            val perPrayerMap = mutableMapOf<PrayerKey, AdhanAlertMode>()
            for (pk in PrayerKey.values()) {
                val mStr = modesObj?.optString(pk.name, globalMode.name) ?: globalMode.name
                perPrayerMap[pk] = try {
                    AdhanAlertMode.valueOf(mStr)
                } catch (_: Exception) {
                    globalMode
                }
            }

            val rawLocName = obj.optString("locationName", "القاهرة، مصر")
            val normalizedLocName = normalizeArabicCityDisplayName(rawLocName)

            AdhanSettingsState(
                latitude = obj.optDouble("latitude", 30.0444),
                longitude = obj.optDouble("longitude", 31.2357),
                locationName = normalizedLocName,
                isAutoLocation = obj.optBoolean("isAutoLocation", true),
                calculationMethod = method,
                globalAlertMode = globalMode,
                customAdhanUri = obj.optString("customAdhanUri", ""),
                customAdhanFileName = obj.optString("customAdhanFileName", ""),
                perPrayerModes = perPrayerMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Adhan settings", e)
            AdhanSettingsState()
        }
    }

    fun saveSettingsAndSchedule(context: Context, state: AdhanSettingsState) {
        try {
            val normalizedState = state.copy(
                locationName = normalizeArabicCityDisplayName(state.locationName)
            )
            val modesObj = JSONObject()
            normalizedState.perPrayerModes.forEach { (k, v) ->
                modesObj.put(k.name, v.name)
            }
            val obj = JSONObject().apply {
                put("latitude", normalizedState.latitude)
                put("longitude", normalizedState.longitude)
                put("locationName", normalizedState.locationName)
                put("isAutoLocation", normalizedState.isAutoLocation)
                put("calculationMethod", normalizedState.calculationMethod.name)
                put("globalAlertMode", normalizedState.globalAlertMode.name)
                put("customAdhanUri", normalizedState.customAdhanUri)
                put("customAdhanFileName", normalizedState.customAdhanFileName)
                put("perPrayerModes", modesObj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SETTINGS_JSON, obj.toString())
                .apply()

            scheduleAllFivePrayers(context, normalizedState)
            syncOfficialTimingsFromAlAdhanApi(context, normalizedState)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Adhan settings", e)
        }
    }

    /**
     * Searches for any city worldwide (Online via Open-Meteo Arabic Geocoding API + Android Geocoder + Offline Presets).
     */
    fun searchCitiesWorldwide(
        context: Context,
        query: String,
        onResults: (List<CityPreset>) -> Unit
    ) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            onResults(presetCities)
            return
        }

        val localMatches = presetCities.filter {
            it.nameAr.contains(trimmed, ignoreCase = true)
        }
        // Immediately return local matches first for instant UI responsiveness
        if (localMatches.isNotEmpty()) {
            onResults(localMatches)
        }

        Thread {
            val combined = LinkedHashMap<String, CityPreset>()
            for (preset in localMatches) {
                combined[preset.nameAr] = preset
            }

            // 1. Query Open-Meteo Global Geocoding API in Arabic
            try {
                val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
                val url = java.net.URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=15&language=ar&format=json")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val resultsArr = root.optJSONArray("results")
                    if (resultsArr != null) {
                        for (i in 0 until resultsArr.length()) {
                            val item = resultsArr.getJSONObject(i)
                            val name = item.optString("name", "")
                            val admin1 = item.optString("admin1", "")
                            val country = item.optString("country", "")
                            val lat = item.optDouble("latitude", Double.NaN)
                            val lon = item.optDouble("longitude", Double.NaN)
                            if (name.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                                val display = normalizeArabicCityDisplayName(name, admin1, country)
                                if (!combined.containsKey(display)) {
                                    combined[display] = CityPreset(display, lat, lon)
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }

            // 2. Fallback/Supplement with Android Geocoder
            if (combined.size < 5) {
                try {
                    val geocoder = Geocoder(context, Locale("ar"))
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocationName(trimmed, 6)
                    if (!list.isNullOrEmpty()) {
                        for (addr in list) {
                            if (addr.hasLatitude() && addr.hasLongitude()) {
                                val cName = addr.locality ?: addr.subAdminArea ?: addr.featureName ?: trimmed
                                val admin = addr.adminArea ?: ""
                                val country = addr.countryName ?: ""
                                val display = normalizeArabicCityDisplayName(cName, admin, country)
                                if (!combined.containsKey(display)) {
                                    combined[display] = CityPreset(display, addr.latitude, addr.longitude)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            val finalResults = combined.values.toList()
            Handler(Looper.getMainLooper()).post {
                onResults(finalResults)
            }
        }.start()
    }

    /**
     * Syncs official daily prayer times from AlAdhan API when online and caches them for today's date & coordinates.
     * Falls back seamlessly to the local astronomical calculator when offline.
     */
    fun syncOfficialTimingsFromAlAdhanApi(
        context: Context,
        settings: AdhanSettingsState,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        Thread {
            var synced = false
            try {
                val cal = Calendar.getInstance()
                val dateKey = String.format(
                    Locale.US,
                    "%04d-%02d-%02d_%.2f_%.2f_%s",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    settings.latitude,
                    settings.longitude,
                    settings.calculationMethod.name
                )
                val methodId = when (settings.calculationMethod) {
                    CalculationMethod.EGYPTIAN_SURVEY -> 5
                    CalculationMethod.UMM_AL_QURA -> 4
                    CalculationMethod.MUSLIM_WORLD_LEAGUE -> 3
                    CalculationMethod.KARACHI -> 1
                }
                val urlStr = String.format(
                    Locale.US,
                    "https://api.aladhan.com/v1/timings?latitude=%.4f&longitude=%.4f&method=%d",
                    settings.latitude,
                    settings.longitude,
                    methodId
                )
                val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val timings = JSONObject(body).optJSONObject("data")?.optJSONObject("timings")
                    if (timings != null) {
                        val cacheObj = JSONObject().apply {
                            put("dateKey", dateKey)
                            put("Fajr", timings.optString("Fajr", ""))
                            put("Dhuhr", timings.optString("Dhuhr", ""))
                            put("Asr", timings.optString("Asr", ""))
                            put("Maghrib", timings.optString("Maghrib", ""))
                            put("Isha", timings.optString("Isha", ""))
                        }
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString("cached_aladhan_timings", cacheObj.toString())
                            .apply()
                        synced = true
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }
            Handler(Looper.getMainLooper()).post {
                onComplete?.invoke(synced)
            }
        }.start()
    }

    /**
     * Automatically detects device GPS / Network coordinates and reverse-geocodes city name in Arabic.
     */
    @SuppressLint("MissingPermission")
    fun detectLocationAutomatically(
        context: Context,
        onResult: (latitude: Double, longitude: Double, locationName: String, success: Boolean) -> Unit
    ) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null
            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.accuracy < bestLocation!!.accuracy) {
                                bestLocation = loc
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (bestLocation != null) {
                val lat = bestLocation!!.latitude
                val lon = bestLocation!!.longitude
                val name = resolveCityName(context, lat, lon)
                onResult(lat, lon, name, true)
                return
            }

            // Request a fresh location update if no cached location is available
            val enabledProvider = providers.firstOrNull {
                try {
                    locationManager.isProviderEnabled(it)
                } catch (_: Exception) {
                    false
                }
            }

            if (enabledProvider != null) {
                val mainHandler = Handler(Looper.getMainLooper())
                var responded = false
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (!responded) {
                            responded = true
                            locationManager.removeUpdates(this)
                            val name = resolveCityName(context, location.latitude, location.longitude)
                            onResult(location.latitude, location.longitude, name, true)
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                locationManager.requestLocationUpdates(
                    enabledProvider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )

                mainHandler.postDelayed({
                    if (!responded) {
                        responded = true
                        try {
                            locationManager.removeUpdates(listener)
                        } catch (_: Exception) {
                        }
                        val saved = loadSettings(context)
                        onResult(saved.latitude, saved.longitude, normalizeArabicCityDisplayName(saved.locationName), true)
                    }
                }, 3500L)
            } else {
                val saved = loadSettings(context)
                onResult(saved.latitude, saved.longitude, normalizeArabicCityDisplayName(saved.locationName), false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto location detection error", e)
            val saved = loadSettings(context)
            onResult(saved.latitude, saved.longitude, normalizeArabicCityDisplayName(saved.locationName), false)
        }
    }

    private fun resolveCityName(context: Context, lat: Double, lon: Double): String {
        // First check if coordinates are very close (< 18 km) to a known preset city (e.g. Al-Arish)
        val nearestPreset = presetCities.minByOrNull { city ->
            hypot(city.latitude - lat, city.longitude - lon)
        }
        if (nearestPreset != null && hypot(nearestPreset.latitude - lat, nearestPreset.longitude - lon) < 0.16) {
            return nearestPreset.nameAr
        }

        try {
            val geocoder = Geocoder(context, Locale("ar"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val admin = addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                if (city.isNotBlank() || admin.isNotBlank() || country.isNotBlank()) {
                    return normalizeArabicCityDisplayName(city, admin, country)
                }
            }
        } catch (_: Exception) {
        }
        // Fallback to nearest preset city if geocoder is unavailable offline
        if (nearestPreset != null && hypot(nearestPreset.latitude - lat, nearestPreset.longitude - lon) < 2.5) {
            return nearestPreset.nameAr
        }
        return String.format(Locale.US, "موقع GPS (%.2f, %.2f)", lat, lon)
    }

    /**
     * Calculates the 5 daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) astronomically
     * for the given date and coordinates. Works 100% offline.
     */
    fun calculateDailyPrayerTimes(
        settings: AdhanSettingsState,
        calendar: Calendar = Calendar.getInstance()
    ): List<PrayerTimeSlot> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val tz = calendar.timeZone
        val tzOffsetHours = tz.getOffset(calendar.timeInMillis) / (1000.0 * 3600.0)

        val jd = julianDate(year, month, day) - settings.longitude / (15.0 * 24.0)
        val (declination, eqOfTime) = sunPosition(jd)

        // Solar Noon (Dhuhr) in local hours
        val dhuhrHours = 12.0 + tzOffsetHours - (settings.longitude / 15.0) - eqOfTime

        // Fajr Angle
        val fajrDiff = sunAngleTime(settings.calculationMethod.fajrAngle, settings.latitude, declination)
        val fajrHours = dhuhrHours - fajrDiff

        // Asr (Shafi'i / Standard shadow factor = 1)
        val asrDiff = asrTimeDiff(1.0, settings.latitude, declination)
        val asrHours = dhuhrHours + asrDiff

        // Maghrib (Sun altitude = 0.833 degrees for refraction & solar disc radius)
        val maghribDiff = sunAngleTime(0.833, settings.latitude, declination)
        val maghribHours = dhuhrHours + maghribDiff

        // Isha
        val ishaHours = if (settings.calculationMethod.ishaMinutesAfterMaghrib > 0) {
            maghribHours + (settings.calculationMethod.ishaMinutesAfterMaghrib / 60.0)
        } else {
            val ishaDiff = sunAngleTime(settings.calculationMethod.ishaAngle, settings.latitude, declination)
            dhuhrHours + ishaDiff
        }

        // Add 2 minutes precaution for Dhuhr after meridian transit
        val dhuhrAdjusted = dhuhrHours + (2.0 / 60.0)

        fun hoursToHourMinute(h: Double): Pair<Int, Int> {
            var normalized = h % 24.0
            if (normalized < 0) normalized += 24.0
            val totalMinutes = (normalized * 60.0).roundToInt()
            val hr = (totalMinutes / 60) % 24
            val min = totalMinutes % 60
            return Pair(hr, min)
        }

        val (fajrH, fajrM) = hoursToHourMinute(fajrHours)
        val (dhuhrH, dhuhrM) = hoursToHourMinute(dhuhrAdjusted)
        val (asrH, asrM) = hoursToHourMinute(asrHours)
        val (maghribH, maghribM) = hoursToHourMinute(maghribHours)
        val (ishaH, ishaM) = hoursToHourMinute(ishaHours)

        return listOf(
            PrayerTimeSlot(
                PrayerKey.FAJR,
                fajrH,
                fajrM,
                settings.perPrayerModes[PrayerKey.FAJR] ?: settings.globalAlertMode
            ),
            PrayerTimeSlot(
                PrayerKey.DHUHR,
                dhuhrH,
                dhuhrM,
                settings.perPrayerModes[PrayerKey.DHUHR] ?: settings.globalAlertMode
            ),
            PrayerTimeSlot(
                PrayerKey.ASR,
                asrH,
                asrM,
                settings.perPrayerModes[PrayerKey.ASR] ?: settings.globalAlertMode
            ),
            PrayerTimeSlot(
                PrayerKey.MAGHRIB,
                maghribH,
                maghribM,
                settings.perPrayerModes[PrayerKey.MAGHRIB] ?: settings.globalAlertMode
            ),
            PrayerTimeSlot(
                PrayerKey.ISHA,
                ishaH,
                ishaM,
                settings.perPrayerModes[PrayerKey.ISHA] ?: settings.globalAlertMode
            )
        )
    }

    /**
     * Returns the next upcoming PrayerTimeSlot and remaining hours/minutes text.
     */
    fun getNextPrayerInfo(slots: List<PrayerTimeSlot>, now: Calendar = Calendar.getInstance()): Pair<PrayerTimeSlot, String> {
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val upcomingToday = slots.firstOrNull { it.totalMinutesFromMidnight() > currentMinutes }
        val nextSlot = upcomingToday ?: slots.first()
        val diffMinutes = if (upcomingToday != null) {
            upcomingToday.totalMinutesFromMidnight() - currentMinutes
        } else {
            (24 * 60 - currentMinutes) + nextSlot.totalMinutesFromMidnight()
        }
        val remHours = diffMinutes / 60
        val remMins = diffMinutes % 60
        val remainingText = if (remHours > 0) {
            "متبقي $remHours ساعة و $remMins دقيقة"
        } else {
            "متبقي $remMins دقيقة"
        }
        return Pair(nextSlot, remainingText)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return Pair(decl, eqTime)
    }

    private fun sunAngleTime(angle: Double, latitude: Double, declination: Double): Double {
        val term = (-dsin(angle) - dsin(latitude) * dsin(declination)) / (dcos(latitude) * dcos(declination))
        val clamped = term.coerceIn(-1.0, 1.0)
        return darccos(clamped) / 15.0
    }

    private fun asrTimeDiff(shadowFactor: Double, latitude: Double, declination: Double): Double {
        val angle = -darccot(shadowFactor + dtan(abs(latitude - declination)))
        return sunAngleTime(angle, latitude, declination)
    }

    private fun dsin(d: Double): Double = sin(Math.toRadians(d))
    private fun dcos(d: Double): Double = cos(Math.toRadians(d))
    private fun dtan(d: Double): Double = tan(Math.toRadians(d))
    private fun darcsin(x: Double): Double = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun darccos(x: Double): Double = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double): Double = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double): Double = Math.toDegrees(atan(1.0 / x))

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    /**
     * Schedules the 5 daily Adhan alarms with Android's AlarmManager.
     */
    fun scheduleAllFivePrayers(context: Context, settings: AdhanSettingsState = loadSettings(context)) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val slots = calculateDailyPrayerTimes(settings)

        for (slot in slots) {
            val requestCode = ("adhan_${slot.prayer.id}").hashCode()
            val intent = Intent(context, MihrabiAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_PRAYER_ADHAN
                putExtra("prayerId", slot.prayer.id)
                putExtra("prayerNameAr", slot.prayer.arabicName)
                putExtra("alertMode", slot.alertMode.name)
                putExtra("customAdhanUri", settings.customAdhanUri)
                putExtra("locationName", settings.locationName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (slot.alertMode == AdhanAlertMode.OFF) {
                try {
                    alarmManager.cancel(pendingIntent)
                } catch (_: Exception) {
                }
                continue
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, slot.hour)
                set(Calendar.MINUTE, slot.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } catch (_: Exception) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Plays the Adhan sound according to the selected [AdhanAlertMode].
     */
    fun playAdhanAudio(
        context: Context,
        mode: AdhanAlertMode,
        customUri: String = ""
    ) {
        MihrabiAlarmManager.stopRingtone()
        if (mode == AdhanAlertMode.OFF) return

        if (mode == AdhanAlertMode.NOTIFICATION_ONLY) {
            try {
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context.applicationContext, notifUri)
                r?.play()
            } catch (_: Exception) {
            }
            return
        }

        val durationMs = when (mode) {
            AdhanAlertMode.FULL_ADHAN -> 165_000L // Full 2:33 Adhan
            AdhanAlertMode.SHORT_ADHAN -> 12_500L // First 12.5 seconds ("Allahu Akbar Allahu Akbar")
            AdhanAlertMode.CUSTOM_UPLOADED -> 180_000L
            AdhanAlertMode.ISLAMIC_NASHEED -> 55_000L
            else -> 30_000L
        }

        MihrabiAlarmManager.playRawOrUriAudio(
            context = context,
            rawResId = if (mode == AdhanAlertMode.ISLAMIC_NASHEED) R.raw.islamic_alarm_nasheed else R.raw.adhan_makkah_full,
            customUri = if (mode == AdhanAlertMode.CUSTOM_UPLOADED) customUri else "",
            durationMillis = durationMs
        )
    }

    fun handleTriggeredAdhanBroadcast(context: Context, intent: Intent) {
        val prayerId = intent.getStringExtra("prayerId") ?: "fajr"
        val prayerNameAr = intent.getStringExtra("prayerNameAr") ?: "الصلاة"
        val alertModeStr = intent.getStringExtra("alertMode") ?: AdhanAlertMode.FULL_ADHAN.name
        val customUri = intent.getStringExtra("customAdhanUri") ?: ""
        val locationName = intent.getStringExtra("locationName") ?: ""

        val alertMode = try {
            AdhanAlertMode.valueOf(alertModeStr)
        } catch (_: Exception) {
            AdhanAlertMode.FULL_ADHAN
        }

        if (alertMode == AdhanAlertMode.OFF) return

        ensureAdhanNotificationChannel(context)
        playAdhanAudio(context, alertMode, customUri)

        val notifId = ("adhan_notif_$prayerId").hashCode()
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
            action = MihrabiAlarmManager.ACTION_STOP_ALARM
            putExtra("notifId", notifId)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = when (alertMode) {
            AdhanAlertMode.FULL_ADHAN -> "حي على الصلاة • حي على الفلاح (أذان كامل)"
            AdhanAlertMode.SHORT_ADHAN -> "الله أكبر الله أكبر (مختصر الأذان)"
            AdhanAlertMode.NOTIFICATION_ONLY -> "حان الآن وقت صلاة $prayerNameAr"
            AdhanAlertMode.CUSTOM_UPLOADED -> "حي على الصلاة (نغمة الأذان المخصصة)"
            AdhanAlertMode.ISLAMIC_NASHEED -> "تذكير بصلاة $prayerNameAr"
            AdhanAlertMode.OFF -> ""
        }

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🕌 حان الآن موعد أذان $prayerNameAr ${if (locationName.isNotBlank()) "($locationName)" else ""}")
            .setContentText(modeLabel)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(alertMode != AdhanAlertMode.NOTIFICATION_ONLY)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_media_pause,
                "إيقاف صوت الأذان",
                stopPending
            )
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notifId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Adhan notification", e)
        }

        // Reschedule all 5 prayers for tomorrow
        scheduleAllFivePrayers(context)
    }

    private fun ensureAdhanNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(ADHAN_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    ADHAN_CHANNEL_ID,
                    "مواقيت الصلاة والأذان الشرعي (محرابي)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات الأذان للصلوات الخمس حسب موقعك الجغرافي"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
