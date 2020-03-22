package ru.vasiliev.sandbox.camera.data.result;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import ru.vasiliev.sandbox.camera.data.action.CameraAction;
import ru.vasiliev.sandbox.legacycamera.camera2.Camera2Metadata;
import ru.vasiliev.sandbox.legacycamera.camera2.Camera2Result;

public class CameraResult implements Parcelable, Comparable<CameraResult> {

    public static final Creator<CameraResult> CREATOR = new Creator<CameraResult>() {
        @Override
        public CameraResult createFromParcel(Parcel in) {
            return new CameraResult(in);
        }

        @Override
        public CameraResult[] newArray(int size) {
            return new CameraResult[size];
        }
    };
    private CameraAction action;
    private String barcode;
    private String photoBase64;
    private Camera2Metadata metadata;

    private CameraResult(@NonNull CameraAction action, String barcode, String photoBase64, Camera2Metadata metadata) {
        this.action = action;
        this.barcode = barcode;
        this.photoBase64 = photoBase64;
        this.metadata = metadata;
    }

    protected CameraResult(Parcel in) {
        action = in.readParcelable(CameraAction.class.getClassLoader());
        barcode = in.readString();
        photoBase64 = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(action, flags);
        dest.writeString(barcode);
        dest.writeString(photoBase64);
    }

    @Override
    public int compareTo(CameraResult anotherResult) {
        return Integer.compare(action.getOrder(),
                               anotherResult.getAction()
                                       .getOrder());
    }

    public CameraAction getAction() {
        return action;
    }

    public boolean hasBarcode() {
        return barcode != null && !barcode.equals("");
    }

    public String getBarcode() {
        return barcode;
    }

    boolean hasPhoto() {
        return photoBase64 != null && !photoBase64.equals("");
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public boolean hasMetadata() {
        return metadata != null;
    }

    public Camera2Metadata getMetadata() {
        return metadata;
    }

    public static class Builder {

        private CameraAction action;
        private String barcode;
        private String photoBase64;
        private Camera2Metadata metadata;

        public Builder(@NonNull CameraAction action) {
            this.action = action;
        }

        public Builder setCamera2Result(Camera2Result camera2Result) {
            if (camera2Result != null) {
                this.barcode = camera2Result.getBarcode();
                this.photoBase64 = camera2Result.getImageBase64();
                this.metadata = camera2Result.getMetadata();
            }
            return this;
        }

        public Builder setBarcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public Builder setPhotoBase64(String photoBase64) {
            this.photoBase64 = photoBase64;
            return this;
        }

        public Builder setMetadata(Camera2Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public CameraResult build() {
            return new CameraResult(action, barcode, photoBase64, metadata);
        }
    }
}
