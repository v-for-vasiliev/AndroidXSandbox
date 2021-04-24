package ru.vasiliev.sandbox.camera.device.camera.camera2;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;

import androidx.annotation.NonNull;

import java.util.SortedSet;

import ru.vasiliev.sandbox.camera.device.camera.common.CameraPreview;
import ru.vasiliev.sandbox.camera.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera.device.camera.util.Size;
import ru.vasiliev.sandbox.camera.device.camera.util.SizeMap;

public class Camera2Config {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;
    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    static final int CAPTURE_IMAGE_BUFFER_SIZE = 3;
    public static AspectRatio ASPECT_RATIO_4_3 = AspectRatio.of(4, 3);
    public static AspectRatio ASPECT_RATIO_16_9 = AspectRatio.of(16, 9);

    private CameraCharacteristics cameraCharacteristics;
    private CameraPreview cameraPreview;
    private final SizeMap previewSizes = new SizeMap();
    private final SizeMap captureSizes = new SizeMap();
    private AspectRatio aspectRatio;

    Camera2Config(@NonNull CameraCharacteristics cameraCharacteristics, @NonNull AspectRatio aspectRatio,
                  @NonNull CameraPreview cameraPreview) {
        this.cameraCharacteristics = cameraCharacteristics;
        this.cameraPreview = cameraPreview;
        this.aspectRatio = aspectRatio;
        collectCameraInfo();
    }

    /**
     * <p>Collects some information from {@link #cameraCharacteristics}.</p>
     * <p>This rewrites {@link #previewSizes}, {@link #captureSizes}, and optionally,
     * {@link #aspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map");
        }

        previewSizes.clear();
        //noinspection unchecked
        for (android.util.Size size : map.getOutputSizes(cameraPreview.getPreviewOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                previewSizes.add(new Size(width, height));
            }
        }

        captureSizes.clear();
        collectPictureSizes(map);
        // Filter out preview sizes which are not compatible with capture sizes
        for (AspectRatio ratio : previewSizes.ratios()) {
            if (!captureSizes.ratios()
                    .contains(ratio)) {
                previewSizes.remove(ratio);
            }
        }

        if (!previewSizes.ratios()
                .contains(aspectRatio)) {
            aspectRatio = previewSizes.ratios()
                    .iterator()
                    .next();
        }
    }

    private void collectPictureSizes(StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)) {
            captureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    Size getLargestCaptureSize() {
        return captureSizes.sizes(aspectRatio)
                .last();
    }

    /**
     * Chooses the optimal preview size based on {@link #previewSizes} and the surface size.
     *
     * @return The picked size for camera preview.
     */
    Size getPreviewOptimalSize() {
        int previewLonger, previewShorter;
        final int surfaceWidth = cameraPreview.getPreviewWidth();
        final int surfaceHeight = cameraPreview.getPreviewHeight();
        if (surfaceWidth < surfaceHeight) {
            previewLonger = surfaceHeight;
            previewShorter = surfaceWidth;
        } else {
            previewLonger = surfaceWidth;
            previewShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = previewSizes.sizes(aspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= previewLonger && size.getHeight() >= previewShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    /**
     * Indicates whether the cameraDevice device lens has fixed focus. When cameraDevice device has fixed focus lens can't
     * move, so we can't set AF mode to streaming preview requests, only for capture.
     *
     * @return true if cameraDevice device has fixed focus, false otherwise.
     */
    boolean isLensFixedFocus() {
        return getMinimumFocusDistance() == 0.0f;
    }

    /**
     * Shortest distance from frontmost surface of the lens that can be brought into sharp focus.
     * If the lens is fixed focus (doesn't support manual focus), this will be 0.
     */
    private float getMinimumFocusDistance() {
        Float lensMinimumFocusDistance = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        return (lensMinimumFocusDistance != null) ? lensMinimumFocusDistance : 0.0f;
    }

    /**
     * @return true if device support auto focus feature, false otherwise.
     */
    boolean isAfSupported() {
        int[] supportedAfModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes) ||
               contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes);
    }

    /**
     * @return the most suitable auto focus mode, if auto focus feature supported, throw exception otherwise.
     */
    int getOptimalAfMode() {
        int[] supportedAfModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes)) {
            return CameraMetadata.CONTROL_AF_MODE_AUTO;
        } else {
            throw new IllegalStateException("Auto focus not supported");
        }
    }

    /**
     * @return true if flash unit exist for selected camera module (FRONT/BACK)
     */
    boolean isFlashSupported() {
        Boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return (flashAvailable == null) ? false : flashAvailable;
    }

    /**
     * @return true if auto exposure feature supported, false otherwise.
     */
    boolean isAeSupported() {
        int[] aeModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AE_MODE_ON, aeModes);
    }

    /**
     * @return the most suitable auto exposure mode, if auto exposure feature supported, throw exception otherwise.
     */
    int getAeMode(boolean flashRequired) {
        // If flash required and there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (flashRequired && contains(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                                      cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))) {
            return CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
        } else if (contains(CaptureRequest.CONTROL_AE_MODE_ON,
                            cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))) {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        } else {
            throw new IllegalStateException("Auto-exposure not supported");
        }
    }

    /**
     * @return true if auto white balance feature supported, false otherwise.
     */
    boolean isAwbSupported() {
        int[] awbModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AWB_MODE_AUTO, awbModes);
    }

    /**
     * @return true if auto focus regions are supported, false otherwise.
     */
    boolean isAfMeteringAreaSupported() {
        Integer maxRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return maxRegions != null && maxRegions >= 1;
    }

    /**
     * @return true if auto exposure regions are supported, false otherwise.
     */
    boolean isAeMeteringAreaSupported() {
        Integer maxRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return maxRegions != null && maxRegions > 0;
    }

    /**
     * @return camera sensor orientation
     */
    int getCameraSensorOrientation() {
        return cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    /**
     * Helper function to check whether the array contains the given value
     *
     * @param value value
     * @param array array
     * @return true if contains, false otherwise
     */
    private boolean contains(int value, int[] array) {
        if (array == null) {
            return false;
        }
        for (int v : array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }
}
