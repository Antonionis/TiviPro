package com.tivipro

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.tivipro.data.network.NetworkModule

class PolTiviApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: PolTiviApplication
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
