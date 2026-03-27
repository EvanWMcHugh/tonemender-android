package com.tonemender.app.data.remote

import android.content.Context
import com.google.gson.GsonBuilder
import com.tonemender.app.BuildConfig
import com.tonemender.app.data.local.cookies.PersistentCookieJar
import com.tonemender.app.data.remote.api.ToneMenderApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    @Volatile
    private var cookieJar: PersistentCookieJar? = null

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var apiInstance: ToneMenderApi? = null

    fun init(context: Context) {
        if (apiInstance != null) return

        synchronized(this) {
            if (apiInstance != null) return

            val appContext = context.applicationContext
            val jar = PersistentCookieJar(appContext)
            val okHttpClient = buildOkHttpClient(jar)
            val retrofitInstance = buildRetrofit(okHttpClient)

            cookieJar = jar
            retrofit = retrofitInstance
            apiInstance = retrofitInstance.create(ToneMenderApi::class.java)
        }
    }

    val api: ToneMenderApi
        get() = apiInstance
            ?: error("NetworkModule.init(context) must be called before using api")

    fun clearSessionCookies() {
        cookieJar?.clear()
    }

    private fun buildOkHttpClient(cookieJar: PersistentCookieJar): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-client-platform", "android")
                    .addHeader("X-ToneMender-Client", "android")
                    .build()

                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }

        return builder.build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().create()
                )
            )
            .build()
    }
}