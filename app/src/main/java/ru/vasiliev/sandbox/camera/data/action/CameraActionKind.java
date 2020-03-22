package ru.vasiliev.sandbox.camera.data.action;

import org.jetbrains.annotations.NotNull;

public enum CameraActionKind {
    PHOTO("PHOTO"),
    BARCODE("BARCODE"),
    PHOTO_AND_BARCODE("PHOTO_AND_BARCODE");

    private String code;

    CameraActionKind(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @NotNull
    @Override
    public String toString() {
        return code;
    }
}
