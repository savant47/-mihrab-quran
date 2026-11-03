package com.example.data

object IslamicDuaHelper {

    val ROTATING_DUAS: List<String> = listOf(
        "اللهم اغفر لوالدي وارحمهما كما ربياني صغيراً، اللهم اجعل القرآن الكريم ربيع قلوبهم ونور صدورهم، وارزقهما الفردوس الأعلى من الجنة.",
        "اللهم بارك في أهلي وزوجتي وأولادي، واجعلهم قرة عين لي، واهدهم صراطك المستقيم واجعل بيتنا عامراً بذكرك وطاعتك.",
        "اللهم أصلح لي ذريتي، واجعلهم من حفظة كتابك، وأنبتهم نباتاً حسناً، واحمهم من كل سوء وفتنة.",
        "اللهم اغفر لجميع المسلمين والمسلمات، الأحياء منهم والأموات، واجمعنا بهم في جنات النعيم.",
        "اللهم احفظ بيتي وعائلتي من كل مكروه، واكفنا شر الحاقدين والحاسدين، واجمعنا دائماً على الخير والمحبة.",
        "ربّ اجعلني مقيم الصلاة ومن ذريتي ربنا وتقبل دعاء، ربنا اغفر لي ولوالدي وللمؤمنين يوم يقوم الحساب.",
        "اللهم ارزق عائلتي الصحة والعافية، وافتح لهم أبواب رزقك الحلال، ويسر أمورهم في الدنيا والآخرة.",
        "اللهم ألبس والديّ ثوب العافية، واشفهم من كل داء، وأطل في عمرهما على طاعتك ورضاك.",
        "اللهم اجعلنا وأهلنا من المقبولين، الذاكرين الشاكرين، واجمع شملنا في الفردوس الأعلى بلا حساب ولا سابق عذاب.",
        "اللهم احفظ أولادي من فتن الدنيا، واجعلهم هداة مهتدين، وارزقهم العلم النافع والعمل الصالح."
    )

    fun getRandomDua(): String {
        return ROTATING_DUAS.random()
    }

    /**
     * Cleans and formats reflection text to ensure:
     * 1. Section 3 heading is strictly "3. دعاء:"
     * 2. Any old references to "(ضيف الرحمن)" or "للقارئ" are stripped out.
     * 3. A fresh, moving supplication from the 10 supplications list is presented.
     */
    fun sanitizeAndRandomizeDua(rawReflectionText: String): String {
        if (rawReflectionText.isBlank()) return rawReflectionText

        var cleaned = rawReflectionText
            .replace("3. دعاء البر بالوالدين والأهل:", "3. دعاء:")
            .replace("3. دعاء البر بالوالدين:", "3. دعاء:")
            .replace("دعاء البر بالوالدين والأهل:", "دعاء:")

        // If the text contains the section "3. دعاء:"
        if (cleaned.contains("3. دعاء:")) {
            val parts = cleaned.split("3. دعاء:")
            val firstPart = parts[0].trimEnd()
            val newDua = getRandomDua()
            return "$firstPart\n\n3. دعاء:\n$newDua"
        }

        return cleaned
    }
}
