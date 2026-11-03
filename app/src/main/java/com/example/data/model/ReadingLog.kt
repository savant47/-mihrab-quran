package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ReadingLog(
    @DocumentId val logId: String = "",
    @ServerTimestamp val date: Timestamp? = null,
    val surah: String = "",
    val ayah: Int = 1,
    val pagesReadToday: Int = 0,
    val reflectionSummary: String = ""
)
