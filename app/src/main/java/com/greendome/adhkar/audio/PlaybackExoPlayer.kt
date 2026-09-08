package com.greendome.adhkar.audio

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * نفس شبكة OkHttp المستخدمة لتحميل المحتوى، حتى روابط فايربيس لا تُعاد ترميزها
 * كما يحدث مع مصدر HTTP الافتراضي لمشغّل الوسائط.
 */
internal object PlaybackExoPlayer {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun create(context: Context): ExoPlayer {
        val appContext = context.applicationContext
        val http = OkHttpDataSource.Factory(httpClient)
            .setUserAgent("Sabbih")
        val dataSource = DefaultDataSource.Factory(appContext, http)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build()
    }
}
