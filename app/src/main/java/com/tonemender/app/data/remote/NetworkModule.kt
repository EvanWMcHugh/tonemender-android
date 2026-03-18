package com.tonemender.app.data.remote

import android.content.Context
import com.google.gson.GsonBuilder
import com.tonemender.app.BuildConfig
import com.tonemender.app.data.local.cookies.PersistentCookieJar
import com.tonemender.app.data.remote.api.ToneMenderApi
import com.tonemender.app.data.remote.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private var cookieJar: PersistentCookieJar? = null
    private var retrofit: Retrofit? = null
    private var apiInstance: ToneMenderApi? = null

    fun init(context: Context) {
        if (apiInstance != null) return

        cookieJar = PersistentCookieJar(context.applicationContext)

        val okHttpClientBuilder = OkHttpClient.Builder()
            .cookieJar(cookieJar!!)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val requestWithHeaders = originalRequest.newBuilder()
                    .addHeader("X-ToneMender-Client", "android")
                    .build()

                chain.proceed(requestWithHeaders)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        apiInstance = retrofit!!.create(ToneMenderApi::class.java)
    }

    val api: ToneMenderApi
        get() = apiInstance ?: error("NetworkModule.init(context) must be called before using api")

    fun clearSessionCookies() {
        cookieJar?.clear()
    }
}