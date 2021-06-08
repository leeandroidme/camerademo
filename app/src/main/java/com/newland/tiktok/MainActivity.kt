package com.newland.tiktok

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import butterknife.OnClick
import com.newland.tiktok.camera1.Camera11Activity
import com.newland.tiktok.camera1.Camera1Activity
import com.newland.tiktok.camera1.TakePhoneActivity
import com.newland.tiktok.camera1.TakeVedioPhoneActivity
import com.newland.tiktok.network.RetrofitService
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : BaseActivity() {
    override fun getLayoutId(): Int = R.layout.activity_main

    @OnClick(R.id.btn1, R.id.btn2, R.id.btn3,R.id.btn4)
    fun onClick(view: View) {
        //检查相机
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return
        }
        when (view.id) {
            R.id.btn1 -> startActivity(Intent(this, TakePhoneActivity::class.java))
            R.id.btn2 -> startActivity(Intent(this, TakeVedioPhoneActivity::class.java))
            R.id.btn3 -> startActivity(Intent(this, Camera1Activity::class.java))
            R.id.btn4 -> startActivity(Intent(this, Camera11Activity::class.java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val files = getExternalFilesDirs(Environment.DIRECTORY_PICTURES)
        val call: Call<ResponseBody>? = RetrofitService.getInstance()?.getApiService()
            ?.getImage("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fwx1.sinaimg.cn%2Flarge%2F008fHVgdly4gqfhftvhl5j30u00iv40g.jpg&refer=http%3A%2F%2Fwx1.sinaimg.cn&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1625648552&t=53e529f57b27f621c94d9d06b5fc2a08")
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.body() == null) return
                val file = files[0]
                Log.e("aaaaaaaaaaaaaaaa", file.absolutePath)
                val inputStream: InputStream? = response?.body()?.byteStream()
                inputStream?.let {
                    val outputStream = FileOutputStream(File(file, "img1.png"))
                    var length: Int = 0
                    val buff = ByteArray(1024)
                    while (inputStream.read(buff).also { length = it } != -1) {
                        outputStream.write(buff, 0, length)
                    }
                    inputStream.close()
                    outputStream.flush()
                    outputStream.close()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            }
        })
        arrayOf("234234", "sdf")
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA,Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO), 1)
    }

}