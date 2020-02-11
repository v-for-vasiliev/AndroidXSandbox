package ru.vasiliev.sandbox.camera2.device.camera.util;

import androidx.annotation.Nullable;

import java.util.Locale;

import timber.log.Timber;

import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_CAPTURING;
import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_LOCKED;
import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_LOCKING;
import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_PRECAPTURE;
import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_PREVIEW;
import static ru.vasiliev.sandbox.camera2.device.camera.CameraStateMachine.STATE_WAITING;

public class CameraDbg {

    private static boolean DBG = true;

    public static void dbgCameraState(int state) {
        switch (state) {

            case STATE_PREVIEW:
                dbg("Camera state: STATE_PREVIEW");
                break;
            case STATE_LOCKING:
                dbg("Camera state: STATE_LOCKING");
                break;
            case STATE_LOCKED:
                dbg("Camera state: STATE_LOCKED");
                break;
            case STATE_PRECAPTURE:
                dbg("Camera state: STATE_PRECAPTURE");
                break;
            case STATE_WAITING:
                dbg("Camera state: STATE_WAITING");
                break;
            case STATE_CAPTURING:
                dbg("Camera state: STATE_CAPTURING");
                break;
            default:
                dbg("Camera state: %d", state);
                break;
        }
    }

    public static void dbg(String fmt, Object... args) {
        dbg(fmt, null, args);
    }

    public static void dbg(String fmt, @Nullable Throwable t, Object... args) {
        if (DBG) {
            if (t == null) {
                Timber.d(t, fmt, args);
            } else {
                Timber.d(fmt, args);
            }
        }
    }

    private static String fmt(String template, Object... args) {
        return String.format(Locale.getDefault(), template, args);
    }
}
