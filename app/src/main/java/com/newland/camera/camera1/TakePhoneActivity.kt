package com.newland.camera.camera1

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.newland.camera.R

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class TakePhoneActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    lateinit var imageView: AppCompatImageView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_phone)
        imageView = findViewById(R.id.imageView)
        findViewById<View>(R.id.btn).setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent->
            intent.resolveActivity(packageManager).also {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
        }
    }
}