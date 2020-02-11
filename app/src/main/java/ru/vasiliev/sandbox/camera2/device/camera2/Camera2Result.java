package ru.vasiliev.sandbox.camera2.device.camera2;

import ru.vasiliev.sandbox.common.java.Optional;

public class Camera2Result {

    private String imageBase64;
    private Camera2Metadata metadata;
    private String barcode;

    public Camera2Result(String imageBase64, Camera2Metadata metadata) {
        this.imageBase64 = imageBase64;
        this.metadata = metadata;
    }

    public Camera2Result(String imageBase64, Camera2Metadata metadata, Optional<String> barcodeOpt) {
        this.imageBase64 = imageBase64;
        this.metadata = metadata;
        if (barcodeOpt.isPresent()) {
            this.barcode = barcodeOpt.get();
        }
    }

    public boolean hasImage() {
        return imageBase64 != null;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public Camera2Metadata getMetadata() {
        return metadata;
    }

    public boolean hasBarcode() {
        return barcode != null;
    }

    public String getBarcode() {
        return barcode;
    }

    public void updateBarcode(String barcode) {
        this.barcode = barcode;
    }
}
