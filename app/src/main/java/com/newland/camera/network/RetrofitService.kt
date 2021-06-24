package com.newland.camera.network

import android.text.TextUtils
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.newland.camera.CameraApplication
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author: leellun
 * @data: 2021/6/7.
 *
 */
class RetrofitService {
    private var mApiService: ApiService? = null
    public fun getApiService(): ApiService? = mApiService

    companion object {
        @Volatile
        private var mInstance: RetrofitService? = null
        fun getInstance(): RetrofitService? {
            if (mInstance == null) {
                synchronized(RetrofitService::class.java) {
                    if (mInstance == null) {
                        mInstance = RetrofitService()
                    }
                }
            }
            return mInstance
        }
    }

    constructor() {
        var okHttpClient = OkHttpClient.Builder().addNetworkInterceptor(httpLoggingInterceptor)
            .retryOnConnectionFailure(true).connectTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).cookieJar(
                PersistentCookieJar(
                    SetCookieCache(), SharedPrefsCookiePersistor(
                        CameraApplication.getApplication()
                    )
                )
            ).build()
        var retrofitService = Retrofit.Builder().baseUrl("http://www.baidu.com").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
        mApiService = retrofitService.create(ApiService::class.java)
    }

    private val httpLoggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor() {
        if (!TextUtils.isEmpty(it)) {

        }
    }
}