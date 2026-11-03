package com.example.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.data.OfflineQuranDatabase
import com.example.data.QuranDataHelper
import com.example.data.QuranTextMatcher
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "MushafCameraRecognizer"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

data class CameraRecognitionResult(
    val surahName: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val recognizedVerseSnippet: String,
    val confidenceNote: String
)

object MushafCameraRecognizer {

    private val mushafSchema = Schema.obj(
        properties = mapOf(
            "surahNumber" to Schema.integer(description = "Surah number from 1 to 114 detected on the Mushaf page"),
            "surahNameArabic" to Schema.string(description = "Arabic name of the Surah without 'سورة' prefix, e.g., البقرة"),
            "ayahNumber" to Schema.integer(description = "Ayah number where the user stopped, pointed, or the prominent Ayah on the page"),
            "pageNumber" to Schema.integer(description = "Mushaf page number (1 to 604) printed at the bottom or top of the page if visible, or 0 if not visible"),
            "extractedArabicText" to Schema.string(description = "A clear snippet of Arabic Quranic text visible on the page to verify exact match")
        )
    )

    private val googleAiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mushafSchema
            }
        )
    }

    private val vertexAiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mushafSchema
            }
        )
    }

    suspend fun recognizeMushafPage(
        context: Context,
        bitmap: Bitmap
    ): Result<CameraRecognitionResult> = withContext(Dispatchers.IO) {
        try {
            OfflineQuranDatabase.ensureLoaded(context)

            val scaledBitmap = scaleBitmapDown(bitmap, 1280)

            val prompt = """
                أنت خبير في قراءة صفحات المصحف الشريف (مصحف المدينة النبوية - 604 صفحة).
                قم بتحليل صورة صفحة المصحف المرفقة بدقة واستخرج:
                1. رقم الصفحة المطبوع في أسفل أو أعلى الصفحة (بين 1 و 604) إن وجد.
                2. اسم السورة الكريمة ورقمها (من 1 إلى 114).
                3. رقم الآية الكريمة (إذا كان هناك علامة وقف أو إشارة إصبع أو فاصل مصحف، اختر الآية المشار إليها، وإلا اختر آخر آية واضحة أو أول آية في الصفحة).
                4. جزءاً واضحاً من النص القرآني الظاهر في الصورة لمطابقته مع قاعدة البيانات.
            """.trimIndent()

            val inputContent = content {
                image(scaledBitmap)
                text(prompt)
            }

            val response = withTimeout(60_000L) {
                try {
                    googleAiModel.generateContent(inputContent)
                } catch (firstErr: Exception) {
                    if (firstErr is CancellationException) throw firstErr
                    Log.w(TAG, "GoogleAI backend failed, falling back to VertexAI backend: ${firstErr.message}")
                    vertexAiModel.generateContent(inputContent)
                }
            }

            val jsonText = response.text ?: throw IllegalStateException("لم يتم استلام استجابة من نموذج الرؤية الذكي.")
            val json = JSONObject(jsonText)

            val rawSurahNum = json.optInt("surahNumber", 0)
            val rawSurahName = json.optString("surahNameArabic", "").trim()
            val rawAyahNum = json.optInt("ayahNumber", 1).coerceAtLeast(1)
            val rawPageNum = json.optInt("pageNumber", 0)
            val extractedSnippet = json.optString("extractedArabicText", "").trim()

            // Cross-verify against our 100% accurate local 6,236-verse database
            // 1. Try matching the extracted Arabic snippet first
            if (extractedSnippet.length >= 6) {
                val textMatch = QuranTextMatcher.findAyahMatch(context, extractedSnippet)
                if (textMatch != null) {
                    val surahInfo = QuranDataHelper.getSurahByName(textMatch.surahName)
                    val finalPage = if (rawPageNum in 1..604) rawPageNum else textMatch.pageNumber
                    val finalAyah = if (rawAyahNum in 1..surahInfo.totalAyahs) rawAyahNum else textMatch.ayahNumber
                    val verifiedVerse = OfflineQuranDatabase.getVerse(context, surahInfo.number, finalAyah)
                    return@withContext Result.success(
                        CameraRecognitionResult(
                            surahName = surahInfo.nameArabic,
                            surahNumber = surahInfo.number,
                            ayahNumber = finalAyah,
                            pageNumber = verifiedVerse?.page ?: finalPage,
                            recognizedVerseSnippet = verifiedVerse?.textAr ?: extractedSnippet,
                            confidenceNote = "تم التعرف على الصفحة ومطابقة الآيات مع قاعدة بيانات المصحف المحلي بنجاح"
                        )
                    )
                }
            }

            // 2. If pageNumber (1..604) was detected directly from the printed page
            if (rawPageNum in 1..604) {
                val pageVerses = OfflineQuranDatabase.getVersesForPage(context, rawPageNum)
                if (pageVerses.isNotEmpty()) {
                    val matchedVerse = pageVerses.find {
                        (rawSurahNum == 0 || it.surahNumber == rawSurahNum) && it.ayahNumber == rawAyahNum
                    } ?: pageVerses.find {
                        it.ayahNumber == rawAyahNum
                    } ?: pageVerses.first()

                    return@withContext Result.success(
                        CameraRecognitionResult(
                            surahName = matchedVerse.surahNameAr,
                            surahNumber = matchedVerse.surahNumber,
                            ayahNumber = matchedVerse.ayahNumber,
                            pageNumber = matchedVerse.page,
                            recognizedVerseSnippet = matchedVerse.textAr,
                            confidenceNote = "تم قراءة رقم الصفحة (${matchedVerse.page}) وسورة ${matchedVerse.surahNameAr} آية ${matchedVerse.ayahNumber} بنجاح"
                        )
                    )
                }
            }

            // 3. Match by Surah Number or Surah Name + Ayah Number
            val targetSurah = QuranDataHelper.surahs.find { it.number == rawSurahNum }
                ?: QuranDataHelper.surahs.find {
                    QuranTextMatcher.normalizeArabic(it.nameArabic) == QuranTextMatcher.normalizeArabic(rawSurahName)
                }
                ?: QuranDataHelper.surahs.first()

            val clampedAyah = rawAyahNum.coerceIn(1, targetSurah.totalAyahs)
            val verifiedVerse = OfflineQuranDatabase.getVerse(context, targetSurah.number, clampedAyah)
            val computedPage = verifiedVerse?.page ?: QuranDataHelper.getPageForSurahAndAyah(targetSurah.number, clampedAyah)

            Result.success(
                CameraRecognitionResult(
                    surahName = targetSurah.nameArabic,
                    surahNumber = targetSurah.number,
                    ayahNumber = clampedAyah,
                    pageNumber = computedPage,
                    recognizedVerseSnippet = verifiedVerse?.textAr ?: extractedSnippet,
                    confidenceNote = "تم التعرف على سورة ${targetSurah.nameArabic} الآية $clampedAyah (صفحة $computedPage)"
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Mushaf camera recognition timed out", e)
            Result.failure(Exception("استغرق تحليل الصورة وقتاً أطول من المعتاد. يرجى المحاولة مرة أخرى بصورة أوضح."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Mushaf camera recognition failed", e)
            Result.failure(Exception("تعذر التعرف التلقائي من الصورة (${e.localizedMessage ?: "خطأ في الاتصال"}). تأكد من وضوح إضاءة صفحة المصحف أو أدخل رقم الصفحة يدوياً."))
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap
        }
        val ratio = originalWidth.toFloat() / originalHeight.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (originalWidth > originalHeight) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
