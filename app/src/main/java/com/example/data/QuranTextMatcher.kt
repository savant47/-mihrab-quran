package com.example.data

/**
 * High-performance Arabic text normalizer and Quran Ayah matcher.
 * Automatically identifies Surah name, Ayah number, and Page number from pasted Arabic text.
 */
object QuranTextMatcher {

    data class MatchResult(
        val surahName: String,
        val ayahNumber: Int,
        val pageNumber: Int,
        val description: String
    )

    data class AyahEntry(
        val normalizedText: String,
        val surahName: String,
        val ayahNumber: Int,
        val pageNumber: Int
    )

    private val tashkeelAndQuranSymbolsRegex = Regex("[\\u0610-\\u061A\\u0640\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u06E6\\u06DF\\u06E0\\u06E1\\u06E2\\u06E3\\u06E5\\u06E6]")
    private val nonArabicLettersRegex = Regex("[^\\u0621-\\u064A\\s0-9]")

    fun convertArabicDigitsToAscii(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660' + '0'.code).toChar())
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0' + '0'.code).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun normalizeArabic(input: String): String {
        val converted = convertArabicDigitsToAscii(input.trim())
        var text = converted.replace(tashkeelAndQuranSymbolsRegex, "")
        text = text.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
        text = text.replace(nonArabicLettersRegex, " ")
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Converts a normalized Arabic word to an Alef/Hamza-agnostic Uthmani skeleton
     * so Uthmani script (e.g. "الكتب", "سبحن", "الصلحت", "عظما", "رفتا", "السموت")
     * matches standard Arabic ("الكتاب", "سبحان", "الصالحات", "عظاما", "رفاتا", "السماوات") identically.
     */
    fun toUthmaniSkeleton(word: String): String {
        val clean = word.replace("ء", "").trim()
        if (clean.length <= 2) return clean
        return clean[0] + clean.substring(1).replace("ا", "")
    }

    private val citationBracketRegex = Regex(
        "[\\[\\(\\{]\\s*(?:سُورَةُ|سورة)?\\s*([^:\\]\\)\\}\\-\\—\\n\\r]+?)\\s*[:\\-\\—\\,]\\s*(?:الآية|الآيه|آية|ايه)?\\s*([0-9\u0660-\u0669\u06F0-\u06F9]+)\\s*[\\]\\)\\}]"
    )

    private val freeTextCitationRegex = Regex(
        "(?:سُورَةُ|سورة)\\s+([^\\d:،\\-\\—\\n\\r]+?)\\s*(?:[:\\-\\—]|(?:الآية|الآيه|آية|ايه|رقم))\\s*([0-9\u0660-\u0669\u06F0-\u06F9]+)"
    )

    private val stopWords = setOf(
        "الله", "رب", "الذين", "امنوا", "في", "من", "على", "ما", "ان", "هو", "الا", "قال",
        "يا", "ايها", "كل", "ولا", "لهم", "بما", "كانوا", "الي", "به", "عن", "لا", "هذا",
        "هذه", "ذلك", "تلك", "ثم", "قد", "لو", "او", "بل", "كي", "حتى"
    )

    private val knownAyahs: List<AyahEntry> by lazy {
        val list = mutableListOf<AyahEntry>()

        fun add(text: String, surah: String, ayah: Int, page: Int) {
            list.add(AyahEntry(normalizeArabic(text), surah, ayah, page))
        }

        // Al-Fatihah
        add("بسم الله الرحمن الرحيم", "الفاتحة", 1, 1)
        add("الحمد لله رب العالمين", "الفاتحة", 2, 1)
        add("الرحمن الرحيم", "الفاتحة", 3, 1)
        add("مالك يوم الدين", "الفاتحة", 4, 1)
        add("اياك نعبد واياك نستعين", "الفاتحة", 5, 1)
        add("اهدنا الصراط المستقيم", "الفاتحة", 6, 1)
        add("صراط الذين انعمت عليهم غير المغضوب عليهم ولا الضالين", "الفاتحة", 7, 1)

        // Al-Baqarah
        add("الم ذلك الكتاب لا ريب فيه هدى للمتقين", "البقرة", 1, 2)
        add("ذلك الكتاب لا ريب فيه", "البقرة", 2, 2)
        add("الذين يؤمنون بالغيب ويقيمون الصلاة ومما رزقناهم ينفقون", "البقرة", 3, 2)
        add("والذين يؤمنون بما انزل اليك وما انزل من قبلك وبالاخرة هم يوقنون", "البقرة", 4, 2)
        add("اولئك على هدى من ربهم واولئك هم المفلحون", "البقرة", 5, 2)
        add("ان الذين كفروا سواء عليهم اانذرتهم ام لم تنذرهم لا يؤمنون", "البقرة", 6, 2)
        add("ختم الله على قلوبهم وعلى سمعهم وعلى ابصارهم غشاوة", "البقرة", 7, 2)
        add("ان الله لا يستحيي ان يضرب مثلا ما بعوضة فما فوقها فاما الذين امنوا فيعلمون انه الحق من ربهم واما الذين كفروا فيقولون ماذا اراد الله بهذا مثلا يضل به كثيرا ويهدي به كثيرا وما يضل به الا الفاسقين", "البقرة", 26, 5)
        add("اراد الله بهذا مثلا يضل به كثيرا ويهدي به كثيرا وما يضل به الا الفاسقين", "البقرة", 26, 5)
        add("واذ قال ربك للملائكة اني جاعل في الارض خليفة", "البقرة", 30, 6)
        add("واتبعوا ما تتلوا الشياطين على ملك سليمان", "البقرة", 102, 16)
        add("يا ايها الذين امنوا كتب عليكم الصيام كما كتب على الذين من قبلكم", "البقرة", 183, 28)
        add("شهر رمضان الذي انزل فيه القران هدى للناس وبينات من الهدى والفرقان", "البقرة", 185, 28)
        add("واذا سالك عبادي عني فاني قريب اجيب دعوة الداع اذا دعان", "البقرة", 186, 29)
        add("الله لا اله الا هو الحي القيوم لا تاخذه سنة ولا نوم", "البقرة", 255, 42)
        add("لا اكراه في الدين قد تبين الرشد من الغي", "البقرة", 256, 42)
        add("امن الرسول بما انزل اليه من ربه والمؤمنون", "البقرة", 285, 49)
        add("لا يكلف الله نفسا الا وسعها لها ما كسبت وعليها ما اكتسبت", "البقرة", 286, 49)

        // Ali 'Imran
        add("الم الله لا اله الا هو الحي القيوم", "آل عمران", 1, 50)
        add("نزل عليك الكتاب بالحق مصدقا لما بين يديه", "آل عمران", 3, 50)
        add("شهد الله انه لا اله الا هو والملائكة واولو العلم قائما بالقسط", "آل عمران", 18, 52)
        add("ان الدين عند الله الاسلام", "آل عمران", 19, 52)
        add("قل اللهم مالك الملك تؤتي الملك من تشاء وتنزع الملك ممن تشاء", "آل عمران", 26, 53)
        add("واعتصموا بحبل الله جميعا ولا تفرقوا", "آل عمران", 103, 63)
        add("ان في خلق السماوات والارض واختلاف الليل والنهار لايات لاولي الالباب", "آل عمران", 190, 75)
        add("يا ايها الذين امنوا اصبروا وصابروا ورابطوا واتقوا الله لعلكم تفلحون", "آل عمران", 200, 76)

        // An-Nisa
        add("يا ايها الناس اتقوا ربكم الذي خلقكم من نفس واحدة", "النساء", 1, 77)
        add("واعبدوا الله ولا تشركوا به شيئا وبالوالدين احسانا", "النساء", 36, 84)
        add("ان الله يامركم ان تؤدوا الامانات الى اهلها", "النساء", 58, 87)

        // Al-Ma'idah
        add("يا ايها الذين امنوا اوفوا بالعقود", "المائدة", 1, 106)
        add("اليوم اكملت لكم دينكم واتممت عليكم نعمتي ورضيت لكم الاسلام دينا", "المائدة", 3, 107)
        add("وتعاونوا على البر والتقوى ولا تعاونوا على الاثم والعدوان", "المائدة", 2, 106)

        // Al-An'am
        add("الحمد لله الذي خلق السماوات والارض وجعل الظلمات والنور", "الأنعام", 1, 128)
        add("قل تعالوا اتل ما حرم ربكم عليكم الا تشركوا به شيئا وبالوالدين احسانا", "الأنعام", 151, 148)

        // Al-A'raf
        add("المص كتاب انزل اليك فلا يكن في صدرك حرج منه", "الأعراف", 1, 151)
        add("ان وليي الله الذي نزل الكتاب وهو يتولى الصالحين", "الأعراف", 196, 176)

        // Al-Anfal
        add("يسالونك عن الانفال قل الانفال لله والرسول", "الأنفال", 1, 177)
        add("انما المؤمنون الذين اذا ذكر الله وجلت قلوبهم", "الأنفال", 2, 177)

        // At-Tawbah
        add("براءة من الله ورسوله الى الذين عاهدتم من المشركين", "التوبة", 1, 187)
        add("لقد جاءكم رسول من انفسكم عزيز عليه ما عنتم حريص عليكم بالمؤمنين رءوف رحيم", "التوبة", 128, 207)

        // Yunus
        add("الر تلك ايات الكتاب الحكيم", "يونس", 1, 208)
        add("الا ان اولياء الله لا خوف عليهم ولا هم يحزنون", "يونس", 62, 215)

        // Hud
        add("الر كتاب احكمت اياته ثم فصلت من لدن حكيم خبير", "هود", 1, 221)
        add("واقم الصلاة طرفي النهار وزلفا من الليل ان الحسنات يذهبن السيئات", "هود", 114, 234)

        // Yusuf
        add("الر تلك ايات الكتاب المبين انا انزلناه قرانا عربيا", "يوسف", 1, 235)
        add("نحن نقص عليك احسن القصص بما اوحينا اليك هذا القران", "يوسف", 3, 235)
        add("اذ قال يوسف لابيه يا ابت اني رايت احد عشر كوكبا والشمس والقمر رايتهم لي ساجدين", "يوسف", 4, 235)

        // Ar-Ra'd
        add("المر تلك ايات الكتاب والذي انزل اليك من ربك الحق", "الرعد", 1, 249)
        add("الذين امنوا وتطمئن قلوبهم بذكر الله الا بذكر الله تطمئن القلوب", "الرعد", 28, 253)

        // Ibrahim
        add("الر كتاب انزلناه اليك لتخرج الناس من الظلمات الى النور", "إبراهيم", 1, 255)
        add("واذ تاذن ربكم لئن شكرتم لازيدنكم ولئن كفرتم ان عذابي لشديد", "إبراهيم", 7, 256)

        // Al-Hijr
        add("الر تلك ايات الكتاب وقران مبين", "الحجر", 1, 262)
        add("انا نحن نزلنا الذكر وانا له لحافظون", "الحجر", 9, 262)

        // An-Nahl
        add("اتى امر الله فلا تستعجلوه سبحانه وتعالى عما يشركون", "النحل", 1, 267)
        add("ان الله يامر بالعدل والاحسان وايتاء ذي القربى وينهى عن الفحشاء والمنكر والبغي", "النحل", 90, 277)

        // Al-Isra
        add("سبحان الذي اسرى بعبده ليلا من المسجد الحرام الى المسجد الاقصى", "الإسراء", 1, 282)
        add("وقضى ربك الا تعبدوا الا اياه وبالوالدين احسانا", "الإسراء", 23, 284)
        add("وقل رب ارحمهما كما ربياني صغيرا", "الإسراء", 24, 284)

        // Al-Kahf
        add("الحمد لله الذي انزل على عبده الكتاب ولم يجعل له عوجا", "الكهف", 1, 293)
        add("ام حسبت ان اصحاب الكهف والرقيم كانوا من اياتنا عجبا", "الكهف", 9, 294)
        add("قل لو كان البحر مدادا لكلمات ربي لنفد البحر قبل ان تنفد كلمات ربي", "الكهف", 109, 304)
        add("قل انما انا بشر مثلكم يوحى الي انما الهكم اله واحد فمن كان يرجو لقاء ربه فليعمل عملا صالحا", "الكهف", 110, 304)

        // Maryam
        add("كهيعص ذكر رحمت ربك عبده زكريا", "مريم", 1, 305)
        add("قال اني عبد الله اتاني الكتاب وجعلني نبيا وجعلني مباركا اين ما كنت", "مريم", 30, 307)

        // Ta-Ha
        add("طه ما انزلنا عليك القران لتشقى الا تذكرة لمن يخشى", "طه", 1, 312)
        add("انني انا الله لا اله الا انا فاعبدني واقم الصلاة لذكري", "طه", 14, 313)

        // Al-Anbiya
        add("اقترب للناس حسابهم وهم في غفلة معرضون", "الأنبياء", 1, 322)
        add("وما ارسلناك الا رحمة للعالمين", "الأنبياء", 107, 331)

        // Al-Hajj
        add("يا ايها الناس اتقوا ربكم ان زلزلة الساعة شيء عظيم", "الحج", 1, 332)
        add("اذن للذين يقاتلون بانهم ظلموا وان الله على نصرهم لقدير", "الحج", 39, 337)

        // Al-Mu'minun
        add("قد افلح المؤمنون الذين هم في صلاتهم خاشعون والذين هم عن اللغو معرضون", "المؤمنون", 1, 342)
        add("ولقد خلقنا الانسان من سلالة من طين", "المؤمنون", 12, 342)

        // An-Nur
        add("سورة انزلناها وفرضناها وانزلنا فيها ايات بينات لعلكم تذكرون", "النور", 1, 350)
        add("الله نور السماوات والارض مثل نوره كمشكاة فيها مصباح", "النور", 35, 354)

        // Al-Furqan
        add("تبارك الذي نزل الفرقان على عبده ليكون للعالمين نذيرا", "الفرقان", 1, 359)
        add("وعباد الرحمن الذين يمشون على الارض هونا واذا خاطبهم الجاهلون قالوا سلاما", "الفرقان", 63, 365)

        // Ya-Sin
        add("يس والقران الحكيم انك لمن المرسلين", "يس", 1, 440)
        add("انما امره اذا اراد شيئا ان يقول له كن فيكون", "يس", 82, 445)
        add("فسبحان الذي بيده ملكوت كل شيء واليه ترجعون", "يس", 83, 445)

        // Az-Zumar
        add("تنزيل الكتاب من الله العزيز الحكيم", "الزمر", 1, 458)
        add("قل يا عبادي الذين اسرفوا على انفسهم لا تقنطوا من رحمة الله ان الله يغفر الذنوب جميعا", "الزمر", 53, 464)

        // Al-Fath
        add("انا فتحنا لك فتحا مبينا ليغفر لك الله ما تقدم من ذنبك وما تاخر", "الفتح", 1, 511)
        add("محمد رسول الله والذين معه اشداء على الكفار رحماء بينهم", "الفتح", 29, 515)

        // Al-Hujurat
        add("يا ايها الذين امنوا ان جاءكم فاسق بنبا فتبينوا", "الحجرات", 6, 516)
        add("انما المؤمنون اخوة فاصلحوا بين اخويكم واتقوا الله", "الحجرات", 10, 516)
        add("يا ايها الناس انا خلقناكم من ذكر وانثى وجعلناكم شعوبا وقبائل لتعارفوا ان اكرمكم عند الله اتقاكم", "الحجرات", 13, 517)

        // Qaf
        add("ق والقران المجيد بل عجبوا ان جاءهم منذر منهم", "ق", 1, 518)
        add("ولقد خلقنا الانسان ونعلم ما توسوس به نفسه ونحن اقرب اليه من حبل الوريد", "ق", 16, 519)

        // Ar-Rahman
        add("الرحمن علم القران خلق الانسان علمه البيان", "الرحمن", 1, 531)
        add("فباي الاء ربكما تكذبان", "الرحمن", 13, 531)
        add("كل من عليها فان ويبقى وجه ربك ذو الجلال والاكرام", "الرحمن", 26, 532)

        // Al-Waqi'ah
        add("اذا وقعت الواقعة ليس لوقعتها كاذبة خافضة رافعة", "الواقعة", 1, 534)
        add("فروح وريحان وجنة نعيم", "الواقعة", 89, 537)

        // Al-Hadid
        add("سبح لله ما في السماوات والارض وهو العزيز الحكيم", "الحديد", 1, 537)
        add("هو الاول والاخر والظاهر والباطن وهو بكل شيء عليم", "الحديد", 3, 537)
        add("الم يان للذين امنوا ان تخشع قلوبهم لذكر الله وما نزل من الحق", "الحديد", 16, 539)

        // Al-Hashr
        add("لو انزلنا هذا القران على جبل لرايته خاشعا متصدعا من خشية الله", "الحشر", 21, 548)
        add("هو الله الذي لا اله الا هو عالم الغيب والشهادة هو الرحمن الرحيم", "الحشر", 22, 548)

        // Al-Mulk
        add("تبارك الذي بيده الملك وهو على كل شيء قدير", "الملك", 1, 562)
        add("الذي خلق الموت والحياة ليبلوكم ايكم احسن عملا", "الملك", 2, 562)

        // Al-Qalam
        add("ن والقلم وما يسطرون ما انت بنعمة ربك بمجنون وان لك لاجرا غير ممنون وانك لعلى خلق عظيم", "القلم", 1, 564)

        // An-Naba
        add("عم يتساءلون عن النبا العظيم الذي هم فيه مختلفون", "النبأ", 1, 582)
        add("ان للمتقين مفازا حدائق واعنابا", "النبأ", 31, 583)

        // Al-Fajr
        add("والفجر وليال عشر والشفع والوتر والليل اذا يسر", "الفجر", 1, 593)
        add("يا ايتها النفس المطمئنة ارجعي الى ربك راضية مرضية فادخلي في عبادي وادخلي جنتي", "الفجر", 27, 594)

        // Ad-Duha
        add("والضحى والليل اذا سجى ما ودعك ربك وما قلى وللاخرة خير لك من الاولى ولسوف يعطيك ربك فترضى", "الضحى", 1, 596)
        add("فاما اليتيم فلا تقهر واما السائل فلا تنهر واما بنعمة ربك فحدث", "الضحى", 9, 596)

        // Ash-Sharh
        add("الم نشرح لك صدرك ووضعنا عنك وزرك الذي انقض ظهرك ورفعنا لك ذكرك فان مع العسر يسرا ان مع العسر يسرا", "الشرح", 1, 596)

        // Al-Qadr
        add("انا انزلناه في ليلة القدر وما ادراك ما ليلة القدر ليلة القدر خير من الف شهر", "القدر", 1, 598)

        // Az-Zalzalah
        add("اذا زلزلت الارض زلزالها واخرجت الارض اثقالها", "الزلزلة", 1, 599)
        add("فمن يعمل مثقال ذرة خيرا يره ومن يعمل مثقال ذرة شرا يره", "الزلزلة", 7, 599)

        // Al-'Asr
        add("والعصر ان الانسان لفي خسر الا الذين امنوا وعملوا الصالحات وتواصوا بالحق وتواصوا بالصبر", "العصر", 1, 601)

        // Al-Ikhlas
        add("قل هو الله احد الله الصمد لم يلد ولم يولد ولم يكن له كفوا احد", "الإخلاص", 1, 604)

        // Al-Falaq
        add("قل اعوذ برب الفلق من شر ما خلق ومن شر غاسق اذا وقب ومن شر النفاثات في العقد ومن شر حاسد اذا حسد", "الفلق", 1, 604)

        // An-Nas
        add("قل اعوذ برب الناس ملك الناس اله الناس من شر الوسواس الخناس", "الناس", 1, 604)

        list
    }

    /**
     * Attempts to find a matching Surah and Ayah from raw user pasted or entered Arabic text.
     */
    fun findAyahMatch(rawText: String): MatchResult? {
        val trimmed = rawText.trim()
        if (trimmed.length < 2) return null

        // 1. Check for standard Quran citation bracket: [سُورَةُ البَقَرَةِ: ٢٦] or (البقرة: 26) or {سورة البقرة: 26}
        val bracketMatch = citationBracketRegex.find(trimmed)
        if (bracketMatch != null) {
            val rawSurah = bracketMatch.groupValues[1]
            val rawAyah = bracketMatch.groupValues[2]
            val result = matchSurahAndAyah(rawSurah, rawAyah, "تم التعرف بدقة من التوثيق")
            if (result != null) return result
        }

        // 2. Check for free-text citation: "سورة البقرة آية 26" or "سورة الكهف: 10"
        val freeMatch = freeTextCitationRegex.find(trimmed)
        if (freeMatch != null) {
            val rawSurah = freeMatch.groupValues[1]
            val rawAyah = freeMatch.groupValues[2]
            val result = matchSurahAndAyah(rawSurah, rawAyah, "تم التعرف من النص المقتبس")
            if (result != null) return result
        }

        val normalized = normalizeArabic(trimmed)
        if (normalized.length < 3) return null

        // 3. Direct contains match against known popular Ayahs
        for (entry in knownAyahs) {
            if ((normalized.length >= 10 && entry.normalizedText.contains(normalized)) ||
                (entry.normalizedText.length >= 12 && normalized.contains(entry.normalizedText))) {
                return MatchResult(
                    surahName = entry.surahName,
                    ayahNumber = entry.ayahNumber,
                    pageNumber = entry.pageNumber,
                    description = "تطابق نص الآية الكريمة: سورة ${entry.surahName} (الآية ${entry.ayahNumber})"
                )
            }
        }

        // 4. Check if user typed or pasted Surah name directly (e.g., "سورة البقرة" or "الكهف")
        // Require explicit "سوره <Surah>" prefix OR short query matching a standalone Surah name
        val normWords = normalized.split(" ").filter { it.isNotBlank() }
        for (surah in QuranDataHelper.surahs.sortedByDescending { it.nameArabic.length }) {
            val normalizedSurah = normalizeArabic(surah.nameArabic)
            val explicitPrefix = normalized.contains("سوره $normalizedSurah")
            val isExactShortQuery = normalized == normalizedSurah ||
                (normalizedSurah.length >= 4 && normWords.size <= 3 && normWords.contains(normalizedSurah))

            if (explicitPrefix || isExactShortQuery) {
                val convertedAscii = convertArabicDigitsToAscii(trimmed)
                val digits = Regex("\\d+").findAll(convertedAscii)
                    .mapNotNull { it.value.toIntOrNull() }
                    .filter { it in 1..surah.totalAyahs }
                    .toList()
                val ayahNum = digits.firstOrNull() ?: 1
                val page = QuranDataHelper.getPageForSurahAndAyah(surah.number, ayahNum)
                return MatchResult(
                    surahName = surah.nameArabic,
                    ayahNumber = ayahNum,
                    pageNumber = page,
                    description = "تم التعرف على سورة ${surah.nameArabic} (الآية $ayahNum - صفحة $page)"
                )
            }
        }

        // 5. High-confidence partial word overlap match (filtering out common stop words)
        val inputWords = normalized.split(" ").filter { it.length >= 4 && !stopWords.contains(it) }
        if (inputWords.size >= 3) {
            var bestScore = 0
            var bestEntry: AyahEntry? = null
            for (entry in knownAyahs) {
                var score = 0
                for (word in inputWords) {
                    if (entry.normalizedText.contains(word)) score++
                }
                if (score > bestScore && score >= 3) {
                    bestScore = score
                    bestEntry = entry
                }
            }
            if (bestEntry != null) {
                return MatchResult(
                    surahName = bestEntry.surahName,
                    ayahNumber = bestEntry.ayahNumber,
                    pageNumber = bestEntry.pageNumber,
                    description = "تطابق كلمي موثوق: سورة ${bestEntry.surahName} (الآية ${bestEntry.ayahNumber})"
                )
            }
        }

        return null
    }

    /**
     * Context-aware matcher that searches all 6,236 Quran verses in OfflineQuranDatabase
     * using both normalized text and Alef-agnostic Uthmani skeletons.
     */
    fun findAyahMatch(context: android.content.Context, rawText: String): MatchResult? {
        val fastMatch = findAyahMatch(rawText)
        if (fastMatch != null) return fastMatch

        val trimmed = rawText.trim()
        val normalized = normalizeArabic(trimmed)
        if (normalized.length < 4) return null

        val allVerses = OfflineQuranDatabase.getAllVerses(context)
        val inputSkeleton = normalized.split(" ").joinToString(" ") { toUthmaniSkeleton(it) }

        // 1. Direct or Uthmani-skeleton substring match across all 6,236 verses
        for (v in allVerses) {
            val vSkeleton = v.normalizedTextAr.split(" ").joinToString(" ") { toUthmaniSkeleton(it) }
            if ((normalized.length >= 8 && v.normalizedTextAr.contains(normalized)) ||
                (v.normalizedTextAr.length >= 10 && normalized.contains(v.normalizedTextAr)) ||
                (inputSkeleton.length >= 8 && vSkeleton.contains(inputSkeleton))
            ) {
                return MatchResult(
                    surahName = v.surahNameAr,
                    ayahNumber = v.ayahNumber,
                    pageNumber = v.page,
                    description = "تطابق مباشر من قاعدة بيانات المصحف: سورة ${v.surahNameAr} (آية ${v.ayahNumber} • صفحة ${v.page})"
                )
            }
        }

        // 2. Word & Uthmani-skeleton overlap across all 6,236 verses
        val inputWords = normalized.split(" ").filter { it.length >= 3 && !stopWords.contains(it) }
        val inputSkels = inputWords.map { toUthmaniSkeleton(it) }.filter { it.length >= 3 }.toSet()
        if (inputWords.size >= 2) {
            var bestScore = 0
            var bestVerse: OfflineVerse? = null
            for (v in allVerses) {
                var score = 0
                val vWords = v.normalizedTextAr.split(" ")
                val vSkels = vWords.map { toUthmaniSkeleton(it) }.toSet()
                for (w in inputWords) {
                    val wSkel = toUthmaniSkeleton(w)
                    if (vWords.contains(w)) {
                        score += 3
                    } else if (wSkel.length >= 3 && vSkels.contains(wSkel)) {
                        score += 2
                    } else if (v.normalizedTextAr.contains(w)) {
                        score += 1
                    }
                }
                if (score > bestScore && score >= 4) {
                    bestScore = score
                    bestVerse = v
                }
            }
            if (bestVerse != null) {
                return MatchResult(
                    surahName = bestVerse.surahNameAr,
                    ayahNumber = bestVerse.ayahNumber,
                    pageNumber = bestVerse.page,
                    description = "تم التعرف من قاعدة بيانات المصحف: سورة ${bestVerse.surahNameAr} (آية ${bestVerse.ayahNumber} • صفحة ${bestVerse.page})"
                )
            }
        }

        return null
    }

    private fun matchSurahAndAyah(rawSurahName: String, rawAyahDigits: String, prefixDesc: String): MatchResult? {
        val cleanSurah = normalizeArabic(rawSurahName)
            .removePrefix("سورة ")
            .removePrefix("سوره ")
            .trim()

        val matchedSurah = QuranDataHelper.surahs.find { s ->
            val normName = normalizeArabic(s.nameArabic)
            normName == cleanSurah
        } ?: QuranDataHelper.surahs.find { s ->
            val normName = normalizeArabic(s.nameArabic)
            normName.contains(cleanSurah) || cleanSurah.contains(normName)
        } ?: return null

        val asciiDigits = convertArabicDigitsToAscii(rawAyahDigits)
        val ayahNum = asciiDigits.toIntOrNull()?.coerceIn(1, matchedSurah.totalAyahs) ?: 1
        val page = QuranDataHelper.getPageForSurahAndAyah(matchedSurah.number, ayahNum)

        return MatchResult(
            surahName = matchedSurah.nameArabic,
            ayahNumber = ayahNum,
            pageNumber = page,
            description = "$prefixDesc: سورة ${matchedSurah.nameArabic} (الآية $ayahNum - صفحة $page)"
        )
    }
}
