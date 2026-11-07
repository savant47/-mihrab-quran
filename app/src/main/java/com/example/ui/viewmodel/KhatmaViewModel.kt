package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.OnDeviceAiManager
import com.example.data.KhatmaMetrics
import com.example.data.QuranDataHelper
import com.example.data.model.KhatmaTrack
import com.example.data.model.ReadingLog
import com.example.data.model.User
import com.example.data.repository.KhatmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ReflectionDialogData(
    val surah: String,
    val ayah: Int,
    val page: Int,
    val reflectionText: String
)

sealed interface KhatmaUiState {
    data class Success(
        val user: User,
        val activeTrack: KhatmaTrack,
        val tracks: List<KhatmaTrack>,
        val metrics: KhatmaMetrics,
        val latestReflection: String
    ) : KhatmaUiState
    data class Error(val message: String) : KhatmaUiState
}

class KhatmaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KhatmaRepository(application)
    private val aiManager = OnDeviceAiManager(application)

    private val defaultUser = User(
        uid = "local_quran_reader",
        email = "",
        displayName = "القارئ المبارك",
        role = "user",
        currentKhatmaNumber = 1,
        currentSurah = "الفاتحة",
        currentAyah = 1,
        currentPage = 1,
        completedRubCount = 0,
        progressPercentage = 0.0
    )

    private val defaultTrack = KhatmaTrack(
        trackId = "main",
        trackName = "الختمة الرئيسية",
        khatmaNumber = 1,
        currentSurah = "الفاتحة",
        currentAyah = 1,
        currentPage = 1,
        completedRubCount = 0,
        progressPercentage = 0.0
    )

    private val _uiState = MutableStateFlow<KhatmaUiState>(
        KhatmaUiState.Success(
            user = defaultUser,
            activeTrack = defaultTrack,
            tracks = listOf(defaultTrack),
            metrics = QuranDataHelper.calculateMetrics(1),
            latestReflection = "مرحباً بك في محرابي (إعداد آسر الشربيني). وِردُكَ.. خلوتُكَ.. ومحرابُك. أدخل وردك اليومي للبدء بالتدبر والدعاء."
        )
    )
    val uiState: StateFlow<KhatmaUiState> = _uiState.asStateFlow()

    private val _readingLogs = MutableStateFlow<List<ReadingLog>>(emptyList())
    val readingLogs: StateFlow<List<ReadingLog>> = _readingLogs.asStateFlow()

    private val _isGeneratingReflection = MutableStateFlow(false)
    val isGeneratingReflection: StateFlow<Boolean> = _isGeneratingReflection.asStateFlow()

    private val _postLogReflectionDialogData = MutableStateFlow<ReflectionDialogData?>(null)
    val postLogReflectionDialogData: StateFlow<ReflectionDialogData?> = _postLogReflectionDialogData.asStateFlow()

    private val _khatmaTracks = MutableStateFlow<List<KhatmaTrack>>(listOf(defaultTrack))
    val khatmaTracks: StateFlow<List<KhatmaTrack>> = _khatmaTracks.asStateFlow()

    private val _selectedTrackId = MutableStateFlow("main")
    val selectedTrackId: StateFlow<String> = _selectedTrackId.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    companion object {
        private const val TAG = "KhatmaViewModel"
    }

    init {
        try {
            observeUserData()
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing KhatmaViewModel", e)
        }
    }

    fun observeUserData() {
        viewModelScope.launch {
            repository.observeUserProfile().collectLatest { activeUser ->
                val tracks = _khatmaTracks.value
                val currentTrackId = _selectedTrackId.value
                val activeTrack = tracks.find { it.trackId == currentTrackId }
                    ?: tracks.firstOrNull()
                    ?: defaultTrack.copy(
                        currentSurah = activeUser.currentSurah,
                        currentAyah = activeUser.currentAyah,
                        currentPage = activeUser.currentPage,
                        completedRubCount = activeUser.completedRubCount,
                        progressPercentage = activeUser.progressPercentage
                    )

                val metrics = QuranDataHelper.calculateMetrics(activeTrack.currentPage)
                val currentSuccess = _uiState.value as? KhatmaUiState.Success
                val reflectionText = currentSuccess?.latestReflection ?: "مرحباً بك في محرابي (إعداد آسر الشربيني). وِردُكَ.. خلوتُكَ.. ومحرابُك. أدخل وردك اليومي للبدء بالتدبر والدعاء."

                if (!activeTrack.isFixedSurahWerd && (activeTrack.currentPage >= 604 || activeTrack.isCompleted)) {
                    _showCompletionDialog.value = true
                }

                _uiState.value = KhatmaUiState.Success(
                    user = activeUser,
                    activeTrack = activeTrack,
                    tracks = if (tracks.isEmpty()) listOf(defaultTrack) else tracks,
                    metrics = metrics,
                    latestReflection = reflectionText
                )
            }
        }

        viewModelScope.launch {
            repository.observeKhatmaTracks().collectLatest { tracks ->
                val activeTracksList = if (tracks.isEmpty()) listOf(defaultTrack) else tracks
                _khatmaTracks.value = activeTracksList
                val currentState = _uiState.value as? KhatmaUiState.Success
                if (currentState != null) {
                    val activeTrack = activeTracksList.find { it.trackId == _selectedTrackId.value } ?: activeTracksList.first()
                    val metrics = QuranDataHelper.calculateMetrics(activeTrack.currentPage)
                    _uiState.value = currentState.copy(
                        activeTrack = activeTrack,
                        tracks = activeTracksList,
                        metrics = metrics
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observeReadingLogs().collectLatest { logs ->
                _readingLogs.value = logs
            }
        }
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
        val tracks = _khatmaTracks.value
        val track = tracks.find { it.trackId == trackId } ?: defaultTrack
        val currentState = _uiState.value as? KhatmaUiState.Success
        if (currentState != null) {
            val metrics = QuranDataHelper.calculateMetrics(track.currentPage)
            _uiState.value = currentState.copy(
                activeTrack = track,
                metrics = metrics
            )
        }
    }

    fun createNewTrack(trackName: String) {
        viewModelScope.launch {
            try {
                val newId = repository.createKhatmaTrack(trackName)
                selectTrack(newId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new track", e)
            }
        }
    }

    fun createNewFixedSurahTrack(trackName: String, surahName: String, startPage: Int) {
        viewModelScope.launch {
            try {
                val newId = repository.createFixedSurahTrack(trackName, surahName, startPage)
                selectTrack(newId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating fixed surah track", e)
            }
        }
    }

    fun deleteTrack(trackId: String) {
        if (trackId == "main") return
        viewModelScope.launch {
            try {
                repository.deleteKhatmaTrack(trackId)
                if (_selectedTrackId.value == trackId) {
                    selectTrack("main")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting track", e)
            }
        }
    }

    fun renameTrack(trackId: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameKhatmaTrack(trackId, newName)
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming track", e)
            }
        }
    }

    fun logReadingProgress(
        surah: String,
        ayah: Int,
        page: Int,
        pagesReadToday: Int
    ) {
        val currentState = _uiState.value as? KhatmaUiState.Success
        val activeTrack = currentState?.activeTrack ?: defaultTrack
        val userName = currentState?.user?.displayName ?: "القارئ المبارك"

        viewModelScope.launch {
            _isGeneratingReflection.value = true
            try {
                val reflectionText = aiManager.generateQuranicReflection(
                    surahName = surah,
                    ayahNumber = ayah,
                    pageNumber = page,
                    userName = userName
                )

                repository.logReadingProgressForTrack(
                    trackId = activeTrack.trackId,
                    surah = surah,
                    ayah = ayah,
                    page = page,
                    pagesReadToday = pagesReadToday,
                    reflectionSummary = reflectionText
                )

                if (page >= 604) {
                    _showCompletionDialog.value = true
                }

                val updatedTrack = activeTrack.copy(
                    currentSurah = surah,
                    currentAyah = ayah,
                    currentPage = page
                )

                val updatedMetrics = QuranDataHelper.calculateMetrics(page)

                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        activeTrack = updatedTrack,
                        metrics = updatedMetrics,
                        latestReflection = reflectionText
                    )
                }

                // Show the post-log reflection modal with tafsir and story
                _postLogReflectionDialogData.value = ReflectionDialogData(
                    surah = surah,
                    ayah = ayah,
                    page = page,
                    reflectionText = reflectionText
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error logging progress and generating reflection", e)
            } finally {
                _isGeneratingReflection.value = false
            }
        }
    }

    fun autoSaveStopPointSilently(
        surah: String,
        ayah: Int,
        page: Int
    ) {
        val currentState = _uiState.value as? KhatmaUiState.Success
        val activeTrack = currentState?.activeTrack ?: defaultTrack
        if (activeTrack.isFixedSurahWerd) {
            // Fixed Surah Werd (e.g. سورة الكهف) always stays anchored to its Surah start page
            return
        }
        val clampedPage = page.coerceIn(1, 604)

        val updatedTrack = activeTrack.copy(
            currentSurah = surah,
            currentAyah = ayah,
            currentPage = clampedPage,
            completedRubCount = QuranDataHelper.pageToRub(clampedPage),
            progressPercentage = (clampedPage.toDouble() / 604.0) * 100.0
        )
        val updatedMetrics = QuranDataHelper.calculateMetrics(clampedPage)
        if (currentState != null) {
            val updatedTracksList = currentState.tracks.map {
                if (it.trackId == activeTrack.trackId) updatedTrack else it
            }
            _uiState.value = currentState.copy(
                activeTrack = updatedTrack,
                tracks = updatedTracksList,
                metrics = updatedMetrics
            )
        }

        viewModelScope.launch {
            repository.updateTrackCheckpointSilently(
                trackId = activeTrack.trackId,
                surah = surah,
                ayah = ayah,
                page = clampedPage
            )
        }
    }

    fun dismissPostLogReflectionDialog() {
        _postLogReflectionDialogData.value = null
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    fun startNextKhatma() {
        val currentState = _uiState.value as? KhatmaUiState.Success
        val activeTrack = currentState?.activeTrack ?: defaultTrack

        viewModelScope.launch {
            try {
                repository.startNextKhatmaForTrack(
                    trackId = activeTrack.trackId,
                    currentKhatmaNum = activeTrack.khatmaNumber
                )
                _showCompletionDialog.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error starting next Khatma", e)
            }
        }
    }
}

