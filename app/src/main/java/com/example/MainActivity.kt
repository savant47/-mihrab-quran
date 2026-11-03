package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.LocalKhatmaStorage
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KhatmaTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhatmaUiState
import com.example.ui.viewmodel.KhatmaViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val khatmaViewModel: KhatmaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.example.ai.configureAppCheck(this, intent)
            Log.i("MainActivity", "FirebaseApp & AppCheck configured in MainActivity.onCreate")
        } catch (e: Throwable) {
            Log.e("MainActivity", "FirebaseApp initialization failed in MainActivity", e)
        }
        enableEdgeToEdge()

        // Keep screen on while user is reading Quran
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            KhatmaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            authViewModel = authViewModel,
                            khatmaViewModel = khatmaViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    khatmaViewModel: KhatmaViewModel
) {
    val context = LocalContext.current
    val localStorage = remember { LocalKhatmaStorage.getInstance(context) }

    val authUiState by authViewModel.uiState.collectAsState()
    val khatmaUiState by khatmaViewModel.uiState.collectAsState()
    val readingLogs by khatmaViewModel.readingLogs.collectAsState()
    val adminAllUsers by khatmaViewModel.adminAllUsers.collectAsState()
    val isGeneratingReflection by khatmaViewModel.isGeneratingReflection.collectAsState()
    val showCompletionDialog by khatmaViewModel.showCompletionDialog.collectAsState()
    val postLogReflectionDialogData by khatmaViewModel.postLogReflectionDialogData.collectAsState()

    var showSplashScreen by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(900)
        showSplashScreen = false
    }

    var currentScreen by remember {
        mutableStateOf(
            if (localStorage.skipWelcomeScreen && localStorage.hasEnteredBefore) "dashboard" else "welcome"
        )
    }

    LaunchedEffect(Unit) {
        khatmaViewModel.observeUserData()
    }

    if (showSplashScreen) {
        SplashScreen()
    } else {
        when (currentScreen) {
            "welcome" -> {
                BackHandler(enabled = localStorage.hasEnteredBefore) {
                    currentScreen = "dashboard"
                }
                AuthScreen(
                    isLoading = false,
                    onEnterClicked = { readerName, goalPages, skipFuture ->
                        khatmaViewModel.updateReaderName(readerName)
                        authViewModel.updateReaderProfile(readerName, goalPages)
                        currentScreen = "dashboard"
                    },
                    onGuestSignInClicked = {
                        currentScreen = "dashboard"
                    }
                )
            }

            "admin" -> {
                BackHandler { currentScreen = "dashboard" }
                AdminDashboardScreen(
                    usersList = adminAllUsers,
                    onNavigateBack = { currentScreen = "dashboard" }
                )
            }

            else -> {
                val kState = khatmaUiState
                val activeUser = (authUiState as? AuthUiState.Authenticated)?.guestUser
                    ?: (kState as? KhatmaUiState.Success)?.user
                    ?: com.example.data.model.User(
                        uid = "local_quran_reader",
                        email = "",
                        displayName = localStorage.guestUserFlow.value.displayName.ifBlank { "القارئ المبارك" },
                        role = "user",
                        currentKhatmaNumber = 1,
                        currentSurah = "الفاتحة",
                        currentAyah = 1,
                        currentPage = 1,
                        completedRubCount = 0,
                        progressPercentage = 0.0
                    )

                val activeTrack = (kState as? KhatmaUiState.Success)?.activeTrack ?: com.example.data.model.KhatmaTrack(
                    trackId = "main",
                    trackName = "الختمة الرئيسية",
                    currentSurah = "الفاتحة",
                    currentAyah = 1,
                    currentPage = 1,
                    completedRubCount = 0,
                    progressPercentage = 0.0
                )

                val tracksList = (kState as? KhatmaUiState.Success)?.tracks ?: listOf(activeTrack)
                val activeMetrics = (kState as? KhatmaUiState.Success)?.metrics ?: com.example.data.QuranDataHelper.calculateMetrics(activeTrack.currentPage)
                val reflectionText = (kState as? KhatmaUiState.Success)?.latestReflection ?: "مرحباً بك في محرابي (إعداد آسر الشربيني). وِردُكَ.. خلوتُكَ.. ومحرابُك. أدخل وردك اليومي للبدء بالتدبر والدعاء."

                MainDashboardScreen(
                    user = activeUser,
                    activeTrack = activeTrack,
                    tracks = tracksList,
                    metrics = activeMetrics,
                    latestReflection = reflectionText,
                    readingLogs = readingLogs,
                    isGeneratingReflection = isGeneratingReflection,
                    showCompletionDialog = showCompletionDialog,
                    postLogReflectionData = postLogReflectionDialogData,
                    onDismissPostLogReflectionDialog = { khatmaViewModel.dismissPostLogReflectionDialog() },
                    onSelectTrack = { trackId -> khatmaViewModel.selectTrack(trackId) },
                    onCreateTrack = { name -> khatmaViewModel.createNewTrack(name) },
                    onRenameTrack = { id, name -> khatmaViewModel.renameTrack(id, name) },
                    onAutoSaveStopPoint = { surah, ayah, page ->
                        khatmaViewModel.autoSaveStopPointSilently(surah, ayah, page)
                    },
                    onLogProgress = { surah, ayah, page, pagesRead ->
                        khatmaViewModel.logReadingProgress(surah, ayah, page, pagesRead)
                    },
                    onStartNextKhatma = { khatmaViewModel.startNextKhatma() },
                    onDismissCompletionDialog = { khatmaViewModel.dismissCompletionDialog() },
                    onNavigateToAdmin = { currentScreen = "admin" },
                    onNavigateToWelcome = { currentScreen = "welcome" },
                    onSignOut = { currentScreen = "welcome" }
                )
            }
        }
    }
}


