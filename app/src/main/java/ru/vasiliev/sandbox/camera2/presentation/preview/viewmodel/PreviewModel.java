package ru.vasiliev.sandbox.camera2.presentation.preview.viewmodel;

import android.graphics.Bitmap;

import ru.vasiliev.sandbox.camera2.data.result.CameraResult;

public class PreviewModel {
    private CameraResult cameraResult;
    private Bitmap previewBitmap;
    private long previewBitmapCacheId;

    public PreviewModel(CameraResult cameraResult, Bitmap previewBitmap, long previewBitmapCacheId) {
        this.cameraResult = cameraResult;
        this.previewBitmap = previewBitmap;
        this.previewBitmapCacheId = previewBitmapCacheId;
    }

    public CameraResult getCameraResult() {
        return cameraResult;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public long getPreviewBitmapCacheId() {
        return previewBitmapCacheId;
    }
}
