package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

object AppDiagnostics {
    private const val TAG = "AppDiagnostics"
    private const val MAX_LOGS = 250

    private val logEntries = Collections.synchronizedList(mutableListOf<String>())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        log("DIAG", "=== AppDiagnostics initialized ===")
        log("DEVICE", "Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT}, Android ${Build.VERSION.RELEASE})")
    }

    fun log(tag: String, message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] [$tag] $message"
        Log.i(tag, message)
        synchronized(logEntries) {
            if (logEntries.size >= MAX_LOGS) {
                logEntries.removeAt(0)
            }
            logEntries.add(entry)
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = timeFormat.format(Date())
        val stackTrace = throwable?.let { Log.getStackTraceString(it) } ?: ""
        val entry = "[$timestamp] [ERROR/$tag] $message ${if (stackTrace.isNotBlank()) "\nStackTrace:\n$stackTrace" else ""}"
        Log.e(tag, message, throwable)
        synchronized(logEntries) {
            if (logEntries.size >= MAX_LOGS) {
                logEntries.removeAt(0)
            }
            logEntries.add(entry)
        }
    }

    fun getFullReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("=================================================\n")
        sb.append("   تقرير تشخيص تطبيق محرابي (Mihrabi Diagnostics)\n")
        sb.append("   Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("=================================================\n\n")

        sb.append("--- [معلومات الجهاز والنظام / Device Info] ---\n")
        sb.append("Manufacturer: ${Build.MANUFACTURER}\n")
        sb.append("Model: ${Build.MODEL}\n")
        sb.append("Device: ${Build.DEVICE} (${Build.PRODUCT})\n")
        sb.append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("Package Name: ${context.packageName}\n\n")

        sb.append("--- [حالة خدمات Google Play / Play Services] ---\n")
        try {
            val availability = GoogleApiAvailability.getInstance()
            val code = availability.isGooglePlayServicesAvailable(context)
            val isAvailable = (code == ConnectionResult.SUCCESS)
            sb.append("Play Services Available: $isAvailable (code: $code, ${availability.getErrorString(code)})\n")
        } catch (e: Throwable) {
            sb.append("Play Services Check Error: ${e.message}\n")
        }

        sb.append("\n--- [إعدادات وتكوين Firebase / Firebase Config] ---\n")
        try {
            val defaultClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Throwable) {
                "NotFound"
            }
            sb.append("Configured Web Client ID: $defaultClientId\n")
            val currentUser = try {
                Firebase.auth.currentUser
            } catch (e: Throwable) {
                null
            }
            if (currentUser != null) {
                sb.append("Current Auth User: ${currentUser.email} (UID: ${currentUser.uid})\n")
            } else {
                sb.append("Current Auth User: None (Logged Out / Unauthenticated)\n")
            }
        } catch (e: Throwable) {
            sb.append("Firebase Status Check Error: ${e.message}\n")
        }

        sb.append("\n--- [سجل الأحداث المباشر / Live Event Logs] ---\n")
        synchronized(logEntries) {
            if (logEntries.isEmpty()) {
                sb.append("(No logs recorded yet)\n")
            } else {
                for (line in logEntries) {
                    sb.append(line).append("\n")
                }
            }
        }

        sb.append("\n================ [نهاية التقرير] ================\n")
        return sb.toString()
    }

    fun copyToClipboard(context: Context): Boolean {
        return try {
            val report = getFullReport(context)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mihrabi Diagnostic Report", report)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم نسخ تقرير التشخيص بنجاح إلى الحافظة", Toast.LENGTH_LONG).show()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed copying to clipboard", e)
            Toast.makeText(context, "تعذر النسخ: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareReport(context: Context) {
        try {
            val report = getFullReport(context)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "تقرير تشخيص تطبيق محرابي")
                putExtra(Intent.EXTRA_TEXT, report)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "مشاركة تقرير الأخطاء والتشخيص")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed sharing report", e)
            Toast.makeText(context, "تعذر المشاركة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun clear() {
        synchronized(logEntries) {
            logEntries.clear()
        }
    }
}
