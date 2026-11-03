package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import com.example.data.OfflineQuranDatabase
import com.example.data.OfflineVerse
import com.example.data.QuranDataHelper
import com.example.data.QuranTextMatcher
import com.example.data.SurahInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "MushafCameraRecognizer"

data class CameraRecognitionResult(
    val surahName: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val recognizedVerseSnippet: String,
    val confidenceNote: String
)

/**
 * 100% Firebase-free Mushaf Page & Ayah Recognizer.
 * Uses a multi-engine Arabic/Auto OCR cascade (OCREngine 2 & 3 with language=auto) over HTTPS
 * paired with local 6,236-verse Uthmani/normalized text matching, Surah banner detection,
 * and printed page number (1..604) extraction.
 */
object MushafCameraRecognizer {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val ocrApiKeys = listOf(
        "helloworld",
        "K81928388988957"
    )

    private data class OcrEngineSpec(
        val engine: String,
        val language: String?
    )

    // OCREngine 2 and 3 natively support Arabic calligraphy via language="auto" without E201 error
    private val engineSpecs = listOf(
        OcrEngineSpec(engine = "2", language = "auto"),
        OcrEngineSpec(engine = "3", language = "auto"),
        OcrEngineSpec(engine = "2", language = null),
        OcrEngineSpec(engine = "1", language = "eng")
    )

