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

    fun getAllVerses(context: Context): List<OfflineVerse> {
        ensureLoadedSynchronously(context)
        return allVersesList
    }

    fun getAllSurahs(context: Context): List<OfflineSurah> {
        ensureLoadedSynchronously(context)
        return allSurahs
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
