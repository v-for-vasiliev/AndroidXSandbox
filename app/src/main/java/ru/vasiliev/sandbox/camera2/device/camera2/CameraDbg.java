package ru.vasiliev.sandbox.camera2.device.camera2;

import androidx.annotation.Nullable;

import java.util.Locale;

import timber.log.Timber;

import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_CAPTURING;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_LOCKED;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_LOCKING;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_PRECAPTURE;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_PREVIEW;
import static ru.vasiliev.sandbox.camera2.device.camera2.CameraStateMachine.STATE_WAITING;

class CameraDbg {

    private static boolean DBG = true;

    static void dbgCameraState(int state) {
        switch (state) {

            case STATE_PREVIEW:
                dbg("State changed: STATE_PREVIEW");
                break;
            case STATE_LOCKING:
                dbg("State changed: STATE_LOCKING");
                break;
            case STATE_LOCKED:
                dbg("State changed: STATE_LOCKED");
                break;
            case STATE_PRECAPTURE:
                dbg("State changed: STATE_PRECAPTURE");
                break;
            case STATE_WAITING:
                dbg("State changed: STATE_WAITING");
                break;
            case STATE_CAPTURING:
                dbg("State changed: STATE_CAPTURING");
                break;
            default:
                dbg("State changed: %d", state);
                break;
        }
    }

    static void dbg(String fmt, Object... args) {
        dbg(fmt, null, args);
    }

    static void dbg(String fmt, @Nullable Throwable t, Object... args) {
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
