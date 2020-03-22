package ru.vasiliev.sandbox.camera.device.camera.util;

import androidx.annotation.Nullable;

import java.util.Locale;

import timber.log.Timber;

import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_CAPTURING;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_LOCKED;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_LOCKING;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_PRECAPTURE;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_PREVIEW;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine.STATE_WAITING;

public class Debug {

    private static boolean DBG = true;

    public static void logCamera2State(int state) {
        switch (state) {

            case STATE_PREVIEW:
                log("Camera state: STATE_PREVIEW");
                break;
            case STATE_LOCKING:
                log("Camera state: STATE_LOCKING");
                break;
            case STATE_LOCKED:
                log("Camera state: STATE_LOCKED");
                break;
            case STATE_PRECAPTURE:
                log("Camera state: STATE_PRECAPTURE");
                break;
            case STATE_WAITING:
                log("Camera state: STATE_WAITING");
                break;
            case STATE_CAPTURING:
                log("Camera state: STATE_CAPTURING");
                break;
            default:
                log("Camera state: %d", state);
                break;
        }
    }

    public static void log(String fmt, Object... args) {
        log(fmt, null, args);
    }

    public static void log(String fmt, @Nullable Throwable t, Object... args) {
        if (DBG) {
            if (t != null) {
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
