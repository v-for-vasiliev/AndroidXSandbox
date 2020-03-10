package ru.vasiliev.sandbox.camera2.device.camera.util;

import android.hardware.camera2.CameraCharacteristics;

public enum CameraFacing {
    FRONT(CameraCharacteristics.LENS_FACING_FRONT),
    BACK(CameraCharacteristics.LENS_FACING_BACK),
    EXTERNAL(CameraCharacteristics.LENS_FACING_EXTERNAL),
    UNKNOWN(-1);

    private int lensFacing;

    CameraFacing(int lensFacing) {
        this.lensFacing = lensFacing;
    }

    public int getLensFacing() {
        return lensFacing;
    }

    public static CameraFacing byLensFacing(int lensFacing) {
        for (CameraFacing facing : values()) {
            if (facing.getLensFacing() == lensFacing) {
                return facing;
            }
        }
        return UNKNOWN;
    }
}
