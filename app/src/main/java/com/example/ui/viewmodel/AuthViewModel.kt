package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.LocalKhatmaStorage
import com.example.data.model.User
import com.example.data.repository.KhatmaRepository
import com.example.util.AppDiagnostics
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(
        val user: FirebaseUser?,
        val isMasterAdmin: Boolean,
        val isGuest: Boolean = false,
        val guestUser: User? = null
    ) : AuthUiState
    data class Unauthenticated(val errorMessage: String? = null) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val localStorage = LocalKhatmaStorage.getInstance(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
        const val MASTER_ADMIN_EMAIL = "savant47@gmail.com"
        const val FALLBACK_WEB_CLIENT_ID = "1022433742444-1g0vsp92i04juefrir8tv67n6evpbm10.apps.googleusercontent.com"
    }

    private fun ensureFirebaseInitialized(): Boolean {
        return try {
            if (FirebaseApp.getApps(getApplication()).isEmpty()) {
                val app = FirebaseApp.initializeApp(getApplication())
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:1022433742444:android:716de845df9815aa359249")
                        .setApiKey("AIzaSyBmvycY_CIu_AodzDHO8Qhw_kMEroFndxc")
                        .setProjectId("gen-lang-client-0751808412")
                        .setStorageBucket("gen-lang-client-0751808412.firebasestorage.app")
                        .setGcmSenderId("1022433742444")
                        .build()
                    val explicitApp = FirebaseApp.initializeApp(getApplication(), options)
                    AppDiagnostics.log(TAG, "Firebase initialized via explicit fallback: ${explicitApp.name}")
                } else {
                    AppDiagnostics.log(TAG, "Firebase initialized via standard options: ${app.name}")
                }
            }
            true
        } catch (e: Throwable) {
            AppDiagnostics.logError(TAG, "ensureFirebaseInitialized error: ${e.message}", e)
            false
        }
    }

    init {
        AppDiagnostics.log(TAG, "Initializing AuthViewModel and checking session...")
        ensureFirebaseInitialized()
        checkCurrentAuthSession()

        // Reactive AuthStateListener: Instantly transition to Authenticated state as soon as Firebase detects a signed-in user
        try {
            ensureFirebaseInitialized()
            Firebase.auth.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null && _uiState.value !is AuthUiState.Authenticated) {
                    AppDiagnostics.log(TAG, "AuthStateListener detected authenticated user: ${fbUser.email}. Immediate UI transition triggered.")
                    localStorage.isGuestMode = false
                    val isMaster = isEmailMasterAdmin(fbUser.email)
                    _uiState.value = AuthUiState.Authenticated(
                        user = fbUser,
                        isMasterAdmin = isMaster,
                        isGuest = false
                    )
                }
            }
        } catch (e: Throwable) {
            AppDiagnostics.logError(TAG, "Error attaching AuthStateListener: ${e.message}", e)
        }
    }

    fun checkCurrentAuthSession() {
        try {
            ensureFirebaseInitialized()
            val currentFirebaseUser = try {
                Firebase.auth.currentUser
            } catch (e: Throwable) {
                null
            }

            if (currentFirebaseUser != null) {
                AppDiagnostics.log(TAG, "Active session restored for: ${currentFirebaseUser.email}")
                val isMaster = isEmailMasterAdmin(currentFirebaseUser.email)
                _uiState.value = AuthUiState.Authenticated(
                    user = currentFirebaseUser,
                    isMasterAdmin = isMaster,
                    isGuest = false
                )
                return
            }
        } catch (e: Throwable) {
            AppDiagnostics.logError(TAG, "Error checking Firebase current user", e)
        }

        // Default to local cache mode directly (100% offline, zero sign-in required)
        val localUser = localStorage.guestUserFlow.value
        AppDiagnostics.log(TAG, "Active local cache mode session restored for: ${localUser.displayName}")
        _uiState.value = AuthUiState.Authenticated(
            user = null,
            isMasterAdmin = true,
            isGuest = true,
            guestUser = localUser
        )
    }

    /**
     * Bypasses Google Sign-In completely and activates local offline mode.
     */
    fun continueAsGuest() {
        AppDiagnostics.log(TAG, "User entered local cache mode")
        localStorage.isGuestMode = true
        val guest = localStorage.guestUserFlow.value
        _uiState.value = AuthUiState.Authenticated(
            user = null,
            isMasterAdmin = true,
            isGuest = true,
            guestUser = guest
        )
    }

    fun updateReaderProfile(name: String, goalPages: Int) {
        localStorage.updateReaderProfile(name, goalPages)
        val updated = localStorage.guestUserFlow.value
        _uiState.value = AuthUiState.Authenticated(
            user = null,
            isMasterAdmin = true,
            isGuest = true,
            guestUser = updated
        )
    }

    fun isEmailMasterAdmin(email: String?): Boolean {
        return email?.trim()?.equals(MASTER_ADMIN_EMAIL, ignoreCase = true) == true
    }

    /**
     * Clean, modern Google Sign-In implementation with CredentialManager.
     * Transitions instantly to Authenticated state on credential reception,
     * offloading remote profile initialization to background IO.
     */
    fun signInWithGoogle(activity: Activity) {
        _uiState.value = AuthUiState.Loading
        AppDiagnostics.log(TAG, "Starting signInWithGoogle flow...")

        viewModelScope.launch {
            try {
                // Dynamically fetch web client ID
                val clientId = try {
                    val id = activity.getString(R.string.default_web_client_id)
                    if (id.isNotBlank() && !id.contains("mockclientid")) id else FALLBACK_WEB_CLIENT_ID
                } catch (e: Throwable) {
                    AppDiagnostics.log(TAG, "Dynamic web client id retrieval failed: ${e.message}. Using fallback.")
                    FALLBACK_WEB_CLIENT_ID
                }

                AppDiagnostics.log(TAG, "Using Web Client ID: $clientId")

                ensureFirebaseInitialized()
                val credentialManager = CredentialManager.create(activity)
                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()

                AppDiagnostics.log(TAG, "Requesting credentials from CredentialManager...")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                AppDiagnostics.log(TAG, "Credential received: ${credential.javaClass.simpleName}, type: ${credential.type}")

                val googleIdToken: String = try {
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } else if (credential is CustomCredential) {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } else {
                        throw IllegalStateException("Unexpected credential type: ${credential.type}")
                    }
                } catch (e: Throwable) {
                    AppDiagnostics.logError(TAG, "Failed standard token extraction. Checking raw bundle: ${e.message}", e)
                    val bundleToken = (credential as? CustomCredential)?.data?.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                        ?: (credential as? CustomCredential)?.data?.getString("id_token")
                    bundleToken ?: throw e
                }

                AppDiagnostics.log(TAG, "Google ID Token obtained successfully (length: ${googleIdToken.length}). Signing in to Firebase...")
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                val user = authResult.user

                if (user != null) {
                    AppDiagnostics.log(TAG, "Firebase signInWithCredential SUCCESS! User: ${user.email} (UID: ${user.uid})")
                    localStorage.isGuestMode = false
                    val isMaster = isEmailMasterAdmin(user.email)

                    // 1. INSTANT ROUTING: Update UI state immediately without awaiting Firestore!
                    _uiState.value = AuthUiState.Authenticated(
                        user = user,
                        isMasterAdmin = isMaster,
                        isGuest = false
                    )

                    // 2. Offload profile sync to background without blocking the UI
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            saveUserInitialProfile(activity, user, isMaster)
                            AppDiagnostics.log(TAG, "Background user profile sync completed for: ${user.email}")
                        } catch (e: Throwable) {
                            AppDiagnostics.logError(TAG, "Background user profile sync warning: ${e.message}", e)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "مرحباً بك: ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw IllegalStateException("Firebase returned null user after successful credential sign-in")
                }
            } catch (e: GetCredentialCancellationException) {
                AppDiagnostics.log(TAG, "Google Sign-In prompt was cancelled by user: ${e.message}")
                _uiState.value = AuthUiState.Unauthenticated()
            } catch (e: Throwable) {
                val detailedError = "${e.javaClass.simpleName}: ${e.localizedMessage ?: e.message}"
                AppDiagnostics.logError(TAG, "Google Sign-In FAILED with exception: $detailedError", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "خطأ تسجيل دخول جوجل: $detailedError", Toast.LENGTH_LONG).show()
                }
                _uiState.value = AuthUiState.Unauthenticated(errorMessage = detailedError)
            }
        }
    }

    private suspend fun saveUserInitialProfile(context: Context, user: FirebaseUser, isMasterAdmin: Boolean) {
        try {
            val repository = KhatmaRepository(context)
            val initialUser = User(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName.orEmpty().ifBlank { if (isMasterAdmin) "آسر الشربيني" else "القارئ المبارك" },
                role = if (isMasterAdmin) "admin" else "user",
                currentKhatmaNumber = 1,
                currentSurah = "الفاتحة",
                currentAyah = 1,
                currentPage = 1,
                completedRubCount = 0,
                progressPercentage = 0.0
            )
            repository.saveUserProfile(initialUser)
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving initial user profile", e)
        }
    }

    fun signOut(context: Context) {
        AppDiagnostics.log(TAG, "Signing out...")
        try {
            ensureFirebaseInitialized()
            if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
                Firebase.auth.signOut()
            }
        } catch (e: Throwable) {
            AppDiagnostics.logError(TAG, "Error during Firebase signOut", e)
        }

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Throwable) {
                AppDiagnostics.logError(TAG, "Error clearing CredentialManager state", e)
            } finally {
                localStorage.clearSession()
                val localUser = localStorage.guestUserFlow.value
                _uiState.value = AuthUiState.Authenticated(
                    user = null,
                    isMasterAdmin = true,
                    isGuest = true,
                    guestUser = localUser
                )
            }
        }
    }
}
