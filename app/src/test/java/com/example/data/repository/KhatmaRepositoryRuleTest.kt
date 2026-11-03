package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class KhatmaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_savesAndObservesProfile() = runBlocking {
        assumeTrue("Firestore emulator is not running", isEmulatorAvailable)
        val uid = signInTestUser("user1@example.com")
        val repository = KhatmaRepository(firestore)

        val testUser = User(
            uid = uid,
            email = "user1@example.com",
            displayName = "قارئ ألوفلاين",
            role = "user",
            currentKhatmaNumber = 1,
            currentSurah = "البقرة",
            currentAyah = 255,
            currentPage = 42,
            completedRubCount = 16,
            progressPercentage = 6.95
        )

        repository.saveUserProfile(testUser)

        val observed = withTimeout(3000L) {
            repository.observeUserProfile(uid).first { it != null }
        }

        assertNotNull(observed)
        assertEquals("user1@example.com", observed?.email)
        assertEquals("البقرة", observed?.currentSurah)
        assertEquals(42, observed?.currentPage)
    }

    @Test
    fun logReadingProgress_authenticatedUser_addsLogAndUpdateProfile() = runBlocking {
        assumeTrue("Firestore emulator is not running", isEmulatorAvailable)
        val uid = signInTestUser("user2@example.com")
        val repository = KhatmaRepository(firestore)

        val initialUser = User(
            uid = uid,
            email = "user2@example.com",
            displayName = "مستخدم جديد",
            role = "user"
        )
        repository.saveUserProfile(initialUser)

        repository.logReadingProgress(
            userId = uid,
            surah = "آل عمران",
            ayah = 100,
            page = 63,
            pagesReadToday = 3,
            reflectionSummary = "تدبر أوفلاين وسورة آل عمران"
        )

        val logs = withTimeout(3000L) {
            repository.observeReadingLogs(uid).first { it.isNotEmpty() }
        }

        assertEquals(1, logs.size)
        assertEquals("آل عمران", logs.first().surah)
        assertEquals(100, logs.first().ayah)

        val updatedProfile = withTimeout(3000L) {
            repository.observeUserProfile(uid).first { it?.currentPage == 63 }
        }
        assertNotNull(updatedProfile)
        assertEquals("آل عمران", updatedProfile?.currentSurah)
    }

    @Test
    fun masterAdmin_savant47_canObserveAllUsers() = runBlocking {
        assumeTrue("Firestore emulator is not running", isEmulatorAvailable)
        val adminUid = signInTestUser("savant47@gmail.com")
        val repository = KhatmaRepository(firestore)

        val adminProfile = User(
            uid = adminUid,
            email = "savant47@gmail.com",
            displayName = "المشرف العام",
            role = "admin"
        )
        repository.saveUserProfile(adminProfile)

        val allUsers = withTimeout(3000L) {
            repository.observeAllUsersForAdmin().first { list -> list.any { it.email == "savant47@gmail.com" } }
        }

        assertNotNull(allUsers)
        assertEquals("savant47@gmail.com", allUsers.first().email)
    }
}
