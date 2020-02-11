package ru.vasiliev.sandbox.camera2.device.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;

import androidx.annotation.NonNull;

import ru.vasiliev.sandbox.camera2.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera2.device.camera.util.Size;
import ru.vasiliev.sandbox.camera2.device.camera.util.SizeMap;

public class CameraConfig {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    static AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3);

    private CameraCharacteristics cameraCharacteristics;

    private CameraView cameraView;

    private final SizeMap previewSizes = new SizeMap();

    private final SizeMap pictureSizes = new SizeMap();

    private AspectRatio aspectRatio = DEFAULT_ASPECT_RATIO;

    private int mFacing;

    private boolean mAutoFocus;

    private int mFlash;

    private int mDisplayOrientation;

    public CameraConfig(@NonNull CameraCharacteristics cameraCharacteristics, @NonNull CameraView cameraView) {
        this.cameraCharacteristics = cameraCharacteristics;
        this.cameraView = cameraView;
    }

    /**
     * <p>Collects some information from {@link #cameraCharacteristics}.</p>
     * <p>This rewrites {@link #previewSizes}, {@link #pictureSizes}, and optionally,
     * {@link #aspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map");
        }
        previewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(cameraView.getPreviewOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                previewSizes.add(new Size(width, height));
            }
        }
        pictureSizes.clear();
        collectPictureSizes(map);
        for (AspectRatio ratio : previewSizes.ratios()) {
            if (!pictureSizes.ratios()
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
            pictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    /**
     * Indicates whether the camera device lens has fixed focus. When camera device has fixed focus lens can't
     * move, so we can't set AF mode to streaming preview requests, only for capture.
     *
     * @return true if camera device has fixed focus, false otherwise.
     */
    boolean isLensFixedFocus() {
        return getMinimumFocusDistance() == 0.0f;
    }

    /**
     * Shortest distance from frontmost surface of the lens that can be brought into sharp focus.
     * If the lens is fixed-focus (doesn't support manual focus), this will be 0.
     */
    private float getMinimumFocusDistance() {
        Float lensMinimumFocusDistance = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        return (lensMinimumFocusDistance != null) ? lensMinimumFocusDistance : 0.0f;
    }

    /**
     * @return true if device support auto-focus feature, false otherwise.
     */
    boolean isAutoFocusSupported() {
        int[] supportedAfModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes) ||
               contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes);
    }

    int getAutoFocusMode() {
        int[] supportedAfModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes)) {
            return CameraMetadata.CONTROL_AF_MODE_AUTO;
        } else {
            throw new IllegalStateException("Auto focus not supported");
        }
    }

    boolean isAutoExposureSupported() {
        int[] aeModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AE_MODE_ON, aeModes);
    }

    int getAutoExposureMode(boolean flashRequired) {
        // If flash required and there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (flashRequired && contains(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                                      cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))) {
            return CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
        } else {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }
    }

    boolean isAWBSupported() {
        int[] awbModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AWB_MODE_AUTO, awbModes);
    }

    boolean isAfMeteringAreaSupported() {
        Integer maxRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return maxRegions != null && maxRegions >= 1;
    }

    boolean isAeMeteringAreaSupported() {
        Integer maxRegions = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return maxRegions != null && maxRegions > 0;
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
