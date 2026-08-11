package io.github.stream29.dashvoice

import android.app.Application

class DashVoiceApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
