package com.newland.tiktok.camera2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import butterknife.BindView
import com.newland.tiktok.BaseActivity
import com.newland.tiktok.R

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class Camera2Activity : BaseActivity() {
    override fun getLayoutId(): Int = R.layout.activity_camera2

    @BindView(R.id.camera_preview)
    lateinit var surfaceView: SurfaceView
    var mCameraDevice: CameraDevice? = null
    lateinit var cameraId: String;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //Connect to system camera.
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        var cameraId = manager.cameraIdList[0]
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                mCameraDevice=camera
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, null)

        var width = 640
        var height = 480
        //Render image data onto surface.
        val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

// Remember to call this only *after* SurfaceHolder.Callback.surfaceCreated()
        val previewSurface = surfaceView.holder.surface
        val imReaderSurface = imageReader.surface
        val targets = listOf(previewSurface, imReaderSurface)

// Create a capture session using the predefined targets; this also involves defining the
// session state callback to be notified of when the session is ready
        mCameraDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                // Do something with `session`
            }

            // Omitting for brevity...
            override fun onConfigureFailed(session: CameraCaptureSession) = Unit
        }, null)
    }
}