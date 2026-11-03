package com.example.ui.screens

import android.content.Context
import android.content.Intent

object ReflectionShareUtil {

    fun shareReflectionCard(
        context: Context,
        surahName: String,
        pageNumber: Int,
        reflectionText: String
    ) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                """
                📖 *ورد ومختصر تدبر اليوم من تطبيق محرابي*
                
                📍 *موقع التوقف:* سورة $surahName - الصفحة $pageNumber
                
                $reflectionText
                
                ---
                📱 *تطبيق محرابي - وِردُكَ.. خلوتُكَ.. ومحرابُك*
                """.trimIndent()
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة ورد اليوم والتدبر"))
    }
}
