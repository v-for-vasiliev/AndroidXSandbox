package ru.vasiliev.sandbox.camera.device.camera.data;

import java.io.Serializable;

public class CaptureMetadata implements Serializable {

    private int quality;
    private long timestamp;

    private CaptureMetadata(int quality, long timestamp) {
        this.quality = quality;
        this.timestamp = timestamp;
    }

    public int getQuality() {
        return quality;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static class Builder {

        private int quality;
        private long timestamp;

        public Builder() {
            quality = -1;
            timestamp = -1;
        }

        public Builder setQuality(int quality) {
            this.quality = quality;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public CaptureMetadata build() {
            return new CaptureMetadata(quality, timestamp);
        }
    }
}
