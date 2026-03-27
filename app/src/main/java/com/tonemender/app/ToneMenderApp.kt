package com.tonemender.app

import android.app.Application
import com.tonemender.app.data.remote.NetworkModule

class ToneMenderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initNetwork()
    }

    private fun initNetwork() {
        NetworkModule.init(applicationContext)
    }
}