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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

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
 * 100% Firebase-free Mushaf Page, Surah & First-Ayah Recognizer.
 * Uses multi-engine Arabic OCR (OCREngine 2 & 3 with language=auto) paired with:
 * - Full 604-page Uthmani + Imlai skeleton scoring across all 6,236 Quran verses
 * - Fuzzy Surah header/banner detection ("سورة الكهف" even when OCR reads "سورة الكفف")
 * - Automatic determination of the Page number, Surah name, and the First Ayah on the page.
 */
object MushafCameraRecognizer {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    // Prioritize dedicated key first to avoid demo key rate-limiting (HTTP 429)
    private val ocrApiKeys = listOf(
        "K81928388988957",
        "helloworld"
    )

    private data class OcrEngineSpec(
        val engine: String,
        val language: String?
    )

    // OCREngine 2 & 3 natively recognize Arabic calligraphy & Arabic-Indic digits via language="auto"
    private val engineSpecs = listOf(
        OcrEngineSpec(engine = "2", language = "auto"),
        OcrEngineSpec(engine = "3", language = "auto"),
        OcrEngineSpec(engine = "2", language = null)
    )

    private val commonBasmalahWords = setOf(
        "بسم", "الله", "الرحمن", "الرحيم"
    )

    suspend fun recognizeMushafPage(
        context: Context,
        bitmap: Bitmap
    ): Result<CameraRecognitionResult> = withContext(Dispatchers.IO) {
        try {
            OfflineQuranDatabase.ensureLoaded(context)

            val preparedBitmap = preprocessMushafBitmap(bitmap, maxDimension = 1400)
            val jpegBytes = compressBitmapToJpegUnderLimit(preparedBitmap, maxBytes = 850 * 1024)

            val rawOcrText = performIndependentArabicOcr(jpegBytes)
            Log.d(TAG, "Independent OCR extracted text (${rawOcrText.length} chars): ${rawOcrText.take(300)}")

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
                Exception("تعذر مطابقة النص المستخرج من الصورة مع صفحات المصحف. حاول تصوير الصفحة كاملة بشكل واضح أو اكتب رقم الصفحة يدوياً.")
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
                                val arabicCharCount = combined.count { it in '\u0600'..'\u06FF' }
                                if (arabicCharCount >= 8) {
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
     * Full-page Uthmani + Skeleton Quran Page & First-Ayah Analyzer:
     * 1. Detects any explicit or fuzzy Surah banner ("سورة الكهف", "سورة الإسراء", etc.).
     * 2. Scores all 604 pages across all 6,236 verses using both exact normalized words and
     *    Alef-agnostic Uthmani skeletons (so Uthmani "الكتب / سبحن / عظما / رفتا" matches OCR "الكتاب / سبحان / عظاما / رفاتا").
     * 3. Automatically selects the matched Page, the Surah, and the FIRST Ayah on that page
     *    (or the first Ayah of the newly starting Surah if a Surah header banner appears on that page).
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

        // 1. Detect explicit or fuzzy Surah Header Banner ("سورة ...")
        val detectedSurahBanner = detectSurahBannerFuzzy(cleanedText)

        // 2. Normalize full OCR text and build word + Uthmani skeleton lookup sets
        val normalizedFullOcr = QuranTextMatcher.normalizeArabic(cleanedText)
        val ocrWordsList = normalizedFullOcr
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 }

        // Exclude standalone Basmalah words from dominating Page 1 (Al-Fatiha) when a new Surah starts on another page
        val nonBasmalahWords = ocrWordsList.filter { it !in commonBasmalahWords }
        val activeOcrWords = if (nonBasmalahWords.size >= 4) nonBasmalahWords else ocrWordsList

        val ocrWordSet = activeOcrWords.toSet()
        val ocrSkeletonSet = activeOcrWords
            .map { QuranTextMatcher.toUthmaniSkeleton(it) }
            .filter { it.length >= 3 }
            .toSet()
        val compactOcrSkeletonStream = activeOcrWords.joinToString("") { QuranTextMatcher.toUthmaniSkeleton(it) }

        val allVerses = OfflineQuranDatabase.getAllVerses(context)
        val pageScores = IntArray(605)
        val verseScores = HashMap<String, Int>(512)

        if (activeOcrWords.isNotEmpty()) {
            for (verse in allVerses) {
                var vScore = 0
                val verseWords = verse.normalizedTextAr.split(" ")
                var matchedDistinctWords = 0

                for (vw in verseWords) {
                    if (vw.length < 3) continue
                    // Skip common Basmalah words on Page 1 unless the whole OCR text is only Al-Fatiha
                    if (verse.page == 1 && vw in commonBasmalahWords && activeOcrWords.size > 10) {
                        continue
                    }

                    val vSkel = QuranTextMatcher.toUthmaniSkeleton(vw)
                    when {
                        ocrWordSet.contains(vw) -> {
                            vScore += if (vw.length >= 5) 6 else 4
                            matchedDistinctWords++
                        }
                        vSkel.length >= 3 && ocrSkeletonSet.contains(vSkel) -> {
                            vScore += if (vSkel.length >= 4) 5 else 3
                            matchedDistinctWords++
                        }
                        vSkel.length >= 5 && compactOcrSkeletonStream.contains(vSkel) -> {
                            // Handles joined OCR words like "كلماخبت", "خلقاجديدا", "منقبله", "اياماتدعوا"
                            vScore += 4
                            matchedDistinctWords++
                        }
                    }
                }

                if (matchedDistinctWords >= 3) {
                    vScore += matchedDistinctWords * 3
                }

                if (vScore > 0 && verse.page in 1..604) {
                    pageScores[verse.page] += vScore
                    verseScores["${verse.surahNumber}:${verse.ayahNumber}"] = vScore
                }
            }
        }

        // Apply bonus for detected Surah banner ("سورة ...")
        if (detectedSurahBanner != null) {
            val startPage = QuranDataHelper.getPageForSurahAndAyah(detectedSurahBanner.number, 1)
            if (startPage in 1..604) {
                pageScores[startPage] += 32
            }
            for (verse in allVerses) {
                if (verse.surahNumber == detectedSurahBanner.number && verse.page in 1..604) {
                    pageScores[verse.page] += 3
                }
            }
        }

        // Find the highest-scoring page across all 604 Mushaf pages
        var bestPage = 0
        var maxPageScore = 0
        for (p in 1..604) {
            if (pageScores[p] > maxPageScore) {
                maxPageScore = pageScores[p]
                bestPage = p
            }
        }

        if (bestPage in 1..604 && maxPageScore >= 12) {
            val pageVerses = OfflineQuranDatabase.getVersesForPage(context, bestPage)
            if (pageVerses.isNotEmpty()) {
                val chosenFirstVerse = selectPageFirstAyahAutomatically(
                    pageVerses = pageVerses,
                    detectedSurahBanner = detectedSurahBanner,
                    verseScores = verseScores
                )
                return CameraRecognitionResult(
                    surahName = chosenFirstVerse.surahNameAr,
                    surahNumber = chosenFirstVerse.surahNumber,
                    ayahNumber = chosenFirstVerse.ayahNumber,
                    pageNumber = bestPage,
                    recognizedVerseSnippet = chosenFirstVerse.textAr,
                    confidenceNote = "تم التعرف التلقائي على الصفحة والسورة والآية الأولى"
                )
            }
        }

        // Fallback 1: If a Surah banner was clearly read (e.g., "سورة الكهف" or "سورة الإسراء")
        if (detectedSurahBanner != null) {
            val startPage = QuranDataHelper.getPageForSurahAndAyah(detectedSurahBanner.number, 1)
            val pageVerses = OfflineQuranDatabase.getVersesForPage(context, startPage)
            val firstOfSurah = pageVerses.firstOrNull { it.surahNumber == detectedSurahBanner.number }
                ?: OfflineQuranDatabase.getVerse(context, detectedSurahBanner.number, 1)

            return CameraRecognitionResult(
                surahName = detectedSurahBanner.nameArabic,
                surahNumber = detectedSurahBanner.number,
                ayahNumber = firstOfSurah?.ayahNumber ?: 1,
                pageNumber = firstOfSurah?.page ?: startPage,
                recognizedVerseSnippet = firstOfSurah?.textAr ?: "سورة ${detectedSurahBanner.nameArabic}",
                confidenceNote = "تم التعرف على عنوان سورة ${detectedSurahBanner.nameArabic} وتحديد أول آية"
            )
        }

        // Fallback 2: Printed Page Number (1..604) in top/bottom margin if no verses were clear enough
        val marginLines = (lines.take(2) + lines.takeLast(2)).distinct()
        val pageCandidate = extractPageNumberCandidate(marginLines.joinToString(" "))
            ?: extractPageNumberCandidate(cleanedText)
        if (pageCandidate != null && pageCandidate in 1..604) {
            val pageVerses = OfflineQuranDatabase.getVersesForPage(context, pageCandidate)
            if (pageVerses.isNotEmpty()) {
                val firstVerseOnPage = pageVerses.first()
                return CameraRecognitionResult(
                    surahName = firstVerseOnPage.surahNameAr,
                    surahNumber = firstVerseOnPage.surahNumber,
                    ayahNumber = firstVerseOnPage.ayahNumber,
                    pageNumber = firstVerseOnPage.page,
                    recognizedVerseSnippet = firstVerseOnPage.textAr,
                    confidenceNote = "تم قراءة رقم الصفحة (${firstVerseOnPage.page}) وتحديد الآية الأولى تلقائياً"
                )
            }
        }

        return null
    }

    /**
     * Automatically selects the primary Surah and FIRST Ayah on the recognized page:
     * - If the page contains a single Surah (e.g. Page 292 -> Surah Al-Isra 97..105), returns `pageVerses.first()`
     *   (Surah Al-Isra, Ayah 97 — the very first Ayah on the page).
     * - If the page contains multiple Surahs (e.g. Page 293 has the end of Al-Isra 105..111 AND the start of Al-Kahf 1..4):
     *   - If the OCR detected the Surah banner for the new Surah on that page (e.g. "سورة الكهف") or if a new Surah starts at Ayah 1 on that page and has matched words, returns the first Ayah of that Surah on the page (`سورة الكهف • آية 1`).
     *   - Otherwise returns `pageVerses.first()`.
     */
    private fun selectPageFirstAyahAutomatically(
        pageVerses: List<OfflineVerse>,
        detectedSurahBanner: SurahInfo?,
        verseScores: Map<String, Int>
    ): OfflineVerse {
        if (pageVerses.isEmpty()) {
            throw IllegalArgumentException("pageVerses cannot be empty")
        }

        // 1. If the OCR explicitly read a Surah banner (e.g. "سورة الكهف") that is present on this page,
        // return the first verse of that Surah on this page!
        if (detectedSurahBanner != null) {
            val firstOfBannerSurah = pageVerses.firstOrNull { it.surahNumber == detectedSurahBanner.number }
            if (firstOfBannerSurah != null) {
                return firstOfBannerSurah
            }
        }

        // 2. Check if this page has more than one Surah (e.g. a new Surah starts with Ayah 1 on this page)
        val distinctSurahsOnPage = pageVerses.map { it.surahNumber }.distinct()
        if (distinctSurahsOnPage.size > 1) {
            // If a new Surah starts on this page (ayahNumber == 1) and has strong verse matches in the OCR,
            // check which Surah on the page had higher average/total match or if the new Surah is prominent
            val newSurahFirstVerse = pageVerses.firstOrNull { it.ayahNumber == 1 && it.surahNumber != pageVerses.first().surahNumber }
            if (newSurahFirstVerse != null) {
                val newSurahScore = pageVerses
                    .filter { it.surahNumber == newSurahFirstVerse.surahNumber }
                    .sumOf { verseScores["${it.surahNumber}:${it.ayahNumber}"] ?: 0 }
                val prevSurahScore = pageVerses
                    .filter { it.surahNumber == pageVerses.first().surahNumber }
                    .sumOf { verseScores["${it.surahNumber}:${it.ayahNumber}"] ?: 0 }

                if (newSurahScore > prevSurahScore) {
                    return newSurahFirstVerse
                }
            }
        }

        // 3. Default: return the very first Ayah at the top of the recognized page!
        return pageVerses.first()
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

    /**
     * Detects explicit Surah banners ("سورة الكهف", "سورة الإسراء", etc.) in OCR text,
     * tolerating 1 character of OCR noise (such as "سورة الكفف" -> "سورة الكهف")
     * while NEVER false-matching 1-letter Surah names ("ص", "ق", "ن") inside normal words.
     */
    private fun detectSurahBannerFuzzy(text: String): SurahInfo? {
        val norm = QuranTextMatcher.normalizeArabic(text)
        val words = norm.split(" ").filter { it.isNotBlank() }

        // Look for "سوره <SurahName>" or fuzzy "سوره" ("سوره", "سورة", "سورا") followed by a Surah name
        for (i in 0 until words.size - 1) {
            val w = words[i]
            if (w == "سوره" || w == "سورا" || (w.length == 4 && editDistance(w, "سوره") <= 1)) {
                val nextOne = words[i + 1]
                val nextTwo = if (i + 2 < words.size) "$nextOne ${words[i + 2]}" else nextOne

                // 1. Exact match after "سوره"
                for (surah in QuranDataHelper.surahs.sortedByDescending { it.nameArabic.length }) {
                    val normSurah = QuranTextMatcher.normalizeArabic(surah.nameArabic)
                    if (nextTwo == normSurah || nextOne == normSurah) {
                        return surah
                    }
                }

                // 2. Fuzzy 1-edit-distance match after "سوره" for Surah names with >= 4 chars (e.g. "الكفف" -> "الكهف")
                var bestSurah: SurahInfo? = null
                var bestDist = 99
                for (surah in QuranDataHelper.surahs) {
                    val normSurah = QuranTextMatcher.normalizeArabic(surah.nameArabic)
                    if (normSurah.length < 4) continue
                    val d1 = editDistance(nextOne, normSurah)
                    val d2 = editDistance( QuranTextMatcher.toUthmaniSkeleton(nextOne), QuranTextMatcher.toUthmaniSkeleton(normSurah))
                    val minD = min(d1, d2)
                    if (minD <= 1 && minD < bestDist) {
                        bestDist = minD
                        bestSurah = surah
                    }
                }
                if (bestSurah != null) {
                    return bestSurah
                }
            }
        }
        return null
    }

    private fun editDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
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
