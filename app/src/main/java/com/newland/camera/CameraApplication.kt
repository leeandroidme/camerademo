package com.newland.camera

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

/**
 * @author: leellun
 * @data: 2021/6/7.
 *
 */
class CameraApplication : Application() {
    companion object {
        private lateinit var mApplication: CameraApplication
        fun getApplication(): CameraApplication = mApplication
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