package ru.vasiliev.sandbox.camera2.device.camera2;

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

    CaptureRequest getPreviewRequest(List<Surface> outputSurfaces) throws CameraAccessException {
        CaptureRequest.Builder previewRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        if (outputSurfaces != null) {
            for (Surface surface : outputSurfaces) {
                previewRequest.addTarget(surface);
            }
        }
        setup3AControls(previewRequest);
        return previewRequest.build();
    }

    private void setup3AControls(CaptureRequest.Builder requestBuilder) {
        // Enable auto-magical 3A run by camera device
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        if (cameraConfig.isAutoFocusSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, cameraConfig.getAutoFocusMode());
        }

        if (cameraConfig.isAutoExposureSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, cameraConfig.getAutoExposureMode(false));
        }

        if (cameraConfig.isAWBSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }
}
