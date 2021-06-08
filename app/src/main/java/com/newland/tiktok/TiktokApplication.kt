package com.newland.tiktok

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

/**
 * @author: leellun
 * @data: 2021/6/7.
 *
 */
class TiktokApplication : Application() {
    companion object {
        private lateinit var mApplication: TiktokApplication
        fun getApplication(): TiktokApplication = mApplication
    }

    override fun onCreate() {
        super.onCreate()
        mApplication = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
        }
    }
}