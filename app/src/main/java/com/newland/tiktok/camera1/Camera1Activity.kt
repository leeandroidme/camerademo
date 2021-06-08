package com.newland.tiktok.camera1

import android.content.Context
import android.hardware.Camera
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
class Camera1Activity : BaseActivity(), SurfaceHolder.Callback {
    @BindView(R.id.camera_preview)
    lateinit var surfaceView: SurfaceView
    private var mCamera: Camera? = null
    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mSupportedPreviewSizes: List<Camera.Size>
    private var mCurrentIndex: Int = 0;
    private val mPictureCallback = Camera.PictureCallback { data, camera ->
        data.run {
            mCamera?.stopPreview()
            getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.path?.let {
                Log.e("=====>", it)
            }
            var filepath: String =
                com.newland.tiktok.utils.FileUtils.getExterPath(
                    this@Camera1Activity,
                    "${System.currentTimeMillis()}.png"
                )
            var file = File(filepath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            var fos = FileOutputStream(filepath)
            fos.write(data)
            fos.flush()
            fos.close()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_camera1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSurfaceHolder = surfaceView.holder.apply {
            addCallback(this@Camera1Activity)
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

    fun displayRotation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
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
                mCamera?.apply {
                    takePicture(null, null, mPictureCallback)
                }
            }
        }
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