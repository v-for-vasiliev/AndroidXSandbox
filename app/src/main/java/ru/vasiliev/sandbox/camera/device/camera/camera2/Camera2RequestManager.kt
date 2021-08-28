package ru.vasiliev.sandbox.camera.device.camera.camera2

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import ru.vasiliev.sandbox.camera.device.camera.common.CameraPreview
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash
import java.util.*

internal class Camera2RequestManager(
    private val camera2Controller: Camera2Controller,
    private val camera2Config: Camera2Config,
    private val cameraPreview: CameraPreview
) {

    private fun setup3AControls(
        requestBuilder: CaptureRequest.Builder, autoFocus: Boolean, cameraFlash: CameraFlash,
        autoWhiteBalance: Boolean
    ) {
        // Enable auto-magical 3A run
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

        // Auto focus
        if (autoFocus && camera2Config.isAfSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, camera2Config.optimalAfMode)
        } else {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        }

        // Flash and auto exposure
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, cameraFlash.aeMode)
        requestBuilder.set(CaptureRequest.FLASH_MODE, cameraFlash.flashMode)

        // Auto white balance
        if (autoWhiteBalance && camera2Config.isAwbSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        } else {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        }
    }

    private fun setupJpegOrientation(
        requestBuilder: CaptureRequest.Builder, cameraFacing: CameraFacing,
        displayOrientation: Int
    ) {
        requestBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            (camera2Config.cameraSensorOrientation + displayOrientation * if (cameraFacing == CameraFacing.FRONT) 1 else -1 + 360) % 360
        )
    }

    /**
     * @return Preview request builder with actual preview params from controller
     * - Auto-focus
     * - Flash
     * - Auto white-balance
     * @throws CameraAccessException when camera is not accessible
     */
    @Throws(CameraAccessException::class)
    fun newPreviewRequestBuilder(): PreviewRequestBuilder {
        return PreviewRequestBuilder()
    }

    /**
     * @return Capture request builder with actual preview params from linked controller:
     * - Auto-focus
     * - Flash
     * - Auto white-balance
     * - Camera facing
     * - Display rotation
     * @throws CameraAccessException when camera is not accessible
     */
    @Throws(CameraAccessException::class)
    fun newCaptureRequestBuilder(captureConfigProvider: CaptureConfigProvider): CaptureRequestBuilder {
        return CaptureRequestBuilder()
    }

    interface CaptureConfigProvider {
        var autoFocus: Boolean
        var cameraFlash: CameraFlash
        var autoWhiteBalance: Boolean
    }

    inner class PreviewRequestBuilder(private val captureConfigProvider: CaptureConfigProvider) {
        private val previewRequestBuilder: CaptureRequest.Builder =
            camera2Controller.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        private var outputSurfaces: MutableList<Surface> = ArrayList()
        private var autoFocus: Boolean = camera2Controller.getAutoFocus()
        private var cameraFlash: CameraFlash = camera2Controller.getFlash()
        private var autoWhiteBalance: Boolean = camera2Controller.getAutoWhiteBalance()
        fun setOutputSurface(outputSurface: Surface): PreviewRequestBuilder {
            outputSurfaces.add(outputSurface)
            return this
        }

        fun setOutputSurfaces(outputSurfaces: MutableList<Surface>): PreviewRequestBuilder {
            this.outputSurfaces = outputSurfaces
            return this
        }

        fun setAutoFocus(autoFocus: Boolean): PreviewRequestBuilder {
            this.autoFocus = autoFocus
            return this
        }

        fun setCameraFlash(cameraFlash: CameraFlash): PreviewRequestBuilder {
            this.cameraFlash = cameraFlash
            return this
        }

        fun setAutoWhiteBalance(autoWhiteBalance: Boolean): PreviewRequestBuilder {
            this.autoWhiteBalance = autoWhiteBalance
            return this
        }

        fun <T> setKeyValue(key: CaptureRequest.Key<T>, value: T): PreviewRequestBuilder {
            previewRequestBuilder[key] = value
            return this
        }

        fun build(): CaptureRequest {
            setup3AControls(previewRequestBuilder, autoFocus, cameraFlash, autoWhiteBalance)
            for (surface in outputSurfaces) {
                previewRequestBuilder.addTarget(surface)
            }
            return previewRequestBuilder.build()
        }

    }

    inner class CaptureRequestBuilder private constructor() {
        private val previewRequestBuilder: CaptureRequest.Builder
        private var outputSurfaces: MutableList<Surface> = ArrayList()
        private var autoFocus: Boolean = camera2Controller.getAutoFocus()
        private var flash: CameraFlash = camera2Controller.getFlash()
        private var autoWhiteBalance: Boolean = camera2Controller.getAutoWhiteBalance()
        private var cameraFacing: CameraFacing = camera2Controller.getFacing()
        private var displayOrientation = cameraPreview.displayOrientation
        fun setOutputSurface(outputSurface: Surface): CaptureRequestBuilder {
            outputSurfaces.add(outputSurface)
            return this
        }

        fun setOutputSurfaces(outputSurfaces: MutableList<Surface>): CaptureRequestBuilder {
            this.outputSurfaces = outputSurfaces
            return this
        }

        fun setAutoFocus(autoFocus: Boolean): CaptureRequestBuilder {
            this.autoFocus = autoFocus
            return this
        }

        fun setFlash(flash: CameraFlash): CaptureRequestBuilder {
            this.flash = flash
            return this
        }

        fun setAutoWhiteBalance(autoWhiteBalance: Boolean): CaptureRequestBuilder {
            this.autoWhiteBalance = autoWhiteBalance
            return this
        }

        fun setDisplayOrientation(
            cameraFacing: CameraFacing,
            displayOrientation: Int
        ): CaptureRequestBuilder {
            this.cameraFacing = cameraFacing
            this.displayOrientation = displayOrientation
            return this
        }

        fun <T> setKeyValue(key: CaptureRequest.Key<T>, value: T): CaptureRequestBuilder {
            previewRequestBuilder[key] = value
            return this
        }

        fun build(): CaptureRequest {
            setup3AControls(previewRequestBuilder, autoFocus, flash, autoWhiteBalance)
            setupJpegOrientation(previewRequestBuilder, cameraFacing, displayOrientation)
            for (surface in outputSurfaces) {
                previewRequestBuilder.addTarget(surface)
            }
            return previewRequestBuilder.build()
        }

        init {
            previewRequestBuilder =
                camera2Controller.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }
    }
}