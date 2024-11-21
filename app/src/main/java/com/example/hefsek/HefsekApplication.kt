package com.example.hefsek

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class HefsekApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
} 