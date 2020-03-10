package ru.vasiliev.sandbox.camera2.device.camera.common;

import android.view.Surface;

public abstract class CameraView {

    private int mWidth = 0;
    private int mHeight = 0;

    public interface CameraViewChangedListener {

        void onCameraViewChanged();
    }

    private CameraViewChangedListener cameraViewChangedListener;

    public void setCameraViewChangedListener(CameraViewChangedListener cameraViewChangedListener) {
        this.cameraViewChangedListener = cameraViewChangedListener;
    }

    public abstract Class<Object> getPreviewOutputClass();

    protected void dispatchCameraViewChanged() {
        cameraViewChangedListener.onCameraViewChanged();
    }

    public abstract boolean isReady();

    public abstract Surface getSurface();

    public abstract int getDisplayOrientation();

    public void setBufferSize(int width, int height) {
    }

    void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
