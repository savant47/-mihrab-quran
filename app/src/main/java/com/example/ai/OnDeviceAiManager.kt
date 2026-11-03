package com.example.ai

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.data.OfflineQuranDatabase
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OnDeviceAiManager(private val context: Context) {

    private val assetManager: AssetManager = context.assets

    private var llmInference: LlmInference? = null
    private var isModelLoaded = false

    companion object {
        private const val TAG = "OnDeviceAiManager"
        private const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"
    }

    init {
        initializeLocalModelFromAssets()
    }

    /**
     * Attempts to locate and load the AI model safely from app assets or files directory
     * using AssetManager with a robust try-catch mechanism.
     */
    private fun initializeLocalModelFromAssets() {
        try {
            val targetModelFile = File(context.filesDir, MODEL_FILENAME)

            // Check if model file needs to be copied from assets folder using AssetManager
            if (!targetModelFile.exists()) {
                val assetList = assetManager.list("") ?: emptyArray()
                if (assetList.contains(MODEL_FILENAME)) {
                    Log.i(TAG, "Copying $MODEL_FILENAME from assets directory via AssetManager...")
                    copyAssetToFile(MODEL_FILENAME, targetModelFile)
                }
            }

            if (targetModelFile.exists() && targetModelFile.length() > 0) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(targetModelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setResultListener { partialResult, done ->
                        Log.d(TAG, "MediaPipe token: $partialResult, done: $done")
                    }
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                isModelLoaded = true
                Log.i(TAG, "MediaPipe GenAI On-Device model initialized successfully from assets/files.")
            } else {
                Log.i(TAG, "AI model file not present in assets. Utilizing fast offline knowledge engine.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AssetManager AI model initialization. Falling back safely.", e)
            isModelLoaded = false
        }
    }

    /**
     * Safely copies an asset file to internal storage with robust stream exception handling.
     */
    private fun copyAssetToFile(assetName: String, destFile: File) {
        try {
            assetManager.open(assetName).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            Log.i(TAG, "Successfully copied asset $assetName to ${destFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying asset $assetName via AssetManager", e)
            if (destFile.exists()) {
                destFile.delete()
            }
        }
    }

    suspend fun generateQuranicReflection(
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int,
        userName: String = "القارئ المبارك"
    ): String = withContext(Dispatchers.Default) {
        if (isModelLoaded && llmInference != null) {
            try {
                val prompt = buildMasterPrompt(surahName, ayahNumber, pageNumber, userName)
                val response = llmInference?.generateResponse(prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response
                }
            } catch (e: Exception) {
                Log.e(TAG, "On-device LLM generation error, utilizing offline knowledge engine from assets", e)
            }
        }

        return@withContext generateSynthesizedReflection(surahName, ayahNumber, pageNumber)
    }

    private fun buildMasterPrompt(
        surahName: String,
        ayahNumber: Int,
        pageNumber: Int,
        userName: String
    ): String {
        return """
            You are "محرابي" (Mihrabi), an intelligent offline Islamic Quran companion and spiritual mentor.
            User read: Surah $surahName, Ayah $ayahNumber, Page $pageNumber.
            
            Strict Rules:
            1. Quran domain strictly ONLY (NO food, calories, health measurements).
            2. Output 3 clear sections in eloquent Modern Standard Arabic:
               - Section 1: التفسير الجامع والمختصر (Core themes & concise explanation)
               - Section 2: أسباب النزول (Authentic occasion of revelation if established)
               - Section 3: دعاء (Heartfelt supplication)
        """.trimIndent()
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
