package ru.vasiliev.sandbox.camera2.framework.camera2;

import java.io.Serializable;

public class Camera2Metadata implements Serializable {

    private int quality;
    private long timestamp;
    private Camera2FocusMode focusMode;

    private Camera2Metadata(int quality, long timestamp, Camera2FocusMode focusMode) {
        this.quality = quality;
        this.timestamp = timestamp;
        this.focusMode = focusMode;
    }

    public int getQuality() {
        return quality;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Camera2FocusMode getFocusMode() {
        return focusMode;
    }

    public static class Builder {

        private int quality;
        private long timestamp;
        private Camera2FocusMode focusMode;

        public Builder() {
            quality = -1;
            timestamp = -1;
            focusMode = Camera2FocusMode.FOCUS_MODE_OFF;
        }

        public Builder setQuality(int quality) {
            this.quality = quality;
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setFocusMode(Camera2FocusMode focusMode) {
            this.focusMode = focusMode;
            return this;
        }

        public Camera2Metadata build() {
            return new Camera2Metadata(quality, timestamp, focusMode);
        }
    }
}
