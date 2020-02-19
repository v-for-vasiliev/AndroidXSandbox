package ru.vasiliev.sandbox.camera2.device.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

import ru.vasiliev.sandbox.camera2.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFacing;
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

    public static final int FACING_BACK = 0;
    public static final int FACING_FRONT = 1;

    public static final int FLASH_OFF = 0;
    public static final int FLASH_ON = 1;
    public static final int FLASH_TORCH = 2;
    public static final int FLASH_AUTO = 3;
    public static final int FLASH_RED_EYE = 4;

    public static final int LANDSCAPE_90 = 90;
    public static final int LANDSCAPE_270 = 270;

    public static AspectRatio ASPECT_RATIO_4_3 = AspectRatio.of(4, 3);
    public static AspectRatio ASPECT_RATIO_16_9 = AspectRatio.of(4, 3);

    private static final SparseIntArray CAMERA_INTERNAL_FACINGS = new SparseIntArray();

    static {
        CAMERA_INTERNAL_FACINGS.put(CameraFacing.BACK.getId(), CameraCharacteristics.LENS_FACING_BACK);
        CAMERA_INTERNAL_FACINGS.put(CameraFacing.FRONT.getId(), CameraCharacteristics.LENS_FACING_FRONT);
    }

    private CameraCharacteristics cameraCharacteristics;

    private CameraView cameraView;

    private final SizeMap previewSizes = new SizeMap();

    private final SizeMap pictureSizes = new SizeMap();

    private AspectRatio aspectRatio = ASPECT_RATIO_4_3;

    public CameraConfig(@NonNull CameraCharacteristics cameraCharacteristics, @NonNull CameraView cameraView) {
        this.cameraCharacteristics = cameraCharacteristics;
        this.cameraView = cameraView;
    }

    public static int getCameraInternalFacing(CameraFacing cameraFacing) {
        return CAMERA_INTERNAL_FACINGS.get(cameraFacing.getId(), CameraCharacteristics.LENS_FACING_BACK);
    }

    static CameraFacing getCameraFacing(int cameraInternalFacing) {
        for (int i = 0, count = CAMERA_INTERNAL_FACINGS.size(); i < count; i++) {
            if (CAMERA_INTERNAL_FACINGS.valueAt(i) == cameraInternalFacing) {
                return CameraFacing.byId(CAMERA_INTERNAL_FACINGS.keyAt(i));
            }
        }
        return CameraFacing.UNKNOWN;
    }

    /**
     * <p>Collects some information from {@link #cameraCharacteristics}.</p>
     * <p>This rewrites {@link #previewSizes}, {@link #pictureSizes}, and optionally,
     * {@link #aspectRatio}.</p>
     */
    void collectCameraInfo() {
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
    int getAfMode() {
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
            throw new IllegalStateException("Auto-focus not supported");
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
