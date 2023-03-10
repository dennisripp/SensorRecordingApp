package dev.ostfalia.iotcam.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Size
import androidx.core.content.ContentProviderCompat.requireContext

class CameraInfo {

    companion object {

        public data class CameraInfo(
            val name: String,
            val cameraId: String,
            val size: Size,
            val fps: Int)

        fun returnPossibleCameraConfigs(context: Context): List<dev.ostfalia.iotcam.camera2.CameraInfo.Companion.CameraInfo> {
            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraList = enumerateVideoCameras(cameraManager)

            return cameraList
        }

        fun returnfilteredSortedCameraConfigs(context: Context, cameraID : String) : List<dev.ostfalia.iotcam.camera2.CameraInfo.Companion.CameraInfo> {
            val camInfo = returnPossibleCameraConfigs(context)

            var resList: List<CameraInfo> = camInfo.filter { s -> s.cameraId == cameraID }
            resList = resList.sortedBy { it.size.width }

            return resList
        }


        /** Converts a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int) = when (value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        /** Lists all video-capable cameras and supported resolution and FPS combinations */
        @SuppressLint("InlinedApi")
        public fun enumerateVideoCameras(cameraManager: CameraManager): List<CameraInfo> {
            val availableCameras: MutableList<CameraInfo> = mutableListOf()

            // Iterate over the list of cameras and add those with high speed video recording
            //  capability to our output. This function only returns those cameras that declare
            //  constrained high speed video recording, but some cameras may be capable of doing
            //  unconstrained video recording with high enough FPS for some use cases and they will
            //  not necessarily declare constrained high speed video capability.
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!)


                // Query the available capabilities and output formats
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val cameraConfig = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

                // Return cameras that declare to be backward compatible
                if (capabilities.contains(
                        CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                    // Recording should always be done in the most efficient format, which is
                    //  the format native to the camera framework
                    val targetClass = MediaRecorder::class.java

                    // For each size, list the expected FPS
                    cameraConfig.getOutputSizes(targetClass).forEach { size ->
                        // Get the number of seconds that each frame will take to process
                        val secondsPerFrame =
                            cameraConfig.getOutputMinFrameDuration(targetClass, size) /
                                    1_000_000_000.0
                        // Compute the frames per second to let user select a configuration
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        val fpsLabel = if (fps > 0) "$fps" else "N/A"
                        availableCameras.add(CameraInfo(
                            "$orientation ($id) $size $fpsLabel FPS", id, size, fps))
                    }
                }
            }

            return availableCameras
        }
    }
}