package ru.vasiliev.sandbox.camera2.framework.camera2;

public class Camera2Result {

    private String imageBase64;
    private Camera2Metadata metadata;
    private String barcode;

    public Camera2Result(String imageBase64,
                         Camera2Metadata metadata) {
        this.imageBase64 = imageBase64;
        this.metadata = metadata;
    }

    public Camera2Result(String imageBase64,
                         Camera2Metadata metadata,
                         String barcode) {
        this.imageBase64 = imageBase64;
        this.metadata = metadata;
        this.barcode = barcode;
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
