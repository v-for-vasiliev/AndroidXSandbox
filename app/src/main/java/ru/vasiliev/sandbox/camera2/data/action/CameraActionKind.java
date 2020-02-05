package ru.vasiliev.sandbox.camera2.data.action;

public enum CameraActionKind {
    PHOTO("PHOTO"), BARCODE("BARCODE"), PHOTO_AND_BARCODE("PHOTO_AND_BARCODE");

    private String code;

    CameraActionKind(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
