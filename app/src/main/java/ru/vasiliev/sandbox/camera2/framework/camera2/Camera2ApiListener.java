package ru.vasiliev.sandbox.camera2.framework.camera2;

public interface Camera2ApiListener {

    void onCameraError(String message);

    void onBarcodeFound(String barcode);

    void onBarcodeLost();
}