package ru.vasiliev.sandbox.camera2.device.camera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFlash;

public class Camera2RequestManager {

    private CameraDevice cameraDevice;

    private Camera2Config camera2Config;

    Camera2RequestManager(CameraDevice cameraDevice, Camera2Config camera2Config) {
        this.cameraDevice = cameraDevice;
        this.camera2Config = camera2Config;
    }

    private void setup3AControls(CaptureRequest.Builder requestBuilder, boolean autoFocus, CameraFlash cameraFlash,
                                 boolean autoWhiteBalance) {
        // Enable auto-magical 3A run
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        // Auto focus
        if (autoFocus && camera2Config.isAfSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, camera2Config.getOptimalAfMode());
        } else {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }

        // Flash and auto exposure
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, cameraFlash.getAeMode());
        requestBuilder.set(CaptureRequest.FLASH_MODE, cameraFlash.getFlashMode());

        // Auto white balance
        if (autoWhiteBalance && camera2Config.isAwbSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        } else {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
        }
    }

    private void setupJpegOrientation(CaptureRequest.Builder requestBuilder, CameraFacing cameraFacing,
                                      int displayOrientation) {
        requestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                           (camera2Config.getCameraSensorOrientation() +
                            displayOrientation * (cameraFacing == CameraFacing.FRONT ? 1 : -1) + 360) % 360);
    }

    PreviewRequestBuilder newPreviewRequestBuilder() throws CameraAccessException {
        return new PreviewRequestBuilder();
    }

    CaptureRequestBuilder newCaptureRequestBuilder() throws CameraAccessException {
        return new CaptureRequestBuilder();
    }

    public class PreviewRequestBuilder {

        private CaptureRequest.Builder previewRequestBuilder;
        private List<Surface> outputSurfaces = new ArrayList<>();
        private boolean autoFocus = false;
        private CameraFlash cameraFlash = CameraFlash.FLASH_OFF;
        private boolean autoWhiteBalance = false;

        private PreviewRequestBuilder() throws CameraAccessException {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }

        PreviewRequestBuilder setOutputSurface(Surface outputSurface) {
            outputSurfaces.add(outputSurface);
            return this;
        }

        PreviewRequestBuilder setOutputSurfaces(List<Surface> outputSurfaces) {
            this.outputSurfaces = outputSurfaces;
            return this;
        }

        PreviewRequestBuilder setAutoFocus(boolean autoFocus) {
            this.autoFocus = autoFocus;
            return this;
        }

        PreviewRequestBuilder setCameraFlash(CameraFlash cameraFlash) {
            this.cameraFlash = cameraFlash;
            return this;
        }

        PreviewRequestBuilder setAutoWhiteBalance(boolean autoWhiteBalance) {
            this.autoWhiteBalance = autoWhiteBalance;
            return this;
        }

        <T> PreviewRequestBuilder setKeyValue(@NonNull CaptureRequest.Key<T> key, T value) {
            previewRequestBuilder.set(key, value);
            return this;
        }

        public CaptureRequest build() {
            setup3AControls(previewRequestBuilder, autoFocus, cameraFlash, autoWhiteBalance);
            for (Surface surface : outputSurfaces) {
                previewRequestBuilder.addTarget(surface);
            }
            return previewRequestBuilder.build();
        }
    }

    public class CaptureRequestBuilder {

        private CaptureRequest.Builder previewRequestBuilder;
        private List<Surface> outputSurfaces = new ArrayList<>();
        private boolean autoFocus = false;
        private CameraFlash cameraFlash = CameraFlash.FLASH_OFF;
        private boolean autoWhiteBalance = false;
        private CameraFacing cameraFacing = CameraFacing.BACK;
        private int displayOrientation = Surface.ROTATION_0;

        private CaptureRequestBuilder() throws CameraAccessException {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        }

        CaptureRequestBuilder setOutputSurface(Surface outputSurface) {
            outputSurfaces.add(outputSurface);
            return this;
        }

        CaptureRequestBuilder setOutputSurfaces(List<Surface> outputSurfaces) {
            this.outputSurfaces = outputSurfaces;
            return this;
        }

        CaptureRequestBuilder setAutoFocus(boolean autoFocus) {
            this.autoFocus = autoFocus;
            return this;
        }

        CaptureRequestBuilder setCameraFlash(CameraFlash cameraFlash) {
            this.cameraFlash = cameraFlash;
            return this;
        }

        CaptureRequestBuilder setAutoWhiteBalance(boolean autoWhiteBalance) {
            this.autoWhiteBalance = autoWhiteBalance;
            return this;
        }

        CaptureRequestBuilder setDisplayOrientation(CameraFacing cameraFacing, int displayOrientation) {
            this.cameraFacing = cameraFacing;
            this.displayOrientation = displayOrientation;
            return this;
        }

        <T> CaptureRequestBuilder setKeyValue(@NonNull CaptureRequest.Key<T> key, T value) {
            previewRequestBuilder.set(key, value);
            return this;
        }

        public CaptureRequest build() {
            setup3AControls(previewRequestBuilder, autoFocus, cameraFlash, autoWhiteBalance);
            setDisplayOrientation(cameraFacing, displayOrientation);
            for (Surface surface : outputSurfaces) {
                previewRequestBuilder.addTarget(surface);
            }
            return previewRequestBuilder.build();
        }
    }
}
