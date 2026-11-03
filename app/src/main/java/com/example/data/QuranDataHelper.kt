package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SurahInfo(
    val number: Int,
    val nameArabic: String,
    val totalAyahs: Int,
    val startPage: Int
)

data class KhatmaMetrics(
    val currentPage: Int,
    val remainingPages: Int,
    val completedRubCount: Int,
    val remainingRubCount: Int,
    val progressPercentage: Double,
    val actualDailyPacePages: Double,
    val projectedCompletionDate: String
)

object QuranDataHelper {
    const val TOTAL_PAGES = 604
    const val TOTAL_RUBS = 240

    val surahs: List<SurahInfo> = listOf(
        SurahInfo(1, "الفاتحة", 7, 1),
        SurahInfo(2, "البقرة", 286, 2),
        SurahInfo(3, "آل عمران", 200, 50),
        SurahInfo(4, "النساء", 176, 77),
        SurahInfo(5, "المائدة", 120, 106),
        SurahInfo(6, "الأنعام", 165, 128),
        SurahInfo(7, "الأعراف", 206, 151),
        SurahInfo(8, "الأنفال", 75, 177),
        SurahInfo(9, "التوبة", 129, 187),
        SurahInfo(10, "يونس", 109, 208),
        SurahInfo(11, "هود", 123, 221),
        SurahInfo(12, "يوسف", 111, 235),
        SurahInfo(13, "الرعد", 43, 249),
        SurahInfo(14, "إبراهيم", 52, 255),
        SurahInfo(15, "الحجر", 99, 262),
        SurahInfo(16, "النحل", 128, 267),
        SurahInfo(17, "الإسراء", 111, 282),
        SurahInfo(18, "الكهف", 110, 293),
        SurahInfo(19, "مريم", 98, 305),
        SurahInfo(20, "طه", 135, 312),
        SurahInfo(21, "الأنبياء", 112, 322),
        SurahInfo(22, "الحج", 78, 332),
        SurahInfo(23, "المؤمنون", 118, 342),
        SurahInfo(24, "النور", 64, 350),
        SurahInfo(25, "الفرقان", 77, 359),
        SurahInfo(26, "الشعراء", 227, 367),
        SurahInfo(27, "النمل", 93, 377),
        SurahInfo(28, "القصص", 88, 385),
        SurahInfo(29, "العنكبوت", 69, 396),
        SurahInfo(30, "الروم", 60, 404),
        SurahInfo(31, "لقمان", 34, 411),
        SurahInfo(32, "السجدة", 30, 415),
        SurahInfo(33, "الأحزاب", 73, 418),
        SurahInfo(34, "سبأ", 54, 428),
        SurahInfo(35, "فاطر", 45, 434),
        SurahInfo(36, "يس", 83, 440),
        SurahInfo(37, "الصافات", 182, 446),
        SurahInfo(38, "ص", 88, 453),
        SurahInfo(39, "الزمر", 75, 458),
        SurahInfo(40, "غافر", 85, 467),
        SurahInfo(41, "فصلت", 54, 477),
        SurahInfo(42, "الشورى", 53, 483),
        SurahInfo(43, "الزخرف", 89, 489),
        SurahInfo(44, "الدخان", 59, 496),
        SurahInfo(45, "الجاثية", 37, 499),
        SurahInfo(46, "الأحقاف", 35, 502),
        SurahInfo(47, "محمد", 38, 507),
        SurahInfo(48, "الفتح", 29, 511),
        SurahInfo(49, "الحجرات", 18, 515),
        SurahInfo(50, "ق", 45, 518),
        SurahInfo(51, "الذاريات", 60, 520),
        SurahInfo(52, "الطور", 49, 523),
        SurahInfo(53, "النجم", 62, 526),
        SurahInfo(54, "القمر", 55, 528),
        SurahInfo(55, "الرحمن", 78, 531),
        SurahInfo(56, "الواقعة", 96, 534),
        SurahInfo(57, "الحديد", 29, 537),
        SurahInfo(58, "المجادلة", 22, 542),
        SurahInfo(59, "الحشر", 24, 545),
        SurahInfo(60, "الممتحنة", 13, 549),
        SurahInfo(61, "الصف", 14, 551),
        SurahInfo(62, "الجمعة", 11, 553),
        SurahInfo(63, "المنافقون", 11, 554),
        SurahInfo(64, "التغابن", 18, 556),
        SurahInfo(65, "الطلاق", 12, 558),
        SurahInfo(66, "التحريم", 12, 560),
        SurahInfo(67, "الملك", 30, 562),
        SurahInfo(68, "القلم", 52, 564),
        SurahInfo(69, "الحاقة", 52, 566),
        SurahInfo(70, "المعارج", 44, 568),
        SurahInfo(71, "نوح", 28, 570),
        SurahInfo(72, "الجن", 28, 572),
        SurahInfo(73, "المزمل", 20, 574),
        SurahInfo(74, "المدثر", 56, 575),
        SurahInfo(75, "القيامة", 40, 577),
        SurahInfo(76, "الإنسان", 31, 578),
        SurahInfo(77, "المرسلات", 50, 580),
        SurahInfo(78, "النبأ", 40, 582),
        SurahInfo(79, "النازعات", 46, 583),
        SurahInfo(80, "عبس", 42, 585),
        SurahInfo(81, "التكوير", 29, 586),
        SurahInfo(82, "الانفطار", 19, 587),
        SurahInfo(83, "المطففين", 36, 587),
        SurahInfo(84, "الانشقاق", 25, 589),
        SurahInfo(85, "البروج", 22, 590),
        SurahInfo(86, "الطارق", 17, 591),
        SurahInfo(87, "الأعلى", 19, 591),
        SurahInfo(88, "الغاشية", 26, 592),
        SurahInfo(89, "الفجر", 30, 593),
        SurahInfo(90, "البلد", 20, 594),
        SurahInfo(91, "الشمس", 15, 595),
        SurahInfo(92, "الليل", 21, 595),
        SurahInfo(93, "الضحى", 11, 596),
        SurahInfo(94, "الشرح", 8, 596),
        SurahInfo(95, "التين", 8, 597),
        SurahInfo(96, "العلق", 19, 597),
        SurahInfo(97, "القدر", 5, 598),
        SurahInfo(98, "البينة", 8, 598),
        SurahInfo(99, "الزلزلة", 8, 599),
        SurahInfo(100, "العاديات", 11, 599),
        SurahInfo(101, "القارعة", 11, 600),
        SurahInfo(102, "التكاثر", 8, 600),
        SurahInfo(103, "العصر", 3, 601),
        SurahInfo(104, "الهمزة", 9, 601),
        SurahInfo(105, "الفيل", 5, 601),
        SurahInfo(106, "قريش", 4, 602),
        SurahInfo(107, "الماعون", 7, 602),
        SurahInfo(108, "الكوثر", 3, 602),
        SurahInfo(109, "الكافرون", 6, 603),
        SurahInfo(110, "النصر", 3, 603),
        SurahInfo(111, "المسد", 5, 603),
        SurahInfo(112, "الإخلاص", 4, 604),
        SurahInfo(113, "الفلق", 5, 604),
        SurahInfo(114, "الناس", 6, 604)
    )

