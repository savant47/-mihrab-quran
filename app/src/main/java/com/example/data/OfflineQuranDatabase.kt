package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class OfflineVerse(
    val surahNumber: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val revelationPlaceAr: String,
    val ayahNumber: Int,
    val textAr: String,
    val textEn: String,
    val tafsirMuyassar: String,
    val juz: Int,
    val hizb: Int = 1,
    val rubInHizb: Int = 1,
    val page: Int,
    val isSajda: Boolean,
    val normalizedTextAr: String
)

data class OfflineSurah(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val transliteration: String,
    val revelationPlaceAr: String,
    val versesCount: Int,
    val startPage: Int,
    val verses: List<OfflineVerse>
)

object OfflineQuranDatabase {
    private const val TAG = "OfflineQuranDatabase"
    private const val ASSET_FILE = "quran_full_offline.json"

    // Canonical 41 Rub' Al-Hizb start positions that coincide with Ayah 1 of a Surah
    // (Together with the 199 in-text '\u06DE' ۞ markers in quran_full_offline.json = 240 exact Rub' Al-Hizbs / 60 Hizbs / 30 Juz)
    private val SURAH_START_RUB_KEYS = setOf(
        "1:1", "4:1", "5:1", "7:1", "8:1", "9:1", "15:1", "16:1", "17:1",
        "20:1", "21:1", "22:1", "23:1", "24:1", "25:1", "26:1", "27:1",
        "29:1", "30:1", "33:1", "40:1", "46:1", "51:1", "55:1", "56:1",
        "58:1", "61:1", "62:1", "65:1", "67:1", "68:1", "69:1", "72:1",
        "75:1", "78:1", "80:1", "82:1", "84:1", "87:1", "90:1", "94:1"
    )

    @Volatile
    private var isLoaded = false

    private var allSurahs: List<OfflineSurah> = emptyList()
    private var versesByPage: Map<Int, List<OfflineVerse>> = emptyMap()
    private var allVersesList: List<OfflineVerse> = emptyList()

