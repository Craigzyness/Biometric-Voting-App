package com.example.biometricvotingapp

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // For release builds, you might plant a different tree that logs to Crashlytics or does nothing.
        // Timber.plant(ReleaseTree()) // Example for release

        // IMPORTANT FOR RELEASE: Ensure the correct google-services.json for the
        // production Firebase project is in place before building a release.
        // Different Firebase projects might be used for debug vs. release builds.
        // Initialize Firebase Crashlytics
        // Disable Crashlytics collection for debug builds to prevent noise during development
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        if (BuildConfig.DEBUG) {
            Timber.d("MainApplication onCreate: Crashlytics collection enabled: ${!BuildConfig.DEBUG}")
        }
    }
}

// Example ReleaseTree for Timber (optional, place in its own file or here if simple)
// class ReleaseTree : Timber.Tree() {
//     override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
//         if (priority == Log.ERROR || priority == Log.WARN) {
//             FirebaseCrashlytics.getInstance().log(message)
//             t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
//         }
//         // Add other conditions or reporting to other services if needed
//     }
// }
