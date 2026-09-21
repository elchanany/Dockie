package com.dockie.app

import android.app.Application
import com.dockie.app.data.DockieRepository
import com.dockie.app.notify.NotificationController

class DockieApp : Application() {
    lateinit var repository: DockieRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DockieRepository(applicationContext)
        NotificationController.ensureChannel(this)
    }
}
