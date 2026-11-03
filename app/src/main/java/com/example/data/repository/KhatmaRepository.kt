package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.QuranDataHelper
import com.example.data.local.LocalKhatmaStorage
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KhatmaRepository(
    private val db: FirebaseFirestore? = null,
    private val context: Context? = null
) {

    constructor(context: Context) : this(
        db = try {
            val dbId = try {
                val resId = context.applicationContext.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                if (resId != 0) context.applicationContext.getString(resId) else null
            } catch (e: Throwable) {
                null
            }
            if (!dbId.isNullOrBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Throwable) {
            Log.e("KhatmaRepository", "Failed to initialize FirebaseFirestore safely: ${e.message}")
            null
        },
        context = context.applicationContext
    )

    private val localStorage: LocalKhatmaStorage? by lazy {
        context?.let { LocalKhatmaStorage.getInstance(it) }
    }

    companion object {
        private const val TAG = "KhatmaRepository"
    }

    private fun isUnauthenticatedSession(): Boolean {
        if (db == null) return true
        return try {
            Firebase.auth.currentUser == null
        } catch (e: Throwable) {
            true
        }
    }

    fun observeUserProfile(userId: String): Flow<User?> {
        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            val localFlow = localStorage?.guestUserFlow
            if (localFlow != null) {
                return localFlow
            }
            return flowOf(
                User(
                    uid = userId,
                    email = "savant47@gmail.com",
                    displayName = "آسر الشربيني",
                    role = "admin",
                    currentKhatmaNumber = 1,
                    currentSurah = "الفاتحة",
                    currentAyah = 1,
                    currentPage = 1,
                    completedRubCount = 0,
                    progressPercentage = 0.0
                )
            )
        }
        return firestore.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(User::class.java)
            }
            .catch { error ->
                Log.i(TAG, "Observing profile handled gracefully: ${error.message}")
                emit(localStorage?.guestUserFlow?.value)
            }
    }

    suspend fun saveUserProfile(user: User) {
        localStorage?.saveGuestUser(user)

        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            Log.i(TAG, "Unauthenticated session active. Saved profile locally.")
            return
        }
        val userRef = firestore.collection("users").document(user.uid)
        try {
            val isMaster = user.email.equals("savant47@gmail.com", ignoreCase = true)
            val assignedRole = if (isMaster) "admin" else user.role

            val payload = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "role" to assignedRole,
                "currentKhatmaNumber" to user.currentKhatmaNumber,
                "currentSurah" to user.currentSurah,
                "currentAyah" to user.currentAyah,
                "currentPage" to user.currentPage,
                "completedRubCount" to user.completedRubCount,
                "progressPercentage" to user.progressPercentage,
                "lastActiveDate" to FieldValue.serverTimestamp()
            )
            userRef.set(payload).await()
            ensureMainTrackExists(user.uid)
        } catch (e: Exception) {
            Log.w(TAG, "Saving profile exception handled: ${e.message}")
        }
    }

    suspend fun ensureMainTrackExists(userId: String) {
        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) return
        try {
            val mainTrackRef = firestore.collection("users").document(userId).collection("khatma_tracks").document("main")
            val doc = mainTrackRef.get().await()
            if (!doc.exists()) {
                val initialTrack = mapOf(
                    "trackName" to "الختمة الرئيسية",
                    "khatmaNumber" to 1,
                    "currentSurah" to "الفاتحة",
                    "currentAyah" to 1,
                    "currentPage" to 1,
                    "completedRubCount" to 0,
                    "progressPercentage" to 0.0,
                    "isCompleted" to false,
                    "startDate" to FieldValue.serverTimestamp(),
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                mainTrackRef.set(initialTrack).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Handling ensureMainTrackExists exception: ${e.message}")
        }
    }

    fun observeKhatmaTracks(userId: String): Flow<List<KhatmaTrack>> {
        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            val localTracks = localStorage?.tracksFlow
            if (localTracks != null) return localTracks
            return flowOf(
                listOf(
                    KhatmaTrack(
                        trackId = "main",
                        trackName = "الختمة الرئيسية",
                        currentSurah = "الفاتحة",
                        currentAyah = 1,
                        currentPage = 1,
                        completedRubCount = 0,
                        progressPercentage = 0.0
                    )
                )
            )
        }
        return firestore.collection("users").document(userId)
            .collection("khatma_tracks")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(KhatmaTrack::class.java)
                if (list.isEmpty()) {
                    localStorage?.tracksFlow?.value ?: listOf(
                        KhatmaTrack(
                            trackId = "main",
                            trackName = "الختمة الرئيسية",
                            currentSurah = "الفاتحة",
                            currentAyah = 1,
                            currentPage = 1,
                            completedRubCount = 0,
                            progressPercentage = 0.0
                        )
                    )
                } else {
                    list
                }
            }
            .catch { error ->
                Log.i(TAG, "Observing tracks handled gracefully: ${error.message}")
                emit(localStorage?.tracksFlow?.value ?: emptyList())
            }
    }

    suspend fun createKhatmaTrack(userId: String, trackName: String): String {
        val newId = "track_${System.currentTimeMillis()}"
        val newTrack = KhatmaTrack(
            trackId = newId,
            trackName = trackName,
            khatmaNumber = 1,
            currentSurah = "الفاتحة",
            currentAyah = 1,
            currentPage = 1,
            completedRubCount = 0,
            progressPercentage = 0.0,
            isCompleted = false
        )
        val currentTracks = localStorage?.tracksFlow?.value?.toMutableList() ?: mutableListOf()
        currentTracks.add(newTrack)
        localStorage?.saveTracks(currentTracks)

        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) return newId
        return try {
            val tracksRef = firestore.collection("users").document(userId).collection("khatma_tracks")
            val docRef = tracksRef.document(newId)
            val track = mapOf(
                "trackName" to trackName,
                "khatmaNumber" to 1,
                "currentSurah" to "الفاتحة",
                "currentAyah" to 1,
                "currentPage" to 1,
                "completedRubCount" to 0,
                "progressPercentage" to 0.0,
                "isCompleted" to false,
                "startDate" to FieldValue.serverTimestamp(),
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            docRef.set(track).await()
            newId
        } catch (e: Exception) {
            Log.w(TAG, "Creating track exception handled: ${e.message}")
            newId
        }
    }

    suspend fun renameKhatmaTrack(userId: String, trackId: String, newName: String) {
        val currentTracks = localStorage?.tracksFlow?.value?.toMutableList()
        val index = currentTracks?.indexOfFirst { it.trackId == trackId } ?: -1
        if (index >= 0 && currentTracks != null) {
            currentTracks[index] = currentTracks[index].copy(trackName = newName)
            localStorage?.saveTracks(currentTracks)
        }

        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) return
        try {
            firestore.collection("users").document(userId)
                .collection("khatma_tracks").document(trackId)
                .update(
                    mapOf(
                        "trackName" to newName,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Rename track exception handled: ${e.message}")
        }
    }

    suspend fun deleteKhatmaTrack(userId: String, trackId: String) {
        val currentTracks = localStorage?.tracksFlow?.value?.toMutableList()
        val index = currentTracks?.indexOfFirst { it.trackId == trackId } ?: -1
        if (index >= 0 && currentTracks != null && trackId != "main") {
            currentTracks.removeAt(index)
            localStorage?.saveTracks(currentTracks)
        }

        val firestore = db
        if (firestore == null || isUnauthenticatedSession() || trackId == "main") return
        try {
            firestore.collection("users").document(userId)
                .collection("khatma_tracks").document(trackId)
                .delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete track exception handled: ${e.message}")
        }
    }

    suspend fun logReadingProgress(
        userId: String,
        surah: String,
        ayah: Int,
        page: Int,
        pagesReadToday: Int,
        reflectionSummary: String
    ) {
        logReadingProgressForTrack(
            userId = userId,
            trackId = "main",
            surah = surah,
            ayah = ayah,
            page = page,
            pagesReadToday = pagesReadToday,
            reflectionSummary = reflectionSummary
        )
    }

    suspend fun logReadingProgressForTrack(
        userId: String,
        trackId: String,
        surah: String,
        ayah: Int,
        page: Int,
        pagesReadToday: Int,
        reflectionSummary: String
    ) {
        // Always persist to local device cache first
        localStorage?.addReadingLog(
            surah = surah,
            ayah = ayah,
            page = page,
            pagesReadToday = pagesReadToday,
            reflectionSummary = reflectionSummary,
            trackId = trackId
        )

        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            Log.i(TAG, "Local cache updated for reading progress: $surah $ayah p.$page")
            return
        }

        val userRef = firestore.collection("users").document(userId)
        val trackRef = userRef.collection("khatma_tracks").document(trackId)
        val logsRef = userRef.collection("reading_logs")

        val rubCount = QuranDataHelper.pageToRub(page)
        val progressPercentage = (page.toDouble() / QuranDataHelper.TOTAL_PAGES.toDouble()) * 100.0
        val isCompleted = page >= QuranDataHelper.TOTAL_PAGES

        val logEntry = mapOf(
            "date" to FieldValue.serverTimestamp(),
            "surah" to surah,
            "ayah" to ayah,
            "pagesReadToday" to pagesReadToday,
            "reflectionSummary" to reflectionSummary
        )

        try {
            logsRef.add(logEntry).await()

            trackRef.update(
                mapOf(
                    "currentSurah" to surah,
                    "currentAyah" to ayah,
                    "currentPage" to page,
                    "completedRubCount" to rubCount,
                    "progressPercentage" to progressPercentage,
                    "isCompleted" to isCompleted,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
            ).await()

            userRef.update(
                mapOf(
                    "currentSurah" to surah,
                    "currentAyah" to ayah,
                    "currentPage" to page,
                    "completedRubCount" to rubCount,
                    "progressPercentage" to progressPercentage,
                    "lastActiveDate" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Logging progress exception handled: ${e.message}")
        }
    }

    suspend fun startNextKhatmaForTrack(userId: String, trackId: String, currentKhatmaNum: Int) {
        val currentTracks = localStorage?.tracksFlow?.value?.toMutableList()
        val index = currentTracks?.indexOfFirst { it.trackId == trackId } ?: -1
        if (index >= 0 && currentTracks != null) {
            currentTracks[index] = currentTracks[index].copy(
                khatmaNumber = currentKhatmaNum + 1,
                currentSurah = "الفاتحة",
                currentAyah = 1,
                currentPage = 1,
                completedRubCount = 0,
                progressPercentage = 0.0,
                isCompleted = false
            )
            localStorage?.saveTracks(currentTracks)
        }

        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) return
        try {
            val trackRef = firestore.collection("users").document(userId)
                .collection("khatma_tracks").document(trackId)

            trackRef.update(
                mapOf(
                    "khatmaNumber" to (currentKhatmaNum + 1),
                    "currentSurah" to "الفاتحة",
                    "currentAyah" to 1,
                    "currentPage" to 1,
                    "completedRubCount" to 0,
                    "progressPercentage" to 0.0,
                    "isCompleted" to false,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "startNextKhatmaForTrack exception handled: ${e.message}")
        }
    }

    fun observeReadingLogs(userId: String): Flow<List<ReadingLog>> {
        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            return localStorage?.readingLogsFlow ?: flowOf(emptyList())
        }
        return firestore.collection("users").document(userId)
            .collection("reading_logs")
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(ReadingLog::class.java)
                if (list.isEmpty()) {
                    localStorage?.readingLogsFlow?.value ?: emptyList()
                } else {
                    list
                }
            }
            .catch { error ->
                Log.i(TAG, "Observe reading logs exception handled: ${error.message}")
                emit(localStorage?.readingLogsFlow?.value ?: emptyList())
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<User>> {
        val firestore = db
        if (firestore == null || isUnauthenticatedSession()) {
            // Absolutely no fake/mock users! Return empty list or local guest profile.
            return flowOf(emptyList())
        }
        return firestore.collection("users")
            .orderBy("lastActiveDate", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(User::class.java)
            }
            .catch { error ->
                Log.i(TAG, "Observe all users exception handled: ${error.message}")
                emit(emptyList())
            }
    }
}
