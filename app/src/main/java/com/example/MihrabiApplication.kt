package com.example

import android.app.Application
import android.util.Log
import com.example.util.AppDiagnostics
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MihrabiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            // First attempt standard initialization
            val app = FirebaseApp.initializeApp(this)
            if (app != null) {
                Log.i(TAG, "FirebaseApp initialized via standard options: ${app.name}")
                AppDiagnostics.log(TAG, "FirebaseApp standard initialization SUCCESS: ${app.name}")
                return
            }

            // Fallback: If standard initialization returned null (resources not found), initialize with explicit options
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1022433742444:android:716de845df9815aa359249")
                    .setApiKey("AIzaSyBmvycY_CIu_AodzDHO8Qhw_kMEroFndxc")
                    .setProjectId("gen-lang-client-0751808412")
                    .setStorageBucket("gen-lang-client-0751808412.firebasestorage.app")
                    .setGcmSenderId("1022433742444")
                    .build()
                val explicitApp = FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "FirebaseApp initialized via explicit options: ${explicitApp.name}")
                AppDiagnostics.log(TAG, "FirebaseApp explicit fallback initialization SUCCESS: ${explicitApp.name}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseApp initialization error in Application.onCreate", e)
            AppDiagnostics.logError(TAG, "FirebaseApp initialization failed in Application.onCreate: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MihrabiApplication"
    }
}
