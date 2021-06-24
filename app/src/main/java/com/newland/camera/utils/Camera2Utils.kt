package com.newland.camera.utils

import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Display
import kotlin.math.max
import kotlin.math.min

/**
 * @author: leellun
 * @data: 2021/6/9.
 *
 */
class Camera2Utils {
    companion object {
        /** Standard High Definition size for pictures and video */
        val SIZE_1080P: SmartSize = SmartSize(1920, 1080)
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

        /** Returns a [SmartSize] object for the given [Display] */
        fun getDisplaySmartSize(display: Display): SmartSize {
            val outPoint = Point()
            display.getRealSize(outPoint)
            return SmartSize(outPoint.x, outPoint.y)
        }

        /**
         * Returns the largest available PREVIEW size. For more information, see:
         * https://d.android.com/reference/android/hardware/camera2/CameraDevice
         */
        fun <T>getPreviewOutputSize(
            display: Display,
            characteristics: CameraCharacteristics,
            targetClass: Class<T>,
            format: Int? = null
        ): Size {

            // Find which is smaller: screen or 1080p
            val screenSize = getDisplaySmartSize(display)
            val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
            val maxSize = if (hdScreen) SIZE_1080P else screenSize

            // If image format is provided, use it to determine supported sizes; else use target class
            val config = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            if (format == null)
                assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
            else
                assert(config.isOutputSupportedFor(format))
            val allSizes = if (format == null)
                config.getOutputSizes(targetClass) else config.getOutputSizes(format)

            // Get available sizes and sort them by area from largest to smallest
            val validSizes = allSizes
                .sortedWith(compareBy { it.height * it.width })
                .map { SmartSize(it.width, it.height) }.reversed()

            // Then, get the largest output size that is smaller or equal than our max size
            return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
        }
    }
    class SmartSize(width: Int, height: Int) {
        var size = Size(width, height)
        var long = max(size.width, size.height)
        var short = min(size.width, size.height)
        override fun toString() = "SmartSize(${long}x${short})"
    }


}