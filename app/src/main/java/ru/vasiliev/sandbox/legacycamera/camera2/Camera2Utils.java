package ru.vasiliev.sandbox.legacycamera.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_CONVERGED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_INACTIVE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_LOCKED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_PRECAPTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_SEARCHING;

public class Camera2Utils {

    static String getAfModeString(Integer afMode) {
        if (afMode == null) {
            return "null";
        }
        switch (afMode) {
            case CameraMetadata.CONTROL_AF_MODE_OFF:
                return "AF_MODE_OFF";
            case CameraMetadata.CONTROL_AF_MODE_AUTO:
                return "AF_MODE_AUTO";
            case CameraMetadata.CONTROL_AF_MODE_MACRO:
                return "AF_MODE_MACRO";
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                return "AF_MODE_CONTINUOUS_VIDEO";
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
                return "AF_MODE_CONTINUOUS_PICTURE";
            case CameraMetadata.CONTROL_AF_MODE_EDOF:
                return "AF_MODE_EDOF";
            default:
                return "UNKNOWN {mode=" + afMode + "}";
        }
    }

    static String getAfStateString(Integer afState) {
        if (afState == null) {
            return "null";
        } else {
            switch (afState) {
                case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                    return "AF_STATE_FOCUSED_LOCKED";
                case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                    return "AF_STATE_NOT_FOCUSED_LOCKED";
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                    return "AF_STATE_PASSIVE_FOCUSED";
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                    return "AF_STATE_PASSIVE_UNFOCUSED";
                case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                    return "AF_STATE_INACTIVE";
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                    return "AF_STATE_PASSIVE_SCAN";
                case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                    return "AF_STATE_ACTIVE_SCAN";
                default:
                    return "UNKNOWN {state=" + afState + "}";
            }
        }
    }

    static String getAeModeString(Integer aeMode) {
        if (aeMode == null) {
            return "null";
        }
        switch (aeMode) {
            case CameraMetadata.CONTROL_AE_MODE_OFF:
                return "AE_MODE_OFF";
            case CameraMetadata.CONTROL_AE_MODE_ON:
                return "AE_MODE_ON";
            case CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH:
                return "AE_MODE_ON_AUTO_FLASH";
            case CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
                return "AE_MODE_ON_ALWAYS_FLASH";
            case CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE:
                return "AE_MODE_ON_AUTO_FLASH_REDEYE";
            case CameraMetadata.CONTROL_AE_MODE_ON_EXTERNAL_FLASH:
                return "AE_MODE_ON_EXTERNAL_FLASH";
            default:
                return "UNKNOWN {mode=" + aeMode + "}";
        }
    }

    static String getAeStateString(Integer aeState) {
        if (aeState == null) {
            return "null";
        } else {
            switch (aeState) {
                case CONTROL_AE_STATE_INACTIVE:
                    return "AE_STATE_INACTIVE";
                case CONTROL_AE_STATE_SEARCHING:
                    return "AE_STATE_SEARCHING";
                case CONTROL_AE_STATE_CONVERGED:
                    return "AE_STATE_CONVERGED";
                case CONTROL_AE_STATE_LOCKED:
                    return "AE_STATE_LOCKED";
                case CONTROL_AE_STATE_FLASH_REQUIRED:
                    return "AE_STATE_FLASH_REQUIRED";
                case CONTROL_AE_STATE_PRECAPTURE:
                    return "AE_STATE_PRECAPTURE";
                default:
                    return "UNKNOWN {state=" + aeState + "}";
            }
        }
    }

    static String getHardwareLevelString(Integer hardwareLevel) {
        if (hardwareLevel == null) {
            return "null";
        }
        switch (hardwareLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                return "INFO_SUPPORTED_HARDWARE_LEVEL_3";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                return "INFO_SUPPORTED_HARDWARE_LEVEL_FULL";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                return "INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                return "INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED";
            default:
                return "UNKNOWN {level=" + hardwareLevel + "}";
        }
    }
}
