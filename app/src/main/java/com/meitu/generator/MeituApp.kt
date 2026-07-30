package com.meitu.generator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeituApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