    suspend fun recognizeMushafPage(
        context: Context,
        bitmap: Bitmap
    ): Result<CameraRecognitionResult> = withContext(Dispatchers.IO) {
        try {
            OfflineQuranDatabase.ensureLoaded(context)

            val preparedBitmap = preprocessMushafBitmap(bitmap, maxDimension = 1350)
            val jpegBytes = compressBitmapToJpegUnderLimit(preparedBitmap, maxBytes = 820 * 1024)

            val rawOcrText = performIndependentArabicOcr(jpegBytes)
            Log.d(TAG, "Independent OCR extracted text (${rawOcrText.length} chars): ${rawOcrText.take(200)}")

            if (rawOcrText.isBlank()) {
                return@withContext Result.failure(
                    Exception("لم يتم العثور على نص واضح في الصورة. يرجى تقريب الكاميرا من صفحة المصحف في إضاءة جيدة.")
                )
            }

            val matchResult = analyzeOcrTextAgainstQuranDatabase(context, rawOcrText)
            if (matchResult != null) {
                return@withContext Result.success(matchResult)
            }

            Result.failure(
                Exception("تعذر مطابقة النص المستخرج من الصورة مع صفحات المصحف. حاول تصوير الصفحة بشكل عمودي واضح أو اكتب رقم الصفحة يدوياً.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Independent Mushaf camera recognition failed", e)
            Result.failure(
                Exception("تعذر قراءة صورة المصحف (${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}). يمكنك أيضاً كتابة رقم الصفحة أو لصق جزء من الآية.")
            )
        }
    }

    private fun performIndependentArabicOcr(jpegBytes: ByteArray): String {
        var lastError: Exception? = null
        val base64DataUri = "data:image/jpeg;base64," + Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val collectedSnippets = StringBuilder()

        for (spec in engineSpecs) {
            for (apiKey in ocrApiKeys) {
                try {
                    val formBuilder = FormBody.Builder()
                        .add("apikey", apiKey)
                        .add("base64Image", base64DataUri)
                        .add("isOverlayRequired", "false")
                        .add("scale", "true")
                        .add("detectOrientation", "true")
                        .add("OCREngine", spec.engine)

                    if (!spec.language.isNullOrBlank()) {
                        formBuilder.add("language", spec.language)
                    }

                    val request = Request.Builder()
                        .url("https://api.ocr.space/parse/image")
                        .header("apikey", apiKey)
                        .post(formBuilder.build())
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        if (!response.isSuccessful || bodyStr.isBlank()) {
                            throw IllegalStateException("HTTP ${response.code}")
                        }

                        val json = JSONObject(bodyStr)
                        val isErrored = json.optBoolean("IsErroredOnProcessing", false)
                        val parsedResults = json.optJSONArray("ParsedResults")
                        if (parsedResults != null && parsedResults.length() > 0) {
                            val sb = StringBuilder()
                            for (i in 0 until parsedResults.length()) {
                                val item = parsedResults.optJSONObject(i)
                                val text = item?.optString("ParsedText", "").orEmpty()
                                if (text.isNotBlank()) {
                                    sb.append(text).append("\n")
                                }
                            }
                            val combined = sb.toString().trim()
                            if (combined.isNotBlank()) {
                                // Check if we got Arabic letters or valid page digits
                                val hasArabicChars = combined.any { it in '\u0600'..'\u06FF' }
                                if (hasArabicChars) {
                                    return combined
                                } else {
                                    collectedSnippets.append(combined).append("\n")
                                }
                            }
                        }

                        if (isErrored) {
                            val errArr = json.optJSONArray("ErrorMessage")
                            val errMsg = errArr?.optString(0) ?: json.optString("ErrorMessage", "OCR processing error")
                            throw IllegalStateException(errMsg)
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "OCR attempt (engine=${spec.engine}, lang=${spec.language}) failed: ${e.message}")
                }
            }
        }

        val fallbackText = collectedSnippets.toString().trim()
        if (fallbackText.isNotBlank()) {
            return fallbackText
        }

        if (lastError != null) {
            throw lastError
        }
        return ""
    }

    /**
     * Multi-stage Quran page & verse analyzer using the local 6,236-verse database:
     * 1. Direct or line-by-line verse match via [QuranTextMatcher].
     * 2. Page-level weighted word & character N-gram scoring across all 604 pages.
     * 3. Printed page number extraction (1..604 in Arabic or Western digits) cross-checked with detected Surah/words.
     * 4. Surah header detection ("سورة ...").
     */
    internal fun analyzeOcrTextAgainstQuranDatabase(
        context: Context,
        rawOcrText: String
    ): CameraRecognitionResult? {
        val cleanedText = rawOcrText.trim()
        if (cleanedText.isEmpty()) return null

        val lines = cleanedText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Extract candidate printed page numbers (especially from top 2 and bottom 2 lines of the page)
        val marginLines = (lines.take(2) + lines.takeLast(2)).distinct()
        val marginPageCandidate = extractPageNumberCandidate(marginLines.joinToString(" "))
        val anyPageCandidate = marginPageCandidate ?: extractPageNumberCandidate(cleanedText)

        // Extract any explicit Surah name mentioned in the OCR text (e.g. top margin or Surah banner)
        val detectedSurah = detectSurahInOcrText(cleanedText)

        // Stage 1: Score all 604 pages in OfflineQuranDatabase using normalized word overlap
        val allVerses = OfflineQuranDatabase.getAllVerses(context)
        val normalizedFullOcr = QuranTextMatcher.normalizeArabic(cleanedText)
        val ocrWords = normalizedFullOcr
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }

        var bestVerseByScoring: OfflineVerse? = null
        var bestVerseScore = 0
        val pageScores = IntArray(605)

        if (ocrWords.isNotEmpty()) {
            val ocrWordSet = ocrWords.toSet()
            for (verse in allVerses) {
                var vScore = 0
                val verseWords = verse.normalizedTextAr.split(" ")
                for (vw in verseWords) {
                    if (vw.length >= 3 && ocrWordSet.contains(vw)) {
                        vScore += if (vw.length >= 5) 3 else 2
                    }
                }
                // Bonus if verse belongs to detected Surah or candidate page
                if (detectedSurah != null && verse.surahNumber == detectedSurah.number) {
                    vScore += 2
                }
                if (anyPageCandidate != null && verse.page == anyPageCandidate) {
                    vScore += 4
                }

                if (vScore > 0 && verse.page in 1..604) {
                    pageScores[verse.page] += vScore
                }
                if (vScore > bestVerseScore) {
                    bestVerseScore = vScore
                    bestVerseByScoring = verse
                }
            }
        }

        // Find the page (1..604) with the highest aggregate score
        var bestPage = 0
        var maxPageScore = 0
        for (p in 1..604) {
            if (pageScores[p] > maxPageScore) {
                maxPageScore = pageScores[p]
                bestPage = p
            }
        }

        // Stage 2: Check if a specific line in OCR has a strong direct match in QuranTextMatcher
        for (line in lines.sortedByDescending { it.length }) {
            if (line.length >= 10) {
                val lineMatch = QuranTextMatcher.findAyahMatch(context, line)
                if (lineMatch != null) {
                    val surahInfo = QuranDataHelper.getSurahByName(lineMatch.surahName)
                    val verifiedVerse = OfflineQuranDatabase.getVerse(context, surahInfo.number, lineMatch.ayahNumber)
                    val resolvedPage = verifiedVerse?.page ?: lineMatch.pageNumber
                    // Check if user's OCR text also contains a specific Ayah number on that same page
                    val pageVerses = OfflineQuranDatabase.getVersesForPage(context, resolvedPage)
                    val refinedVerse = pickBestVerseOnPage(pageVerses, normalizedFullOcr, cleanedText)
                        ?: verifiedVerse

                    return CameraRecognitionResult(
                        surahName = refinedVerse?.surahNameAr ?: surahInfo.nameArabic,
                        surahNumber = refinedVerse?.surahNumber ?: surahInfo.number,
                        ayahNumber = refinedVerse?.ayahNumber ?: lineMatch.ayahNumber,
                        pageNumber = resolvedPage,
                        recognizedVerseSnippet = refinedVerse?.textAr ?: line,
                        confidenceNote = "تم التعرف البصري ومطابقة الآيات محلياً بنجاح"
                    )
                }
            }
        }

        // Stage 3: Use highest-scoring page from full 6,236-verse word overlap
        if (bestPage in 1..604 && maxPageScore >= 5) {
            val pageVerses = OfflineQuranDatabase.getVersesForPage(context, bestPage)
            val chosenVerse = pickBestVerseOnPage(pageVerses, normalizedFullOcr, cleanedText)
                ?: bestVerseByScoring
                ?: pageVerses.firstOrNull()

            if (chosenVerse != null) {
                return CameraRecognitionResult(
                    surahName = chosenVerse.surahNameAr,
                    surahNumber = chosenVerse.surahNumber,
                    ayahNumber = chosenVerse.ayahNumber,
                    pageNumber = chosenVerse.page,
                    recognizedVerseSnippet = chosenVerse.textAr,
                    confidenceNote = "تم التعرف على صفحة ${chosenVerse.page} (سورة ${chosenVerse.surahNameAr}) ومطابقة الآيات"
                )
            }
        }

        // Stage 4: Printed Page Number (1..604) detected in margin
        if (anyPageCandidate != null && anyPageCandidate in 1..604) {
            val pageVerses = OfflineQuranDatabase.getVersesForPage(context, anyPageCandidate)
            if (pageVerses.isNotEmpty()) {
                val chosenVerse = pickBestVerseOnPage(pageVerses, normalizedFullOcr, cleanedText)
                    ?: pageVerses.first()
                return CameraRecognitionResult(
                    surahName = chosenVerse.surahNameAr,
                    surahNumber = chosenVerse.surahNumber,
                    ayahNumber = chosenVerse.ayahNumber,
                    pageNumber = chosenVerse.page,
                    recognizedVerseSnippet = chosenVerse.textAr,
                    confidenceNote = "تم قراءة رقم الصفحة (${chosenVerse.page}) وتحديد سورة ${chosenVerse.surahNameAr}"
                )
            }
        }

        // Stage 5: Surah Header detected ("سورة ...") + optional Ayah number
        if (detectedSurah != null) {
            val asciiDigits = QuranTextMatcher.convertArabicDigitsToAscii(cleanedText)
            val ayahCandidate = Regex("\\b(\\d{1,3})\\b").findAll(asciiDigits)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .firstOrNull { it in 1..detectedSurah.totalAyahs } ?: 1

            val verifiedVerse = OfflineQuranDatabase.getVerse(context, detectedSurah.number, ayahCandidate)
            val resolvedPage = verifiedVerse?.page
                ?: QuranDataHelper.getPageForSurahAndAyah(detectedSurah.number, ayahCandidate)

            return CameraRecognitionResult(
                surahName = detectedSurah.nameArabic,
                surahNumber = detectedSurah.number,
                ayahNumber = ayahCandidate,
                pageNumber = resolvedPage,
                recognizedVerseSnippet = verifiedVerse?.textAr ?: "سورة ${detectedSurah.nameArabic}",
                confidenceNote = "تم التعرف على سورة ${detectedSurah.nameArabic} (صفحة $resolvedPage)"
            )
        }

        return null
    }

