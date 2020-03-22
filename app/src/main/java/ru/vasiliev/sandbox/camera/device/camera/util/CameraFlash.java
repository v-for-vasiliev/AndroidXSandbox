package ru.vasiliev.sandbox.camera.device.camera.util;

import android.hardware.camera2.CaptureRequest;

public enum CameraFlash {

    FLASH_OFF(CaptureRequest.FLASH_MODE_OFF, CaptureRequest.CONTROL_AE_MODE_ON),
    FLASH_ON(CaptureRequest.FLASH_MODE_OFF, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH),
    FLASH_TORCH(CaptureRequest.FLASH_MODE_TORCH, CaptureRequest.CONTROL_AE_MODE_ON),
    FLASH_AUTO(CaptureRequest.FLASH_MODE_OFF, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH),
    FLASH_RED_EYE(CaptureRequest.FLASH_MODE_OFF, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);

    private int flashMode;
    private int aeMode;

    CameraFlash(int flashMode, int aeMode) {

    }

    public int getFlashMode() {
        return flashMode;
    }

    public int getAeMode() {
        return aeMode;
    }
}
