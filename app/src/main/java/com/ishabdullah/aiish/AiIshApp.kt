package com.ishabdullah.aiish

import android.app.Application
import timber.log.Timber

/**
 * AI Ish Application Class
 * Your private, always-on, super-intelligent companion that never phones home
 */
class AiIshApp : Application() {

    companion object {
        lateinit var instance: AiIshApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("🚀 AI Ish initialized - Your private AI companion is ready")
        Timber.i("   Version: ${BuildConfig.VERSION_NAME}")
        Timber.i("   Package: ${BuildConfig.APPLICATION_ID}")
        Timber.i("   100%% Private | Zero Telemetry | On-Device AI")
    }
}
