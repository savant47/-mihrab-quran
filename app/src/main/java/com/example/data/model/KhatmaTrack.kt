package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class KhatmaTrack(
    @DocumentId val trackId: String = "main",
    val trackName: String = "الختمة الرئيسية",
    val khatmaNumber: Int = 1,
    val currentSurah: String = "الفاتحة",
    val currentAyah: Int = 1,
    val currentPage: Int = 1,
    val completedRubCount: Int = 0,
    val progressPercentage: Double = 0.0,
    val isCompleted: Boolean = false,
    @ServerTimestamp val startDate: Timestamp? = null,
    @ServerTimestamp val lastUpdated: Timestamp? = null
)
