package dev.ostfalia.iotcam.camera2
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.view.Surface
import dev.ostfalia.iotcam.utils.AutoFitSurfaceView


class SoftwarePipeline(width: Int, height: Int, fps: Int, filterOn: Boolean,
                       characteristics: CameraCharacteristics, encoder: EncoderWrapper,
                       viewFinder: AutoFitSurfaceView
) : Pipeline(width, height, fps, filterOn,
    characteristics, encoder, viewFinder) {

    override fun createPreviewRequest(session: CameraCaptureSession,
                                      previewStabilization: Boolean): CaptureRequest? {
        // Capture request holds references to target surfaces
        return session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            // Add the preview surface target
            addTarget(viewFinder.holder.surface)

            if (previewStabilization) {
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
            }
        }.build()
    }

    override fun createRecordRequest(session: CameraCaptureSession,
                                     previewStabilization: Boolean): CaptureRequest {
        // Capture request holds references to target surfaces
        return session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            // Add the preview and recording surface targets
            addTarget(viewFinder.holder.surface)
            addTarget(encoder.getInputSurface())

            // Sets user requested FPS for all targets
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))

            if (previewStabilization) {
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
            }
        }.build()
    }

    override fun getTargets(): List<Surface> {
        return listOf(viewFinder.holder.surface, encoder.getInputSurface())
    }
}