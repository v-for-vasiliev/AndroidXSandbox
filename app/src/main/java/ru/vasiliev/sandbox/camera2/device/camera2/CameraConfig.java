package ru.vasiliev.sandbox.camera2.device.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;

import static java.util.Objects.requireNonNull;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraDbg.dbg;

class CameraConfig {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    static AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3);

    int FACING_BACK = 0;
    int FACING_FRONT = 1;

    int FLASH_OFF = 0;
    int FLASH_ON = 1;
    int FLASH_TORCH = 2;
    int FLASH_AUTO = 3;
    int FLASH_RED_EYE = 4;

    int LANDSCAPE_90 = 90;
    int LANDSCAPE_270 = 270;

    private CameraCharacteristics characteristics;

    CameraConfig(@NonNull CameraCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    boolean isLegacyHardware() {
        try {
            return requireNonNull(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) ==
                   CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        } catch (final Throwable ignore) {
            return false;
        }

    }

    boolean isDeviceCameraSupported(int requiredHardwareLevel) {
        boolean hardwareLevelSupported = false;
        try {
            int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                hardwareLevelSupported = requiredHardwareLevel == deviceLevel;
            } else if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
                hardwareLevelSupported = isLimitedDeviceCompatible();
            } else {
                // deviceLevel is not LEGACY or LIMITED, can use numerical sort
                hardwareLevelSupported = requiredHardwareLevel <= deviceLevel;
            }
        } catch (final Throwable t) {
            dbg("isDeviceCameraSupported() error:", t);
        }
        return hardwareLevelSupported;
    }

    boolean isLimitedDeviceCompatible() {
        if (characteristics == null) {
            return false;
        }
        int[] cameraCapabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (cameraCapabilities != null) {
            return contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE, cameraCapabilities);
        }
        return false;
    }

    /**
     * Indicates that device has fixed focus and the camera lens can't move, so we can't set AF mode to streaming
     * preview requests, only for capture.
     *
     * @return true if device has fixed focus, false otherwise.
     */
    boolean isLensFixedFocus() {
        return getMinimumFocusDistance() == 0.0f;
    }

    /**
     * Shortest distance from frontmost surface of the lens that can be brought into sharp focus.
     * If the lens is fixed-focus (doesn't support manual focus), this will be 0.
     */
    private float getMinimumFocusDistance() {
        if (characteristics == null) {
            return 0.0f;
        }
        Float lensMinimumFocusDistance = null;
        try {
            lensMinimumFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (final Throwable t) {
            dbg("getMinimumFocusDistance() error:", t);
        }
        return (lensMinimumFocusDistance != null) ? lensMinimumFocusDistance : 0.0f;
    }

    /**
     * Indicates that device support auto-focus feature.
     *
     * @return true if support, false otherwise.
     */
    boolean isAutoFocusSupported() {
        if (characteristics == null) {
            return false;
        }
        int[] supportedAfModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes) ||
               contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes);
    }

    int getAutoFocusMode() {
        int[] supportedAfModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, supportedAfModes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (contains(CameraMetadata.CONTROL_AF_MODE_AUTO, supportedAfModes)) {
            return CameraMetadata.CONTROL_AF_MODE_AUTO;
        } else {
            throw new IllegalStateException("AUTO_FOCUS_NOT_SUPPORTED");
        }
    }

    boolean isAutoExposureSupported() {
        if (characteristics == null) {
            return false;
        }
        int[] aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AE_MODE_ON, aeModes);
    }

    int getAutoExposureMode(boolean flashRequired) {
        // If flash required and there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (flashRequired && contains(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                                      characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))) {
            return CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
        } else {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }
    }

    boolean isAWBSupported() {
        if (characteristics == null) {
            return false;
        }
        int[] awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AWB_MODE_AUTO, awbModes);
    }

    boolean isAfMeteringAreaSupported() {
        if (characteristics == null) {
            return false;
        }
        Integer maxRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return maxRegions != null && maxRegions >= 1;
    }

    boolean isAeMeteringAreaSupported() {
        if (characteristics == null) {
            return false;
        }
        Integer maxRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return maxRegions != null && maxRegions > 0;
    }

    void printCameraCharacteristics() {
        if (characteristics == null) {
            return;
        }

        dbg("=== CAMERA CHARACTERISTICS ===");
        dbg("Hardware level: %s",
            Camera2Utils.getHardwareLevelString(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)));


        dbg("Supported camera AF modes:");
        int[] supportedAFModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (supportedAFModes != null) {
            for (int mode : supportedAFModes) {
                switch (mode) {
                    case CameraMetadata.CONTROL_AF_MODE_OFF:
                        dbg("- CONTROL_AF_MODE_OFF");
                        break;
                    case CameraMetadata.CONTROL_AF_MODE_AUTO:
                        dbg("- CONTROL_AF_MODE_AUTO");
                        break;
                    case CameraMetadata.CONTROL_AF_MODE_MACRO:
                        dbg("- CONTROL_AF_MODE_MACRO");
                        break;
                    case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                        dbg("- CONTROL_AF_MODE_CONTINUOUS_VIDEO");
                        break;
                    case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
                        dbg("- CONTROL_AF_MODE_CONTINUOUS_PICTURE");
                        break;
                    case CameraMetadata.CONTROL_AF_MODE_EDOF:
                        dbg("- CONTROL_AF_MODE_EDOF");
                        break;
                    default:
                        dbg("- UNKNOWN {mode=" + mode + "}");
                }
            }
        }

        dbg("Minimum focus distance: %.2f", getMinimumFocusDistance());
    }

    private boolean contains(int value, int[] array) {
        if (array == null) {
            return false;
        }
        for (int v : array) {
            if (v == value) {
                return true;
            }
            //java.
        }
        return false;
    }

    /*

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes}, and optionally,
     * {@link #mAspectRatio}.</p>
     */
    /*
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                mPreviewSizes.add(new Size(width, height));
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            if (!mPictureSizes.ratios()
                    .contains(ratio)) {
                mPreviewSizes.remove(ratio);
            }
        }

        if (!mPreviewSizes.ratios()
                .contains(mAspectRatio)) {
            mAspectRatio = mPreviewSizes.ratios()
                    .iterator()
                    .next();
        }
    }

    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)) {
            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }
    */
}
