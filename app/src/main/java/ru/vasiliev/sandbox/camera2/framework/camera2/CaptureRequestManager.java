package ru.vasiliev.sandbox.camera2.framework.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import java.util.List;

public class CaptureRequestManager {

    private CameraDevice cameraDevice;

    private Camera2Options camera2Options;

    private CaptureRequest previewRequest;

    CaptureRequestManager(CameraDevice cameraDevice, Camera2Options camera2Options) {
        this.cameraDevice = cameraDevice;
        this.camera2Options = camera2Options;
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

        if (camera2Options.isAutoFocusSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, camera2Options.getAutoFocusMode());
        }

        if (camera2Options.isAutoExposureSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, camera2Options.getAutoExposureMode(false));
        }

        if (camera2Options.isAWBSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }
}
