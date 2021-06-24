package com.newland.camera.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.OnClick
import com.newland.camera.BaseActivity
import com.newland.camera.R
import com.newland.camera.utils.Camera2Utils
import kotlinx.coroutines.*


/**
 * @author: leellun
 * @data: 2021/6/8.
 * 录像功能
 */
class Camera2VideoActivity : BaseActivity() {
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
    lateinit var surfaceView: SurfaceView

    var mCameraDevice: CameraDevice? = null
    lateinit var mCameraId: String
    var mMediaRecorder: MediaRecorder? = null
    private lateinit var mSurfaceHolder: SurfaceHolder

    lateinit var mainHandler: Handler
    var childHandler: Handler? = null
    var childHandlerThread: HandlerThread? = null
    lateinit var mCameraManager: CameraManager
    var mCameraCaptureSession: CameraCaptureSession? = null

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

    override fun getLayoutId(): Int = R.layout.activity_camera2
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
            R.id.takephone -> switchRecordVideo()
            R.id.exchange -> changeCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder?.resume()
        }
    }

    private var isRecord = false
    private fun switchRecordVideo() {
        if (isRecord) {
            isRecord = false
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            mMediaRecorder = null
            closePreviewSession()
            takePreview()
        } else {
            isRecord = true
            takeVideo()
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

    private fun setMediaRecorder() {
        mMediaRecorder = MediaRecorder()
        mMediaRecorder?.run {
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            var size = Camera2Utils.getPreviewOutputSize(
                windowManager.defaultDisplay,
                mCameraManager.getCameraCharacteristics(mCameraId),
                MediaRecorder::class.java
            )
            var characteristics = mCameraManager.getCameraCharacteristics(mCameraId)
            var streamConfigurationMap =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            var sizes = streamConfigurationMap?.getOutputSizes(MediaRecorder::class.java)
            //获取最大的分辨率大小，通过分辨率大小可以通过CamcorderProfile.get(mcameraid,quality)设置
            var characterSize = sizes?.sortedWith(compareBy {
                it.width * it.height
            })?.last()

            var mProfile = CamcorderProfile.get(mCameraId.toInt(), CamcorderProfile.QUALITY_480P)
            mProfile.videoFrameWidth = size.width
            mProfile.videoFrameHeight = size.height

            mProfile.fileFormat = MediaRecorder.OutputFormat.DEFAULT
            setProfile(mProfile)


            setOutputFile(
                com.newland.camera.utils.FileUtils.getExterPath(
                    this@Camera2VideoActivity,
                    "${System.currentTimeMillis()}.mp4"
                ).toString()
            )
            if (mCameraId.toInt() == CameraMetadata.LENS_FACING_BACK) {
                // 获取手机方向
                val rotation: Int =
                    getWindowManager().getDefaultDisplay().getRotation()
                setOrientationHint(Camera2AutoActivity.ORIENTATIONS[rotation])
            }

            prepare()
        }
    }

    private fun takePreview() {
        val previewSurface = surfaceView.holder.surface
        val targets = listOf(previewSurface)
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
     * 录像
     */
    private fun takeVideo() {
        closePreviewSession()
        setMediaRecorder()
        mCameraDevice?.apply {
            val imReaderSurface = mMediaRecorder?.surface!!
            val surface = surfaceView.holder.surface
            val targets = listOf(imReaderSurface, surface)
            mCameraDevice?.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCameraCaptureSession = session
                        val captureRequest =
                            mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                        captureRequest?.run {
                            captureRequest.addTarget(imReaderSurface)
                            captureRequest.addTarget(surface)
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
                            lifecycleScope.launch(Dispatchers.Main) {
                                mMediaRecorder?.start()
                            }
                            GlobalScope.launch(Dispatchers.Main) {

                            }
                            session.setRepeatingRequest(captureRequest?.build(), null, childHandler)
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }

                },
                childHandler
            )
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
        closePreviewSession()
    }

    private fun closePreviewSession() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession?.close()
            mCameraCaptureSession = null
        }
    }
}