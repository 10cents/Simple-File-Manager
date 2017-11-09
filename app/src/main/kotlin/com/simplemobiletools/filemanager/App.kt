package com.simplemobiletools.filemanager

import android.app.Application
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.filemanager.BuildConfig.USE_LEAK_CANARY
import com.simplemobiletools.filemanager.extensions.config
import com.squareup.leakcanary.LeakCanary
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (USE_LEAK_CANARY) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }

        if (config.useEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        Reprint.initialize(this)
    }
}