    private fun pickBestVerseOnPage(
        pageVerses: List<OfflineVerse>,
        normalizedOcr: String,
        rawOcrText: String
    ): OfflineVerse? {
        if (pageVerses.isEmpty()) return null
        val ocrWords = normalizedOcr.split(" ").filter { it.length >= 3 }.toSet()

        // Also check if any verse number on this page appears explicitly in the OCR digits
        val asciiText = QuranTextMatcher.convertArabicDigitsToAscii(rawOcrText)
        val detectedNumbers = Regex("\\b(\\d{1,3})\\b").findAll(asciiText)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toSet()

        var bestVerse = pageVerses.first()
        var bestScore = -1

        for (v in pageVerses) {
            var score = 0
            for (w in v.normalizedTextAr.split(" ")) {
                if (w.length >= 3 && ocrWords.contains(w)) {
                    score += 3
                }
            }
            if (detectedNumbers.contains(v.ayahNumber)) {
                score += 4
            }
            if (score > bestScore) {
                bestScore = score
                bestVerse = v
            }
        }
        return bestVerse
    }

    private fun extractPageNumberCandidate(text: String): Int? {
        val ascii = QuranTextMatcher.convertArabicDigitsToAscii(text)
        val candidates = Regex("(?<!\\d)(\\d{1,3})(?!\\d)")
            .findAll(ascii)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 1..604 }
            .toList()
        return candidates.lastOrNull() ?: candidates.firstOrNull()
    }

    private fun detectSurahInOcrText(text: String): SurahInfo? {
        val norm = QuranTextMatcher.normalizeArabic(text)
        for (surah in QuranDataHelper.surahs.sortedByDescending { it.nameArabic.length }) {
            val normName = QuranTextMatcher.normalizeArabic(surah.nameArabic)
            if (norm.contains("سوره $normName") || norm.contains(normName)) {
                return surah
            }
        }
        return null
    }

    private fun preprocessMushafBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val ratio = originalWidth.toFloat() / originalHeight.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            targetWidth = originalWidth
            targetHeight = originalHeight
        } else if (originalWidth > originalHeight) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }

        val scaled = if (targetWidth == originalWidth && targetHeight == originalHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        // Enhance contrast slightly for crisp Arabic calligraphy OCR
        val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val contrast = 1.18f
        val brightness = -10f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }

    private fun compressBitmapToJpegUnderLimit(bitmap: Bitmap, maxBytes: Int): ByteArray {
        var quality = 88
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        while (baos.size() > maxBytes && quality > 45) {
            baos.reset()
            quality -= 12
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }
        return baos.toByteArray()
    }
}
