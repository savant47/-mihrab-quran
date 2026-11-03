package com.example

import android.app.Application
import com.example.data.OfflineQuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MihrabiApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-warm the unified 6,236-verse offline Quran database in the background
        appScope.launch {
            OfflineQuranDatabase.ensureLoaded(this@MihrabiApplication)
        }
    }
}

