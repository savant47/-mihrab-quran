package com.example.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.data.QuranDataHelper
import com.example.data.SurahInfo
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class QuranRepository(private val context: Context) {

    companion object {
        private const val TAG = "QuranRepository"
        private const val ASSET_QURAN_METADATA = "quran_metadata.json"
    }

    private val assetManager: AssetManager = context.assets

    /**
     * Reads Quran metadata safely from app's assets folder using AssetManager.
     * Implements a robust try-catch mechanism with automatic fallback to builtin data helper.
     */
    fun loadSurahMetadataFromAssets(): List<SurahInfo> {
        val loadedSurahs = mutableListOf<SurahInfo>()
        try {
            assetManager.open(ASSET_QURAN_METADATA).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonContent = reader.readText()
                val rootObject = JSONObject(jsonContent)
                val surahsArray = rootObject.optJSONArray("surahs")

                if (surahsArray != null) {
                    for (i in 0 until surahsArray.length()) {
                        val obj = surahsArray.getJSONObject(i)
                        val surah = SurahInfo(
                            number = obj.optInt("number", 1),
                            nameArabic = obj.optString("nameArabic", ""),
                            totalAyahs = obj.optInt("numberOfAyahs", 7),
                            startPage = obj.optInt("startPage", 1)
                        )
                        loadedSurahs.add(surah)
                    }
                }
            }
            Log.i(TAG, "Successfully loaded ${loadedSurahs.size} surahs from assets ($ASSET_QURAN_METADATA).")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading $ASSET_QURAN_METADATA from AssetManager, falling back to QuranDataHelper", e)
        }

        return if (loadedSurahs.isNotEmpty()) loadedSurahs else QuranDataHelper.surahs
    }

    /**
     * Reads specific surah tafsir summary safely from assets with fallback.
     */
    fun getSurahTafsirFromAssets(surahName: String): String {
        try {
            assetManager.open(ASSET_QURAN_METADATA).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val rootObject = JSONObject(reader.readText())
                val surahsArray = rootObject.optJSONArray("surahs")
                if (surahsArray != null) {
                    for (i in 0 until surahsArray.length()) {
                        val obj = surahsArray.getJSONObject(i)
                        if (obj.optString("nameArabic") == surahName) {
                            val tafsir = obj.optString("tafsir")
                            if (tafsir.isNotBlank()) return tafsir
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tafsir for $surahName from assets", e)
        }
        return "تضمنت القراءة المباركة لسورة ($surahName) توجيهات إيمانية سامية وحثاً على التدبر والاستمساك بالقرآن الكريم."
    }

    /**
     * Reads specific surah Asbab Al-Nuzul safely from assets with fallback.
     */
    fun getAsbabNuzulFromAssets(surahName: String): String {
        try {
            assetManager.open(ASSET_QURAN_METADATA).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val rootObject = JSONObject(reader.readText())
                val surahsArray = rootObject.optJSONArray("surahs")
                if (surahsArray != null) {
                    for (i in 0 until surahsArray.length()) {
                        val obj = surahsArray.getJSONObject(i)
                        if (obj.optString("nameArabic") == surahName) {
                            val asbab = obj.optString("asbabNuzul")
                            if (asbab.isNotBlank()) return asbab
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Asbab Nuzul for $surahName from assets", e)
        }
        return "ورد في أسباب نزول آيات سورة ($surahName) أنها تنزلت هداية ورحمة وتثبيتاً لقلوب المؤمنين."
    }
}
