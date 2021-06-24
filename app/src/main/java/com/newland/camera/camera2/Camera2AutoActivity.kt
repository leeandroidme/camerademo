package com.newland.camera.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import butterknife.BindView
import butterknife.OnClick
import com.newland.camera.BaseActivity
import com.newland.camera.R
import com.newland.camera.utils.Camera2Utils
import com.newland.camera.widget.AutoFitSurfaceView
import java.io.FileOutputStream


/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class Camera2AutoActivity : BaseActivity() {
    companion object {
        val ORIENTATIONS: SparseIntArray = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    @BindView(R.id.camera_preview)
    lateinit var surfaceView: AutoFitSurfaceView

    var mCameraDevice: CameraDevice? = null
    lateinit var mCameraId: String
    lateinit var mImageReader: ImageReader
    private lateinit var mSurfaceHolder: SurfaceHolder

    lateinit var mainHandler: Handler
    var childHandler: Handler? = null
    var childHandlerThread: HandlerThread? = null
    lateinit var mCameraManager: CameraManager
    lateinit var mCameraCaptureSession: CameraCaptureSession

    var mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            takePreview()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
        }

        override fun onError(cameraDevice: CameraDevice, p1: Int) {
        }

    }

    override fun getLayoutId(): Int = R.layout.activity_camera2_auto
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        mCameraId = Camera2Utils.getFirstCameraIdFacing(mCameraManager)
        mSurfaceHolder = surfaceView.holder.apply {
            addCallback(object : SurfaceHolder.Callback {
                @SuppressLint("MissingPermission")
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    initCamera()
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                    stopCamera()
                }

            })
            setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }

    @OnClick(R.id.takephone, R.id.exchange)
    fun onClick(view: View) {
        when (view.id) {
            R.id.takephone -> takePicture()
            R.id.exchange -> changeCamera()
        }
    }

    private fun changeCamera() {
        stopCamera();
        mCameraId = Camera2Utils.getNextCameraId(mCameraManager, mCameraId)!!
        initCamera()
    }

    private fun initCamera() {
        childHandlerThread = HandlerThread("Camera2").apply {
            start()
            childHandler = Handler(looper)
        }
        var characteristics = mCameraManager.getCameraCharacteristics(mCameraId)
        var imageSize = Camera2Utils.getPreviewOutputSize(
            windowManager.defaultDisplay,
            characteristics,
            ImageReader::class.java,
            ImageFormat.JPEG
        )
        var showSize = Camera2Utils.getPreviewOutputSize(
            windowManager.defaultDisplay,
            characteristics,
            SurfaceHolder::class.java
        )
        surfaceView.setAspectRatio(showSize.width, showSize.height)

        var width = imageSize.width
        var height = imageSize.height
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
        mImageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {
                Log.e("aaaaaaaaaaaaaaaa=>", reader.toString())
                var image = reader?.acquireNextImage()
                var buffer = image?.planes?.get(0)?.buffer
                var bytes = buffer?.remaining()?.let { ByteArray(it) }
                buffer?.get(bytes)
                var file = com.newland.camera.utils.FileUtils.getExterPath(
                    this@Camera2AutoActivity,
                    "${System.currentTimeMillis()}.jpg"
                )
                var fos = FileOutputStream(file)
                fos.write(bytes)
                fos.flush()
                fos.close()
                Toast.makeText(this@Camera2AutoActivity, "图片保存${file}", Toast.LENGTH_LONG).show()
            }
        }, childHandler)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mainHandler = Handler(getMainLooper());
        mCameraManager.openCamera(mCameraId, mStateCallback, mainHandler)
    }

    private fun takePreview() {
        val previewSurface = surfaceView.holder.surface
        val imReaderSurface = mImageReader.surface
        val targets = listOf(previewSurface, imReaderSurface)

        mCameraDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mCameraDevice?.let {
                    mCameraCaptureSession = session
                    val captureRequest =
                        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(previewSurface)
                    // 自动对焦
                    captureRequest.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    );
                    // 打开闪光灯
                    captureRequest.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    );
                    session.setRepeatingRequest(captureRequest.build(), null, childHandler)
                }
            }

            // Omitting for brevity...
            override fun onConfigureFailed(session: CameraCaptureSession) = Unit
        }, childHandler)
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        mCameraDevice?.apply {
            val captureRequest =
                mCameraCaptureSession.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            val imReaderSurface = mImageReader.surface
            captureRequest.addTarget(imReaderSurface)
            // 自动对焦
            captureRequest.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            );
            // 打开闪光灯
            captureRequest.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            );
            // 获取手机方向
            // 获取手机方向
            val rotation: Int =
                this@Camera2AutoActivity.getWindowManager().getDefaultDisplay().getRotation()
            // 根据设备方向计算设置照片的方向
            // 根据设备方向计算设置照片的方向
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS[rotation])
            mCameraCaptureSession.capture(captureRequest.build(), null, childHandler)
        }
    }

    private fun stopCamera() {
        childHandler = null
        childHandlerThread?.apply {
            quitSafely()
            join()
            childHandlerThread = null
        }
        mCameraDevice?.apply {
            close()
            mCameraDevice = null
        }
    }
}