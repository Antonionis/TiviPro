package com.goaltv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.goaltv.data.network.NetworkModule

class GoalTVApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: GoalTVApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.getOkHttpClient())
            .allowHardware(false)
            .crossfade(true)
            .build()
    }
}