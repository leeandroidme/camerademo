package com.newland.camera.camera1

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.VideoView
import butterknife.BindView
import butterknife.OnClick
import com.newland.camera.BaseActivity
import com.newland.camera.R

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class TakeVedioPhoneActivity : BaseActivity() {
    companion object {
        const val REQUEST_VIDEO_CAPTURE = 1
    }

    @BindView(R.id.videoView)
    lateinit var videoView: VideoView;

    override fun getLayoutId(): Int =R.layout.activity_vedio_phone

    @OnClick(R.id.btn,R.id.btn2)
    fun onClick(view: View) {
        when (view.id) {
            R.id.btn -> Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { intent ->
                intent.resolveActivity(packageManager).also {
                    startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
                }
            }
            R.id.btn2 -> videoView.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            val videoUri: Uri? = data?.data
            videoView.setVideoURI(videoUri)
        }
    }
}