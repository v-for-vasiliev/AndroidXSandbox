package ru.vasiliev.sandbox.camera2.framework.camera2;

import androidx.annotation.Nullable;

import java.util.Locale;

import timber.log.Timber;

public class Camera2Debug {

    private static boolean DBG = true;

    static void dbg(String fmt,
                    Object... args) {
        dbg(fmt, null, args);
    }

    static void dbg(String fmt,
                    @Nullable Throwable t,
                    Object... args) {
        if (DBG) {
            if (t == null) {
                Timber.d(t, fmt, args);
            } else {
                Timber.d(fmt, args);
            }
        }
    }

    private static String fmt(String template,
                              Object... args) {
        return String.format(Locale.getDefault(), template, args);
    }
}
