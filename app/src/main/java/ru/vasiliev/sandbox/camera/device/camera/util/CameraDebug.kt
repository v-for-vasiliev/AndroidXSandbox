package ru.vasiliev.sandbox.camera.device.camera.util

import ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2StateMachine
import timber.log.Timber
import java.util.*

object CameraDebug {
    private const val DBG = true
    fun logCamera2State(state: Int) {
        when (state) {
            Camera2StateMachine.STATE_PREVIEW -> log("Camera state: STATE_PREVIEW")
            Camera2StateMachine.STATE_LOCKING -> log("Camera state: STATE_LOCKING")
            Camera2StateMachine.STATE_LOCKED -> log("Camera state: STATE_LOCKED")
            Camera2StateMachine.STATE_PRECAPTURE -> log("Camera state: STATE_PRECAPTURE")
            Camera2StateMachine.STATE_WAITING -> log("Camera state: STATE_WAITING")
            Camera2StateMachine.STATE_CAPTURING -> log("Camera state: STATE_CAPTURING")
            else -> log(
                "Camera state: %d",
                state
            )
        }
    }

    fun log(
        fmt: String?, vararg args: Any?
    ) {
        log(
            fmt,
            null,
            *args
        )
    }

    fun log(
        fmt: String?, t: Throwable?, vararg args: Any?
    ) {
        if (DBG) {
            if (t != null) {
                Timber.d(
                    t,
                    fmt,
                    *args
                )
            } else {
                Timber.d(
                    fmt,
                    *args
                )
            }
        }
    }

    private fun fmt(
        template: String, vararg args: Any
    ): String {
        return String.format(
            Locale.getDefault(),
            template,
            *args
        )
    }
}