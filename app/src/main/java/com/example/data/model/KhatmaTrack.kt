package com.example.data.model

data class KhatmaTrack(
    val trackId: String = "main",
    val trackName: String = "الختمة الرئيسية",
    val khatmaNumber: Int = 1,
    val currentSurah: String = "الفاتحة",
    val currentAyah: Int = 1,
    val currentPage: Int = 1,
    val completedRubCount: Int = 0,
    val progressPercentage: Double = 0.0,
    val isCompleted: Boolean = false,
    val isFixedSurahWerd: Boolean = false,
    val fixedSurahName: String = "",
    val fixedSurahStartPage: Int = 1
)
