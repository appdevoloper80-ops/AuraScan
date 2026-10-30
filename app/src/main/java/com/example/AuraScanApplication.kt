package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Application class for AuraScan.
 * Initializes Timber logging and sets up a global UncaughtExceptionHandler
 * to intercept, log, and persist any startup, provider, or runtime crashes.
 */
class AuraScanApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Install early exception handler before ContentProviders initialize
        installCrashHandler(base ?: this)
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Plant Timber tree
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Production tree: logs INFO, WARN, ERROR to Logcat with tag and captures errors
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    val logTag = tag ?: "AuraScan"
                    when (priority) {
                        Log.VERBOSE -> Log.v(logTag, message, t)
                        Log.DEBUG -> Log.d(logTag, message, t)
                        Log.INFO -> Log.i(logTag, message, t)
                        Log.WARN -> Log.w(logTag, message, t)
                        Log.ERROR, Log.ASSERT -> Log.e(logTag, message, t)
                    }
                }
            })
        }

        Timber.i("AuraScanApplication onCreate initialized. App Version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        // Ensure handler is confirmed with full application context
        installCrashHandler(this)
    }

    private fun installCrashHandler(context: Context) {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Avoid wrapping multiple times
        if (existingHandler is AuraScanUncaughtExceptionHandler) {
            return
        }

        Thread.setDefaultUncaughtExceptionHandler(
            AuraScanUncaughtExceptionHandler(context.applicationContext ?: context, existingHandler)
        )
    }

    private class AuraScanUncaughtExceptionHandler(
        private val context: Context,
        private val defaultHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceString = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val crashReport = buildString {
                appendLine("==================== AURASCAN CRASH REPORT ====================")
                appendLine("Timestamp: $timestamp")
                appendLine("Thread: ${thread.name}")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("Fingerprint: ${Build.FINGERPRINT}")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message ?: "No message"}")
                appendLine("----------------- ROOT CAUSE TRACE -----------------")
                appendLine(stackTraceString)
                appendLine("===============================================================")
            }

            // 1. Output to Logcat under high-visibility tags
            Log.e(CRASH_LOG_TAG, crashReport)
            try {
                Timber.tag(CRASH_LOG_TAG).e(throwable, "Fatal uncaught crash on thread %s", thread.name)
            } catch (t: Throwable) {
                Log.e(CRASH_LOG_TAG, "Timber failed to log exception", t)
            }

            // 2. Persist to internal storage file
            try {
                val crashFile = File(context.filesDir, CRASH_FILE_NAME)
                crashFile.writeText(crashReport)
            } catch (e: Throwable) {
                Log.e(CRASH_LOG_TAG, "Failed to write crash log file", e)
            }

            // 3. Persist to SharedPreferences for easy in-app retrieval/display
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_LAST_CRASH_REPORT, crashReport)
                    .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
                    .apply()
            } catch (e: Throwable) {
                Log.e(CRASH_LOG_TAG, "Failed to write crash log to preferences", e)
            }

            // 4. Delegate to the system default handler so Android OS can properly teardown
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_LOG_TAG = "AuraScanCrash"
        const val CRASH_FILE_NAME = "crash_log.txt"
        private const val PREFS_NAME = "aurascan_crash_prefs"
        private const val KEY_LAST_CRASH_REPORT = "key_last_crash_report"
        private const val KEY_LAST_CRASH_TIME = "key_last_crash_time"

        /**
         * Reads the most recent crash report saved from an uncaught exception, if any.
         */
        fun getLatestCrashLog(context: Context): String? {
            return try {
                // Check file first
                val crashFile = File(context.filesDir, CRASH_FILE_NAME)
                if (crashFile.exists() && crashFile.length() > 0) {
                    crashFile.readText()
                } else {
                    // Fallback to shared preferences
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.getString(KEY_LAST_CRASH_REPORT, null)
                }
            } catch (e: Throwable) {
                Timber.w(e, "Could not read crash log")
                null
            }
        }

        /**
         * Clears the persisted crash log after it has been reviewed or sent.
         */
        fun clearCrashLog(context: Context) {
            try {
                val crashFile = File(context.filesDir, CRASH_FILE_NAME)
                if (crashFile.exists()) {
                    crashFile.delete()
                }
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (e: Throwable) {
                Timber.w(e, "Could not clear crash log")
            }
        }
    }
}