    fun getSurahByName(name: String): SurahInfo {
        return surahs.find { it.nameArabic == name } ?: surahs.first()
    }

    fun pageToRub(page: Int): Int {
        val clampedPage = page.coerceIn(1, TOTAL_PAGES)
        return ((clampedPage - 1) * (TOTAL_RUBS.toDouble() / TOTAL_PAGES.toDouble())).toInt().coerceIn(0, TOTAL_RUBS)
    }

    fun getPageForSurahAndAyah(surahName: String, ayahNumber: Int): Int {
        val surah = surahs.find { it.nameArabic == surahName } ?: return 1
        return getPageForSurahAndAyah(surah.number, ayahNumber)
    }

    fun getPageForSurahAndAyah(surahNumber: Int, ayahNumber: Int): Int {
        val surah = surahs.find { it.number == surahNumber } ?: return 1
        if (surah.number == 1) return 1

        // Exact Medina Mushaf page calibration for Al-Baqarah (Surah 2)
        if (surah.number == 2) {
            val clampedAyah = ayahNumber.coerceIn(1, surah.totalAyahs)
            return when {
                clampedAyah <= 5 -> 2
                clampedAyah <= 16 -> 3
                clampedAyah <= 24 -> 4
                clampedAyah <= 29 -> 5
                clampedAyah <= 37 -> 6
                clampedAyah <= 48 -> 7
                clampedAyah <= 57 -> 8
                clampedAyah <= 61 -> 9
                clampedAyah <= 69 -> 10
                clampedAyah <= 76 -> 11
                clampedAyah <= 83 -> 12
                clampedAyah <= 88 -> 13
                clampedAyah <= 93 -> 14
                clampedAyah <= 101 -> 15
                clampedAyah <= 105 -> 16
                clampedAyah <= 112 -> 17
                clampedAyah <= 119 -> 18
                clampedAyah <= 126 -> 19
                clampedAyah <= 134 -> 20
                clampedAyah <= 141 -> 21
                clampedAyah <= 145 -> 22
                clampedAyah <= 153 -> 23
                clampedAyah <= 163 -> 24
                clampedAyah <= 169 -> 25
                clampedAyah <= 176 -> 26
                clampedAyah <= 181 -> 27
                clampedAyah <= 186 -> 28
                clampedAyah <= 190 -> 29
                clampedAyah <= 196 -> 30
                clampedAyah <= 202 -> 31
                clampedAyah <= 210 -> 32
                clampedAyah <= 215 -> 33
                clampedAyah <= 219 -> 34
                clampedAyah <= 224 -> 35
                clampedAyah <= 230 -> 36
                clampedAyah <= 233 -> 37
                clampedAyah <= 237 -> 38
                clampedAyah <= 245 -> 39
                clampedAyah <= 248 -> 40
                clampedAyah <= 252 -> 41
                clampedAyah <= 256 -> 42
                clampedAyah <= 259 -> 43
                clampedAyah <= 264 -> 44
                clampedAyah <= 269 -> 45
                clampedAyah <= 274 -> 46
                clampedAyah <= 281 -> 47
                clampedAyah <= 282 -> 48
                clampedAyah <= 286 -> 49
                else -> 49
            }
        }

        val nextSurah = surahs.find { it.number == surahNumber + 1 }
        val nextSurahPage = nextSurah?.startPage ?: (TOTAL_PAGES + 1)
        val pageSpan = (nextSurahPage - surah.startPage).coerceAtLeast(1)
        val clampedAyah = ayahNumber.coerceIn(1, surah.totalAyahs)
        val calculatedPage = surah.startPage + (((clampedAyah - 1).toDouble() / surah.totalAyahs.toDouble()) * pageSpan).toInt()
        return calculatedPage.coerceIn(surah.startPage, (nextSurahPage - 1).coerceAtLeast(surah.startPage))
    }

