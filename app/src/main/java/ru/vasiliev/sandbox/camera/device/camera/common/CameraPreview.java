package ru.vasiliev.sandbox.camera.device.camera.common;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class CameraPreview extends FrameLayout {

    private int previewWidth = 0;
    private int previewHeight = 0;
    private PreviewSurfaceChangedListener previewSurfaceChangedListener;

    public CameraPreview(@NonNull Context context) {
        super(context, null);
    }

    public CameraPreview(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public CameraPreview(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface PreviewSurfaceChangedListener {

        void onPreviewSurfaceChanged();
    }

    public void setPreviewSurfaceChangedListener(PreviewSurfaceChangedListener previewSurfaceChangedListener) {
        this.previewSurfaceChangedListener = previewSurfaceChangedListener;
    }

    protected void dispatchSurfaceChanged() {
        if (previewSurfaceChangedListener != null) {
            previewSurfaceChangedListener.onPreviewSurfaceChanged();
        }
    }

    public abstract Class getPreviewOutputClass();

    public abstract boolean isReady();

    public abstract Surface getSurface();

    public abstract SurfaceTexture getSurfaceTexture();

    public abstract int getDisplayOrientation();

    public abstract void setDisplayOrientation(int displayOrientation);

    public void setPreviewBufferSize(int width, int height) {
    }

    protected void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }
}
