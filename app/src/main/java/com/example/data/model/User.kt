package com.example.data.model

data class User(
    val uid: String = "local_quran_reader",
    val email: String = "",
    val displayName: String = "القارئ المبارك",
    val role: String = "user",
    val currentKhatmaNumber: Int = 1,
    val currentSurah: String = "الفاتحة",
    val currentAyah: Int = 1,
    val currentPage: Int = 1,
    val completedRubCount: Int = 0,
    val progressPercentage: Double = 0.0
)

