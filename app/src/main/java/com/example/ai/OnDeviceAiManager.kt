package com.example.ai

import android.content.Context
import com.example.data.OfflineQuranDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnDeviceAiManager(private val context: Context) {

    suspend fun generateQuranicReflection(
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int,
        userName: String = "القارئ المبارك"
    ): String = withContext(Dispatchers.Default) {
        return@withContext generateSynthesizedReflection(surahName, ayahNumber, pageNumber)
    }

    private fun generateSynthesizedReflection(
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int
    ): String {
        val offlineVerse = try {
            OfflineQuranDatabase.getVerseBySurahName(context, surahName, ayahNumber)
        } catch (e: Exception) {
            null
        }

        val tafsirText = if (offlineVerse != null && offlineVerse.tafsirMuyassar.isNotBlank()) {
            "قال تعالى: ﴿ ${offlineVerse.textAr} ﴾ [سورة ${offlineVerse.surahNameAr}: ${offlineVerse.ayahNumber} - صفحة $pageNumber]\n\nالتفسير الميسر:\n${offlineVerse.tafsirMuyassar}"
        } else {
            OfflineQuranDatabase.getSurahTafsirOverview(context, surahName)
        }

        val asbabText = OfflineQuranDatabase.getSurahAsbabAlNuzul(context, surahName)
        val duaText = com.example.data.IslamicDuaHelper.getRandomDua()

        return """
            1. التفسير الجامع والمختصر:
            $tafsirText
            
            2. أسباب النزول:
            $asbabText
            
            3. دعاء:
            $duaText
        """.trimIndent()
    }
}
