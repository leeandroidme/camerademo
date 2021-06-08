package com.newland.tiktok.camera1

import android.content.Context
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import butterknife.BindView
import butterknife.OnClick
import com.newland.tiktok.BaseActivity
import com.newland.tiktok.BuildConfig
import com.newland.tiktok.R
import java.io.File
import java.io.FileOutputStream

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class Camera11Activity : BaseActivity(), SurfaceHolder.Callback {
    @BindView(R.id.camera_preview)
    lateinit var surfaceView: SurfaceView
    private var mCamera: Camera? = null
    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mSupportedPreviewSizes: List<Camera.Size>

    private var mCurrentIndex: Int = 0;
    private var mMediaRecorder: MediaRecorder? = null
    var isRecording = false

    override fun getLayoutId(): Int = R.layout.activity_camera11
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSurfaceHolder = surfaceView.holder.apply {
            addCallback(this@Camera11Activity)
            setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        if (mCamera == null) {
            mCamera = Camera.open()
            mCamera?.apply {
                mSupportedPreviewSizes = parameters.supportedPreviewSizes
                for (size: Camera.Size in mSupportedPreviewSizes) {
                    surfaceView.layoutParams = LinearLayout.LayoutParams(size.width, size.height)
                    break
                }
                setDisplayOrientation(90);
            }
        }
    }

    @OnClick(R.id.button_capture, R.id.exchange, R.id.takephone)
    fun onClick(view: View) {
        when (view.id) {
            R.id.button_capture -> mCamera?.apply {
                startPreview()
            }
            R.id.exchange -> {
                var numbers: Int = Camera.getNumberOfCameras()
                if (numbers > 1) {
                    stopCamera();
                    mCurrentIndex = (mCurrentIndex + 1) % numbers
                    mCamera = Camera.open(mCurrentIndex)
                    mCamera?.setDisplayOrientation(90);
                    showCamera();
                }
            }
            R.id.takephone -> {
                if (isRecording) {
                    mMediaRecorder?.stop() // stop the recording
                    releaseMediaRecorder() // release the MediaRecorder object
                    mCamera?.lock() // take camera access back from MediaRecorder
                    isRecording = false
                } else {
                    isRecording = true
                    initMediaRecorder()
                    mMediaRecorder?.prepare()
                    mMediaRecorder?.start()
                }
            }
        }
    }

    private fun initMediaRecorder() {
        mMediaRecorder = MediaRecorder()
        mCamera?.let { camera ->
            camera?.unlock()
            mMediaRecorder?.run {
                setCamera(mCamera)
                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))

                // Step 4: Set output file
                setOutputFile(
                    com.newland.tiktok.utils.FileUtils.getExterPath(
                        this@Camera11Activity,
                        "${System.currentTimeMillis()}.mp4"
                    ).toString()
                )

                setPreviewDisplay(mSurfaceHolder.surface)
                setOrientationHint(90)
                //setProfile相当于下面三个
//                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
//                setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
            }
        }
    }

    private fun releaseMediaRecorder() {
        mMediaRecorder?.reset() // clear recorder configuration
        mMediaRecorder?.release() // release the recorder object
        mMediaRecorder = null
        mCamera?.lock() // lock camera for later use
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mCamera?.apply {
            setPreviewDisplay(holder)
            startPreview()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mSurfaceHolder.surface == null) {
            return
        }
        showCamera();
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        stopCamera();
    }

    private fun showCamera() {
        mCamera?.apply {
            stopPreview()
            setPreviewDisplay(mSurfaceHolder)
            startPreview()
            mSupportedPreviewSizes = parameters.supportedPreviewSizes
            for (size: Camera.Size in mSupportedPreviewSizes) {
                surfaceView.layoutParams = LinearLayout.LayoutParams(size.width, size.height)
                break
            }
        }
    }

    private fun stopCamera() {
        mCamera?.apply {
            stopPreview()
            release()
            mCamera = null;
        }
    }


}