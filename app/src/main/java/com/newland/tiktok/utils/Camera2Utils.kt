package com.newland.tiktok.utils

import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata

/**
 * @author: leellun
 * @data: 2021/6/9.
 *
 */
class Camera2Utils {
    companion object {
        fun getFirstCameraIdFacing(
            cameraManager: CameraManager,
            facing: Int = CameraMetadata.LENS_FACING_BACK
        ): String {
            var comeraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                    ?: false
            }
            comeraIds.forEach {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                    return it
                }
            }
            return comeraIds.first()
        }

        fun filterCompatibleCameras(cameraManager: CameraManager): List<String> {
            return cameraManager.cameraIdList.filter {
                var characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                    ?: false
            }
        }

        fun filterCameraIdsFacing(
            cameraIds: List<String>,
            cameraManager: CameraManager,
            facing: Int
        ): List<String> {
            return cameraIds.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                characteristics.get(CameraCharacteristics.LENS_FACING) == facing
            }
        }
        fun getNextCameraId(cameraManager: CameraManager,currCameraId: String?=null):String?{
            val cameraIds= filterCompatibleCameras(cameraManager)
            val backCameras= filterCameraIdsFacing(cameraIds,cameraManager,CameraMetadata.LENS_FACING_BACK)
            val frontCameras= filterCameraIdsFacing(cameraIds,cameraManager,CameraMetadata.LENS_FACING_FRONT)
            val externalCameras= filterCameraIdsFacing(cameraIds,cameraManager,CameraMetadata.LENS_FACING_EXTERNAL)
            val allCameras= (externalCameras+ listOf(backCameras.firstOrNull(),frontCameras.firstOrNull()))
            val cameraIndex = allCameras?.indexOf(currCameraId)
            return if (cameraIndex == -1) {
                allCameras.getOrNull(0)
            } else {
                allCameras.getOrNull((cameraIndex + 1) % allCameras.size)
            }
        }
    }
}