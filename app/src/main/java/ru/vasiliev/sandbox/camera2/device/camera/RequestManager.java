package ru.vasiliev.sandbox.camera2.device.camera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import java.util.List;

public class RequestManager {

    private CameraDevice cameraDevice;

    private CameraConfig cameraConfig;

    private CaptureRequest previewRequest;

    RequestManager(CameraDevice cameraDevice, CameraConfig cameraConfig) {
        this.cameraDevice = cameraDevice;
        this.cameraConfig = cameraConfig;
    }

    CaptureRequest createPreviewRequest(List<Surface> outputSurfaces) throws CameraAccessException {
        CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        if (outputSurfaces != null) {
            for (Surface surface : outputSurfaces) {
                previewRequestBuilder.addTarget(surface);
            }
        }
        setup3AControls(previewRequestBuilder);
        return (previewRequest = previewRequestBuilder.build());
    }

    CaptureRequest getPreviewRequest() {
        return previewRequest;
    }

    private void setup3AControls(CaptureRequest.Builder requestBuilder) {
        // Enable auto-magical 3A run by cameraDevice device
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        if (cameraConfig.isAfSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, cameraConfig.getAfMode());
        }

        if (cameraConfig.isAeSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, cameraConfig.getAeMode(false));
        }

        if (cameraConfig.isAwbSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }
}
