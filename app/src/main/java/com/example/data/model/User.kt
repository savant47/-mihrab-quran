package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "user", // "user" or "admin"
    val currentKhatmaNumber: Int = 1,
    val currentSurah: String = "الفاتحة",
    val currentAyah: Int = 1,
    val currentPage: Int = 1,
    val completedRubCount: Int = 0,
    val progressPercentage: Double = 0.0,
    @ServerTimestamp val lastActiveDate: Timestamp? = null
) {
    val isAdmin: Boolean
        get() = role.equals("admin", ignoreCase = true) || email.equals("savant47@gmail.com", ignoreCase = true)
}
