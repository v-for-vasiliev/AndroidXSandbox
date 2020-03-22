package ru.vasiliev.sandbox.camera.data.action;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

public class CameraAction implements Parcelable, Comparable<CameraAction> {

    public static final Creator<CameraAction> CREATOR = new Creator<CameraAction>() {
        @Override
        public CameraAction createFromParcel(Parcel in) {
            return new CameraAction(in);
        }

        @Override
        public CameraAction[] newArray(int size) {
            return new CameraAction[size];
        }
    };

    private CameraActionKind kind;
    private long captureId;
    private int captureQuality;
    private long scanId;
    private String scanPattern;
    private long index;
    private String description;
    private int order;
    private boolean multiPageDocument;

    private CameraAction(CameraActionKind kind, long captureId, int captureQuality, long scanId, String scanPattern,
                         long index, String description, int order, boolean multiPageDocument) {
        this.kind = kind;
        this.captureId = captureId;
        this.captureQuality = captureQuality;
        this.scanId = scanId;
        this.scanPattern = scanPattern;
        this.index = index;
        this.description = description;
        this.order = order;
        this.multiPageDocument = multiPageDocument;
    }

    private CameraAction(Parcel in) {
        kind = (CameraActionKind) in.readSerializable();
        captureId = in.readLong();
        captureQuality = in.readInt();
        scanId = in.readLong();
        scanPattern = in.readString();
        index = in.readLong();
        description = in.readString();
        order = in.readInt();
        multiPageDocument = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(kind);
        dest.writeLong(captureId);
        dest.writeInt(captureQuality);
        dest.writeLong(scanId);
        dest.writeString(scanPattern);
        dest.writeLong(index);
        dest.writeString(description);
        dest.writeInt(order);
        dest.writeByte((byte) (multiPageDocument ? 1 : 0));
    }

    public CameraActionKind getKind() {
        return kind;
    }

    public long getCaptureId() {
        return captureId;
    }

    public int getCaptureQuality() {
        return captureQuality;
    }

    public long getScanId() {
        return scanId;
    }

    public String getScanPattern() {
        return scanPattern;
    }

    public long getIndex() {
        return index;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public boolean isMultiPageDocument() {
        return multiPageDocument;
    }

    public String getHashKey() {
        return String.format(Locale.getDefault(), "%s:%d:%d:%d", kind.getCode(), captureId, scanId, index);
    }

    @NonNull
    @Override
    public String toString() {
        return description;
    }

    @Override
    public int compareTo(CameraAction anotherAction) {
        return Integer.compare(order, anotherAction.getOrder());
    }

    public static class Builder {

        private CameraActionKind kind;
        private long captureId = 0;
        private int captureQuality = 80;
        private long scanId = 0;
        private String scanPattern = null;
        private long index = 0;
        private String description = "Фото";
        private int order = 0;
        private boolean multiPageDocument = false;

        public Builder(CameraActionKind kind) {
            this.kind = kind;
        }

        public Builder setCaptureId(long captureId) {
            this.captureId = captureId;
            return this;
        }

        public Builder setCaptureQuality(int captureQuality) {
            // Validation guard
            if (captureQuality >= 0 && captureQuality <= 100) {
                this.captureQuality = captureQuality;
            }
            return this;
        }

        public Builder setScanId(long scanId) {
            this.scanId = scanId;
            return this;
        }

        public Builder setScanPattern(String scanPattern) {
            this.scanPattern = scanPattern;
            return this;
        }

        public Builder setIndex(long index) {
            this.index = index;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setOrder(int order) {
            this.order = order;
            return this;
        }

        public Builder setMultiPageDocument(boolean multiPageDocument) {
            this.multiPageDocument = multiPageDocument;
            return this;
        }

        public CameraAction build() {
            return new CameraAction(kind,
                                    captureId,
                                    captureQuality,
                                    scanId,
                                    scanPattern,
                                    index,
                                    description,
                                    order,
                                    multiPageDocument);
        }
    }
}
