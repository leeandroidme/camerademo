package com.newland.tiktok.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import butterknife.BindView
import com.newland.tiktok.BaseActivity
import com.newland.tiktok.R
import java.util.concurrent.Executor

/**
 * 封装了3组输出目标的助手类型定义:
 *
 *   1. 逻辑 camera
 *   2. 第一个物理camera
 *   3. 第二个物理camera
 */
typealias DualCameraOutputs =
        Triple<MutableList<Surface>?, MutableList<Surface>?, MutableList<Surface>?>
/**
 * 多相机API支持（因手机不支持未作测试）
 * @author: leellun
 * @data: 2021/6/10.
 *
 */
class DualCameraActivity : BaseActivity() {
    @BindView(R.id.camera_preview)
    lateinit var surfaceView: SurfaceView
    @BindView(R.id.camera_preview2)
    lateinit var surfaceView2: SurfaceView

    override fun getLayoutId(): Int = R.layout.activity_dual_camera2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView.holder.addCallback(object:SurfaceHolder.Callback{
            @RequiresApi(Build.VERSION_CODES.P)
            override fun surfaceCreated(holder: SurfaceHolder?) {
                initCamera()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }

        })
    }
    @RequiresApi(Build.VERSION_CODES.P)
    fun initCamera() {
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 获得两个输出目标
        val surface1 = surfaceView.holder.surface
        val surface2 = surfaceView2.holder.surface

        val dualCamera = findShortLongCameraPair(cameraManager)!!
        val outputTargets = DualCameraOutputs(
            null, mutableListOf<Surface>(surface1), mutableListOf<Surface>(surface2)
        )

        //打开逻辑摄像机，配置输出并创建一个会话
        createDualCameraSession(cameraManager, dualCamera, targets = outputTargets) { session ->

            // 创建一个单独的请求，每个物理摄像机都有一个目标,每个目标只从其关联的物理摄像机接收帧
            val requestTemplate = CameraDevice.TEMPLATE_PREVIEW
            val captureRequest = session.device.createCaptureRequest(requestTemplate).apply {
                arrayOf(surface1, surface2).forEach { addTarget(it) }
            }.build()

            session.setRepeatingRequest(captureRequest, null, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun createDualCameraSession(
        cameraManager: CameraManager,
        dualCamera: DualCamera,
        targets: DualCameraOutputs,
        executor: Executor = AsyncTask.SERIAL_EXECUTOR,
        callback: (CameraCaptureSession) -> Unit
    ) {

        // 创建3组输出配置:一组用于逻辑摄像机，一组用于每个物理摄像机。
        val outputConfigsLogical = targets.first?.map { OutputConfiguration(it) }
        val outputConfigsPhysical1 = targets.second?.map {
            OutputConfiguration(it).apply { setPhysicalCameraId(dualCamera.physicalId1) }
        }
        val outputConfigsPhysical2 = targets.third?.map {
            OutputConfiguration(it).apply { setPhysicalCameraId(dualCamera.physicalId2) }
        }

        // 将所有输出配置放入一个Array中
        val outputConfigsAll = arrayOf(
            outputConfigsLogical, outputConfigsPhysical1, outputConfigsPhysical2
        )
            .filterNotNull().flatMap { it }

        // 实例化可用于创建会话的会话配置
        val sessionConfiguration = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
            outputConfigsAll, executor, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) = callback(session)
                override fun onConfigureFailed(session: CameraCaptureSession) =
                    session.device.close()
            })

        // 使用前面定义的函数打开逻辑摄像机
        openDualCamera(cameraManager, dualCamera, executor = executor) {
            // 最后创建会话并通过回调返回
            it.createCaptureSession(sessionConfiguration)
        }
    }

    /**
     * 打开摄像头连接
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun openDualCamera(
        cameraManager: CameraManager,
        dualCamera: DualCamera,
        executor: Executor = AsyncTask.SERIAL_EXECUTOR,
        callback: (CameraDevice) -> Unit
    ) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        cameraManager.openCamera(
            dualCamera.logicalId, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = callback(device)
                override fun onError(device: CameraDevice, error: Int) = onDisconnected(device)
                override fun onDisconnected(device: CameraDevice) = device.close()
            })
    }

    /**
     * 用于封装一个逻辑摄像机和两个底层物理摄像机
     */
    data class DualCamera(val logicalId: String, val physicalId1: String, val physicalId2: String)

    @RequiresApi(Build.VERSION_CODES.P)
    fun findDualCameras(manager: CameraManager, facing: Int? = null): List<DualCamera> {
        val dualCameras: MutableList<DualCamera> = mutableListOf<DualCamera>()
        // 迭代所有可用的相机特性
        manager.cameraIdList.map {
            Pair(manager.getCameraCharacteristics(it), it)
        }.filter {
            // 由面向要求方向的摄像机进行滤镜
            facing == null || it.first.get(CameraCharacteristics.LENS_FACING) == facing
        }.filter {
            //逻辑相机滤镜(相机设备是由两个或更多物理相机支持的逻辑相机)
            it.first.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            )
        }.forEach {
            // 物理摄像机列表中所有可能的对都是有效结果,可能有N个物理相机作为一个逻辑相机分组的一部分
            val physicalCameras = it.first.physicalCameraIds.toTypedArray()
            for (idx1 in 0 until physicalCameras.size) {
                for (idx2 in (idx1 + 1) until physicalCameras.size) {
                    dualCameras.add(
                        DualCamera(
                            it.second, physicalCameras[idx1], physicalCameras[idx2]
                        )
                    )
                }
            }
        }

        return dualCameras
    }

    /**
     * 查找多相机
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun findShortLongCameraPair(manager: CameraManager, facing: Int? = null): DualCamera? {

        return findDualCameras(manager, facing).map {
            val characteristics1 = manager.getCameraCharacteristics(it.physicalId1)
            val characteristics2 = manager.getCameraCharacteristics(it.physicalId2)

            // 查询物理相机的焦距
            val focalLengths1 = characteristics1.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            ) ?: floatArrayOf(0F)
            val focalLengths2 = characteristics2.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            ) ?: floatArrayOf(0F)

            //计算最小焦距和最大焦距之间的最大差值
            val focalLengthsDiff1 = focalLengths2.maxOrNull()!! - focalLengths1.minOrNull()!!
            val focalLengthsDiff2 = focalLengths1.maxOrNull()!! - focalLengths2.minOrNull()!!

            //返回一对相机id和最小和最大焦距之间的差
            if (focalLengthsDiff1 < focalLengthsDiff2) {
                Pair(DualCamera(it.logicalId, it.physicalId1, it.physicalId2), focalLengthsDiff1)
            } else {
                Pair(DualCamera(it.logicalId, it.physicalId2, it.physicalId1), focalLengthsDiff2)
            }

        //只返回差异最大的对，如果没有找到对，则返回null
        }.maxByOrNull { it.second }?.first
    }

}