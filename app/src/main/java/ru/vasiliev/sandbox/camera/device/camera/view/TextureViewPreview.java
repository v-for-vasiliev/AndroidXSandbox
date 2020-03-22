/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vasiliev.sandbox.camera.device.camera.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera.device.camera.common.CameraPreview;
import ru.vasiliev.sandbox.camera.device.camera.util.DisplayOrientationDetector;

@TargetApi(14)
class TextureViewPreview extends CameraPreview {

    private int previewWidth = 0;
    private int previewHeight = 0;
    private final TextureView textureView;
    private final DisplayOrientationDetector displayOrientationDetector;
    private int displayOrientation;

    public TextureViewPreview(@NonNull Context context) {
        this(context, null);
    }

    public TextureViewPreview(@NonNull Context context,
                              @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextureViewPreview(@NonNull Context context,
                              @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final View view = View.inflate(context, R.layout.camera_texture_preview, this);
        textureView = view.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setPreviewSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                setPreviewSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setPreviewSize(0, 0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        displayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                TextureViewPreview.this.displayOrientation = displayOrientation;
                configureTransform();
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            displayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            displayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public Class getPreviewOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    public boolean isReady() {
        return textureView.getSurfaceTexture() != null;
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    @Override
    public void setPreviewBufferSize(int width, int height) {
        textureView.getSurfaceTexture().setDefaultBufferSize(width, height);
    }

    @Override
    public Surface getSurface() {
        return new Surface(textureView.getSurfaceTexture());
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return textureView.getSurfaceTexture();
    }

    @Override
    public int getDisplayOrientation() {
        return displayOrientation;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        this.displayOrientation = displayOrientation;
        configureTransform();
    }

    /**
     * Configures the transform matrix for TextureView based on {@link #displayOrientation} and
     * the surface size.
     */
    private void configureTransform() {
        Matrix matrix = new Matrix();
        if (displayOrientation % 180 == 90) {
            final int width = getPreviewWidth();
            final int height = getPreviewHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    displayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f, // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (displayOrientation == 180) {
            matrix.postRotate(180, getPreviewWidth() / 2.0f, getPreviewHeight() / 2.0f);
        }
        textureView.setTransform(matrix);
    }
}