    fun ensureLoadedSynchronously(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            loadFromAssetsInternal(context.applicationContext)
        }
    }

    suspend fun ensureLoaded(context: Context) = withContext(Dispatchers.IO) {
        ensureLoadedSynchronously(context)
    }

    private fun loadFromAssetsInternal(context: Context) {
        try {
            val startTime = System.currentTimeMillis()
            context.assets.open(ASSET_FILE).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonText = reader.readText()
                val rootArray = JSONArray(jsonText)

                val loadedSurahs = ArrayList<OfflineSurah>(114)
                val loadedVerses = ArrayList<OfflineVerse>(6236)
                val pageMap = HashMap<Int, MutableList<OfflineVerse>>(610)
                var currentRubIndex = -1

                for (i in 0 until rootArray.length()) {
                    val sObj = rootArray.getJSONObject(i)
                    val sNum = sObj.getInt("num")
                    val sAr = sObj.getString("ar")
                    val sEn = sObj.optString("en", "")
                    val sTr = sObj.optString("tr", "")
                    val sRev = sObj.optString("rev", "مكية")
                    val sCnt = sObj.getInt("cnt")
                    val vArr = sObj.getJSONArray("v")

                    val surahVerses = ArrayList<OfflineVerse>(sCnt)
                    var firstPage = 1

                    for (j in 0 until vArr.length()) {
                        val vObj = vArr.getJSONObject(j)
                        val vNum = vObj.getInt("n")
                        val arText = vObj.getString("ar")
                        val enText = vObj.optString("en", "")
                        val tafsir = vObj.optString("tf", "")
                        val juz = vObj.optInt("j", 1)
                        val page = vObj.optInt("p", 1).coerceIn(1, 604)
                        val sajda = vObj.optBoolean("s", false)

                        if (arText.contains('\u06DE') || SURAH_START_RUB_KEYS.contains("$sNum:$vNum")) {
                            currentRubIndex++
                        }
                        val safeRubIdx = currentRubIndex.coerceIn(0, 239)
                        val exactHizb = ((safeRubIdx / 4) + 1).coerceIn(1, 60)
                        val exactRubInHizb = ((safeRubIdx % 4) + 1).coerceIn(1, 4)

                        if (j == 0) firstPage = page

                        val verse = OfflineVerse(
                            surahNumber = sNum,
                            surahNameAr = sAr,
                            surahNameEn = sEn,
                            revelationPlaceAr = sRev,
                            ayahNumber = vNum,
                            textAr = arText,
                            textEn = enText,
                            tafsirMuyassar = tafsir,
                            juz = juz,
                            hizb = exactHizb,
                            rubInHizb = exactRubInHizb,
                            page = page,
                            isSajda = sajda,
                            normalizedTextAr = QuranTextMatcher.normalizeArabic(arText)
                        )
                        surahVerses.add(verse)
                        loadedVerses.add(verse)
                        pageMap.getOrPut(page) { ArrayList() }.add(verse)
                    }

                    loadedSurahs.add(
                        OfflineSurah(
                            number = sNum,
                            nameAr = sAr,
                            nameEn = sEn,
                            transliteration = sTr,
                            revelationPlaceAr = sRev,
                            versesCount = sCnt,
                            startPage = firstPage,
                            verses = surahVerses
                        )
                    )
                }

                allSurahs = loadedSurahs
                allVersesList = loadedVerses
                versesByPage = pageMap
                isLoaded = true
                val elapsed = System.currentTimeMillis() - startTime
                Log.i(TAG, "Loaded 114 surahs and ${loadedVerses.size} verses with Tafsir Al-Muyassar in ${elapsed}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $ASSET_FILE from assets", e)
        }
    }

    fun getExactPageIfLoaded(surahNumber: Int, ayahNumber: Int): Int? {
        if (!isLoaded) return null
        val surah = allSurahs.getOrNull(surahNumber - 1) ?: return null
        val clamped = ayahNumber.coerceIn(1, surah.versesCount)
        return (surah.verses.getOrNull(clamped - 1) ?: surah.verses.find { it.ayahNumber == clamped })?.page
    }

    fun getVersesForPage(context: Context, page: Int): List<OfflineVerse> {
        ensureLoadedSynchronously(context)
        val clamped = page.coerceIn(1, 604)
        return versesByPage[clamped] ?: emptyList()
    }

    fun getVerse(context: Context, surahNumber: Int, ayahNumber: Int): OfflineVerse? {
        ensureLoadedSynchronously(context)
        val surah = allSurahs.getOrNull(surahNumber - 1) ?: return null
        return surah.verses.find { it.ayahNumber == ayahNumber }
    }

    fun getVerseBySurahName(context: Context, surahNameAr: String, ayahNumber: Int): OfflineVerse? {
        ensureLoadedSynchronously(context)
        val normInput = QuranTextMatcher.normalizeArabic(surahNameAr)
        val surah = allSurahs.find {
            it.nameAr == surahNameAr || QuranTextMatcher.normalizeArabic(it.nameAr) == normInput
        } ?: return null
        return surah.verses.find { it.ayahNumber == ayahNumber } ?: surah.verses.firstOrNull()
    }

    fun getSurahTafsirOverview(context: Context, surahNameAr: String): String {
        ensureLoadedSynchronously(context)
        val normInput = QuranTextMatcher.normalizeArabic(surahNameAr)
        val surah = allSurahs.find {
            it.nameAr == surahNameAr || QuranTextMatcher.normalizeArabic(it.nameAr) == normInput
        }
        if (surah != null) {
            val firstTafsir = surah.verses.firstOrNull()?.tafsirMuyassar.orEmpty()
            if (firstTafsir.isNotBlank()) {
                return "سورة ${surah.nameAr} (${surah.revelationPlaceAr} - ${surah.versesCount} آية): $firstTafsir"
            }
        }
        return "سورة $surahNameAr من سور القرآن الكريم المباركة التي ترسخ الإيمان وتدعو إلى تقوى الله والعمل الصالح."
    }

    fun getSurahAsbabAlNuzul(context: Context, surahNameAr: String): String {
        ensureLoadedSynchronously(context)
        val normInput = QuranTextMatcher.normalizeArabic(surahNameAr)
        val surah = allSurahs.find {
            it.nameAr == surahNameAr || QuranTextMatcher.normalizeArabic(it.nameAr) == normInput
        }
        return when (surah?.number) {
            1 -> "نزلت سورة الفاتحة بمكة المكرمة في أوائل ما نزل من القرآن لتعليم المؤمنين كيفية الثناء على الله ومناجاته في كل ركعة."
            2 -> "نزلت سورة البقرة بالمدينة المنورة لتأسيس المجتمع الإسلامي وتشريع الأحكام والعبادات وتثبيت قلوب المؤمنين."
            3 -> "نزلت مطالع سورة آل عمران في وفد نجران وفي بيان التوحيد الخالص، ونزل شطر منها عقب غزوة أحد لتثبيت المؤمنين."
            18 -> "نزلت سورة الكهف بمكة جواباً لأسئلة قريش عن فتية الكهف وذي القرنين والروح، وفيها عصمة من الفتن."
            36 -> "نزلت سورة يس بمكة المكرمة لتقرير أصول العقيدة من التوحيد والرسالة والبعث والنشور."
            67 -> "نزلت سورة الملك بمكة لبيان عظمة سلطان الله في الخلق وهي المنجية والمانعة من عذاب القبر."
            112 -> "نزلت سورة الإخلاص جواباً لمن سأل النبي ﷺ: انسب لنا ربك، فأنزل الله: قل هو الله أحد."
            113, 114 -> "نزلت المعوذتان (الفلق والناس) تحصيناً للنبي ﷺ وللمؤمنين من كل شر وحسد."
            else -> if (surah != null) {
                "سورة ${surah.nameAr} سورة ${surah.revelationPlaceAr} وعدد آياتها ${surah.versesCount} آية، تنزلت هدايةً ورحمةً وبياناً لأحكام الله ومواعظه للعباد."
            } else {
                "تنزلت آيات سورة ($surahNameAr) هدايةً ورحمةً وتثبيتاً لقلوب المؤمنين."
            }
        }
    }

    fun getAllVerses(context: Context): List<OfflineVerse> {
        ensureLoadedSynchronously(context)
        return allVersesList
    }

    fun getAllSurahs(context: Context): List<OfflineSurah> {
        ensureLoadedSynchronously(context)
        return allSurahs
    }

    fun getVersesForSurah(context: Context, surahNumber: Int): List<OfflineVerse> {
        ensureLoadedSynchronously(context)
        return allSurahs.getOrNull(surahNumber - 1)?.verses ?: emptyList()
    }

    fun searchVerses(context: Context, query: String, limit: Int = 30): List<OfflineVerse> {
        ensureLoadedSynchronously(context)
        val normQuery = QuranTextMatcher.normalizeArabic(query)
        if (normQuery.length < 2) return emptyList()

        val results = ArrayList<OfflineVerse>()
        for (v in allVersesList) {
            if (v.normalizedTextAr.contains(normQuery) || v.surahNameAr.contains(query.trim())) {
                results.add(v)
                if (results.size >= limit) break
            }
        }
        return results
    }
}
