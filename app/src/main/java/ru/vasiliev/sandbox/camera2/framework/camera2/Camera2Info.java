package ru.vasiliev.sandbox.camera2.framework.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;

import static java.util.Objects.requireNonNull;
import static ru.vasiliev.sandbox.camera2.framework.camera2.Camera2Debug.dbg;

class Camera2Info {

    private CameraCharacteristics characteristics;

    Camera2Info(@NonNull CameraCharacteristics characteristics) {
        this.characteristics = characteristics;
    }

    boolean isLegacyHardware() {
        try {
            return requireNonNull(characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
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
            dbg("isDeviceCameraSupported() error:",
                t);
        }
        return hardwareLevelSupported;
    }

    boolean isLimitedDeviceCompatible() {
        if (characteristics == null) {
            return false;
        }
        int[] cameraCapabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (cameraCapabilities != null) {
            return contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                            cameraCapabilities);
        }
        return false;
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
        boolean afModeSupported = contains(CameraMetadata.CONTROL_AF_MODE_AUTO,
                                           supportedAfModes) || contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                                                                         supportedAfModes);
        return afModeSupported && !isLensFixedFocus();
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
            dbg("getMinimumFocusDistance() error:",
                t);
        }
        return (lensMinimumFocusDistance != null) ? lensMinimumFocusDistance : 0.0f;
    }

    boolean isAutoExposureSupported() {
        if (characteristics == null) {
            return false;
        }
        int[] aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AE_MODE_ON,
                        aeModes);
    }

    boolean isAWBSupported() {
        if (characteristics == null) {
            return false;
        }
        int[] awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        return contains(CaptureRequest.CONTROL_AWB_MODE_AUTO,
                        awbModes);
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

        dbg("Minimum focus distance: %.2f",
            getMinimumFocusDistance());
    }

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