    fun pageToSurahAndAyahEstimate(page: Int): Pair<SurahInfo, Int> {
        val clampedPage = page.coerceIn(1, TOTAL_PAGES)
        val surah = surahs.lastOrNull { it.startPage <= clampedPage } ?: surahs.first()
        val nextSurahPage = surahs.getOrNull(surahs.indexOf(surah) + 1)?.startPage ?: (TOTAL_PAGES + 1)
        val pageSpan = (nextSurahPage - surah.startPage).coerceAtLeast(1)
        val pagesIntoSurah = (clampedPage - surah.startPage).coerceAtLeast(0)
        val estimatedAyah = (((pagesIntoSurah + 1).toDouble() / pageSpan.toDouble()) * surah.totalAyahs).toInt().coerceIn(1, surah.totalAyahs)
        return Pair(surah, estimatedAyah)
    }

    fun calculateMetrics(
        currentPage: Int,
        startDateMillis: Long = System.currentTimeMillis() - (86400000L * 7)
    ): KhatmaMetrics {
        val clampedPage = currentPage.coerceIn(1, TOTAL_PAGES)
        val remainingPages = TOTAL_PAGES - clampedPage
        val completedRubCount = pageToRub(clampedPage)
        val remainingRubCount = TOTAL_RUBS - completedRubCount
        val progressPercentage = (clampedPage.toDouble() / TOTAL_PAGES.toDouble()) * 100.0

        val daysElapsed = ((System.currentTimeMillis() - startDateMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        val actualPace = (clampedPage.toDouble() / daysElapsed.toDouble()).coerceAtLeast(1.0)

        val daysToFinish = (remainingPages.toDouble() / actualPace).toInt()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysToFinish)
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        val projectedDate = sdf.format(calendar.time)

        return KhatmaMetrics(
            currentPage = clampedPage,
            remainingPages = remainingPages,
            completedRubCount = completedRubCount,
            remainingRubCount = remainingRubCount,
            progressPercentage = String.format(Locale.US, "%.2f", progressPercentage).toDouble(),
            actualDailyPacePages = String.format(Locale.US, "%.1f", actualPace).toDouble(),
            projectedCompletionDate = projectedDate
        )
    }
}
