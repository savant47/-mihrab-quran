package com.example.data.model

data class ReadingLog(
    val logId: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val surah: String = "",
    val ayah: Int = 1,
    val pagesReadToday: Int = 0,
    val reflectionSummary: String = ""
)

