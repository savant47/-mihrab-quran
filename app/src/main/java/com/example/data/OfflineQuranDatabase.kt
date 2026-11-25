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
    val startsNewRub: Boolean = false,
    val page: Int,
    val isSajda: Boolean,
    val normalizedTextAr: String,
    val searchCanonicalTextAr: String = normalizedTextAr,
    val searchSkeletonTextAr: String = normalizedTextAr
)

data class QuranSearchMatch(
    val verse: OfflineVerse,
    val occurrenceCount: Int,
    val isExactMatch: Boolean
)

data class QuranSearchResult(
    val query: String,
    val totalMatchingVerses: Int,
    val totalWordOccurrences: Int,
    val matches: List<QuranSearchMatch>
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

    private val uthmaniWawAlefRegex = Regex("و\\u0670")
    private val uthmaniYaAlefTaMarbutaRegex = Regex("ى\\u0670(?=[ةه])")
    private val uthmaniStandaloneSuperAlefRegex = Regex("\\u0670")

    /**
     * Converts Uthmani Quranic orthography (e.g., ٱلصَّلَوٰةَ, ٱلزَّكَوٰةَ, ٱلْحَيَوٰةِ, ٱلتَّوْرَىٰةَ, ٱلرَّحْمَٰنِ)
     * into standard searchable Arabic form before normalizing diacritics so standard keyboard queries
     * like "الصلاة", "الزكاة", "الحياة", "التوراة", "الكتاب", "السماوات" match instantly.
     */
    fun toSearchCanonicalArabic(input: String): String {
        val preNormalized = input
            .replace(uthmaniWawAlefRegex, "ا")
            .replace(uthmaniYaAlefTaMarbutaRegex, "ا")
        return QuranTextMatcher.normalizeArabic(preNormalized)
    }

    private fun toSearchSkeletonLine(canonicalText: String): String {
        if (canonicalText.isEmpty()) return ""
        return canonicalText
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { QuranTextMatcher.toUthmaniSkeleton(it) }
    }

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

                        val startsNewRub = arText.contains('\u06DE') || SURAH_START_RUB_KEYS.contains("$sNum:$vNum")
                        if (startsNewRub) {
                            currentRubIndex++
                        }
                        val safeRubIdx = currentRubIndex.coerceIn(0, 239)
                        val exactHizb = ((safeRubIdx / 4) + 1).coerceIn(1, 60)
                        val exactRubInHizb = ((safeRubIdx % 4) + 1).coerceIn(1, 4)

                        if (j == 0) firstPage = page

                        val normAr = QuranTextMatcher.normalizeArabic(arText)
                        val canonAr = toSearchCanonicalArabic(arText)
                        val skelAr = toSearchSkeletonLine(canonAr)

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
                            startsNewRub = startsNewRub,
                            page = page,
                            isSajda = sajda,
                            normalizedTextAr = normAr,
                            searchCanonicalTextAr = canonAr,
                            searchSkeletonTextAr = skelAr
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

    private fun wordMatchesQuery(
        normWord: String,
        canonWord: String,
        skelWord: String,
        normQ: String,
        canonQ: String,
        skelQ: String,
        exactWholeWordOnly: Boolean
    ): Pair<Boolean, Boolean> {
        val isExact = normWord == normQ ||
            canonWord == canonQ ||
            (skelQ.length >= 3 && skelWord == skelQ)
        if (isExact) return true to true
        if (exactWholeWordOnly) return false to false

        val isPartial = normWord.contains(normQ) ||
            canonWord.contains(canonQ) ||
            (skelQ.length >= 3 && skelWord.contains(skelQ))
        return isPartial to false
    }

    /**
     * Instant offline search across all 6,236 verses of the Mushaf.
     * Supports standard Arabic keyboard spelling as well as Uthmani script,
     * counts exact total matching verses and total word occurrences,
     * and ranks exact whole-word/phrase matches above partial matches.
     */
    fun searchVersesInstant(
        context: Context,
        query: String,
        exactWholeWordOnly: Boolean = false,
        limit: Int = 800
    ): QuranSearchResult {
        ensureLoadedSynchronously(context)
        val trimmed = query.trim()
        val normQuery = QuranTextMatcher.normalizeArabic(trimmed)
        val canonQuery = toSearchCanonicalArabic(trimmed)
        if (normQuery.length < 2 && canonQuery.length < 2) {
            return QuranSearchResult(query = trimmed, totalMatchingVerses = 0, totalWordOccurrences = 0, matches = emptyList())
        }

        val normQueryWords = normQuery.split(' ').filter { it.isNotEmpty() }
        val canonQueryWords = canonQuery.split(' ').filter { it.isNotEmpty() }
        val skelQueryWords = canonQueryWords.map { QuranTextMatcher.toUthmaniSkeleton(it) }
        val qLen = canonQueryWords.size
        if (qLen == 0) {
            return QuranSearchResult(query = trimmed, totalMatchingVerses = 0, totalWordOccurrences = 0, matches = emptyList())
        }

        val exactMatches = ArrayList<QuranSearchMatch>()
        val partialMatches = ArrayList<QuranSearchMatch>()
        var totalOccurrences = 0

        if (qLen == 1) {
            val nq = normQueryWords[0]
            val cq = canonQueryWords[0]
            val sq = skelQueryWords[0]

            for (v in allVersesList) {
                // Fast pre-check before splitting
                val maybeHit = v.normalizedTextAr.contains(nq) ||
                    v.searchCanonicalTextAr.contains(cq) ||
                    (sq.length >= 3 && v.searchSkeletonTextAr.contains(sq))
                if (!maybeHit) continue

                val normTokens = v.normalizedTextAr.split(' ')
                val canonTokens = v.searchCanonicalTextAr.split(' ')
                val skelTokens = v.searchSkeletonTextAr.split(' ')
                val countTokens = minOf(normTokens.size, canonTokens.size, skelTokens.size)

                var verseHits = 0
                var hasExact = false
                for (idx in 0 until countTokens) {
                    val (matched, exact) = wordMatchesQuery(
                        normWord = normTokens[idx],
                        canonWord = canonTokens[idx],
                        skelWord = skelTokens[idx],
                        normQ = nq,
                        canonQ = cq,
                        skelQ = sq,
                        exactWholeWordOnly = exactWholeWordOnly
                    )
                    if (matched) {
                        verseHits++
                        if (exact) hasExact = true
                    }
                }
                if (verseHits > 0) {
                    totalOccurrences += verseHits
                    val item = QuranSearchMatch(verse = v, occurrenceCount = verseHits, isExactMatch = hasExact)
                    if (hasExact) exactMatches.add(item) else partialMatches.add(item)
                }
            }
        } else {
            val skelPhrase = skelQueryWords.joinToString(" ")
            for (v in allVersesList) {
                val fastHit = v.normalizedTextAr.contains(normQuery) ||
                    v.searchCanonicalTextAr.contains(canonQuery) ||
                    (skelPhrase.length >= 4 && v.searchSkeletonTextAr.contains(skelPhrase))
                if (!fastHit) continue

                val normTokens = v.normalizedTextAr.split(' ').filter { it.isNotEmpty() }
                val canonTokens = v.searchCanonicalTextAr.split(' ').filter { it.isNotEmpty() }
                val skelTokens = v.searchSkeletonTextAr.split(' ').filter { it.isNotEmpty() }
                val countTokens = minOf(normTokens.size, canonTokens.size, skelTokens.size)

                var verseHits = 0
                var hasExact = false
                if (countTokens >= qLen) {
                    for (start in 0..(countTokens - qLen)) {
                        var allMatch = true
                        var allExact = true
                        for (k in 0 until qLen) {
                            val requireWhole = exactWholeWordOnly || (k in 1 until (qLen - 1))
                            val (matched, exact) = wordMatchesQuery(
                                normWord = normTokens[start + k],
                                canonWord = canonTokens[start + k],
                                skelWord = skelTokens[start + k],
                                normQ = normQueryWords.getOrElse(k) { canonQueryWords[k] },
                                canonQ = canonQueryWords[k],
                                skelQ = skelQueryWords[k],
                                exactWholeWordOnly = requireWhole
                            )
                            if (!matched) {
                                allMatch = false
                                break
                            }
                            if (!exact) allExact = false
                        }
                        if (allMatch) {
                            verseHits++
                            if (allExact) hasExact = true
                        }
                    }
                }
                if (verseHits == 0 && !exactWholeWordOnly && (v.normalizedTextAr.contains(normQuery) || v.searchCanonicalTextAr.contains(canonQuery))) {
                    verseHits = 1
                }
                if (verseHits > 0) {
                    totalOccurrences += verseHits
                    val item = QuranSearchMatch(verse = v, occurrenceCount = verseHits, isExactMatch = hasExact)
                    if (hasExact) exactMatches.add(item) else partialMatches.add(item)
                }
            }
        }

        val combined = ArrayList<QuranSearchMatch>(exactMatches.size + partialMatches.size)
        combined.addAll(exactMatches)
        combined.addAll(partialMatches)

        return QuranSearchResult(
            query = trimmed,
            totalMatchingVerses = combined.size,
            totalWordOccurrences = totalOccurrences,
            matches = if (combined.size > limit) combined.subList(0, limit) else combined
        )
    }

    /**
     * Returns the 0-based whitespace-split word indices in `rawVerseText` that match `query`,
     * enabling diacritic-safe visual highlighting directly on the original Uthmani text.
     */
    fun findMatchingWordIndicesInVerse(
        rawVerseText: String,
        query: String,
        exactWholeWordOnly: Boolean = false
    ): Set<Int> {
        val trimmed = query.trim()
        val normQuery = QuranTextMatcher.normalizeArabic(trimmed)
        val canonQuery = toSearchCanonicalArabic(trimmed)
        val normQueryWords = normQuery.split(' ').filter { it.isNotEmpty() }
        val canonQueryWords = canonQuery.split(' ').filter { it.isNotEmpty() }
        val skelQueryWords = canonQueryWords.map { QuranTextMatcher.toUthmaniSkeleton(it) }
        val qLen = canonQueryWords.size
        if (qLen == 0) return emptySet()

        val rawTokens = rawVerseText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val normTokens = rawTokens.map { QuranTextMatcher.normalizeArabic(it) }
        val canonTokens = rawTokens.map { toSearchCanonicalArabic(it) }
        val skelTokens = canonTokens.map { QuranTextMatcher.toUthmaniSkeleton(it) }

        val matchedIndices = HashSet<Int>()
        if (qLen == 1) {
            val nq = normQueryWords[0]
            val cq = canonQueryWords[0]
            val sq = skelQueryWords[0]
            for (i in rawTokens.indices) {
                val (matched, _) = wordMatchesQuery(
                    normWord = normTokens[i],
                    canonWord = canonTokens[i],
                    skelWord = skelTokens[i],
                    normQ = nq,
                    canonQ = cq,
                    skelQ = sq,
                    exactWholeWordOnly = exactWholeWordOnly
                )
                if (matched) matchedIndices.add(i)
            }
        } else if (rawTokens.size >= qLen) {
            for (start in 0..(rawTokens.size - qLen)) {
                var allMatch = true
                for (k in 0 until qLen) {
                    val requireWhole = exactWholeWordOnly || (k in 1 until (qLen - 1))
                    val (matched, _) = wordMatchesQuery(
                        normWord = normTokens[start + k],
                        canonWord = canonTokens[start + k],
                        skelWord = skelTokens[start + k],
                        normQ = normQueryWords.getOrElse(k) { canonQueryWords[k] },
                        canonQ = canonQueryWords[k],
                        skelQ = skelQueryWords[k],
                        exactWholeWordOnly = requireWhole
                    )
                    if (!matched) {
                        allMatch = false
                        break
                    }
                }
                if (allMatch) {
                    for (k in 0 until qLen) matchedIndices.add(start + k)
                }
            }
        }
        return matchedIndices
    }

    fun searchVerses(context: Context, query: String, limit: Int = 30): List<OfflineVerse> {
        return searchVersesInstant(context, query, exactWholeWordOnly = false, limit = limit).matches.map { it.verse }
    }
}

