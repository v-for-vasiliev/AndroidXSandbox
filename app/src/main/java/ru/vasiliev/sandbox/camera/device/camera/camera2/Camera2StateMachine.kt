package ru.vasiliev.sandbox.camera.device.camera.camera2

import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraMetadata.*
import android.hardware.camera2.CaptureResult.CONTROL_AE_STATE
import android.hardware.camera2.CaptureResult.CONTROL_AF_STATE
import android.view.Surface
import ru.vasiliev.sandbox.camera.device.camera.util.CameraDebug

abstract class Camera2StateMachine internal constructor() : CaptureCallback() {

    private var captureState = STATE_PREVIEW

    fun setCaptureState(newCaptureState: Int) {
        captureState = newCaptureState
        CameraDebug.logCamera2State(newCaptureState)
    }

    private fun process(result: CaptureResult) = when (captureState) {
        STATE_LOCKING -> result.hasKeyValues(key = CONTROL_AF_STATE,
                                             values = listOf(
                                                 CONTROL_AF_STATE_FOCUSED_LOCKED,
                                                 CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                                             ),
                                             orNull = false,
                                             onValueFound = {
                                                 result.hasKeyValues(key = CONTROL_AE_STATE,
                                                                     values = listOf(CONTROL_AE_STATE_CONVERGED),
                                                                     orNull = true,
                                                                     onValueFound = {
                                                                         setCaptureState(STATE_CAPTURING)
                                                                         onReadyForStillPicture()
                                                                     },
                                                                     onValueNotFound = {
                                                                         setCaptureState(STATE_LOCKED)
                                                                         onPrecaptureRequired()
                                                                     })
                                             })
        STATE_PRECAPTURE -> result.hasKeyValues(key = CONTROL_AE_STATE,
                                                values = listOf(
                                                    CONTROL_AE_STATE_PRECAPTURE,
                                                    CONTROL_AE_STATE_FLASH_REQUIRED,
                                                    CONTROL_AE_STATE_CONVERGED
                                                ),
                                                orNull = true,
                                                onValueFound = { setCaptureState(STATE_WAITING) })
        STATE_WAITING -> result.hasKeyValues(key = CONTROL_AE_STATE,
                                             values = listOf(CONTROL_AE_STATE_PRECAPTURE),
                                             orNull = true,
                                             onValueNotFound = {
                                                 setCaptureState(STATE_CAPTURING)
                                                 onReadyForStillPicture()
                                             })
        else -> {
            CameraDebug.logCamera2State(captureState)
        }
    }

    /**
     * Called when it is ready to take a still picture.
     */
    abstract fun onReadyForStillPicture()

    /**
     * Called when it is necessary to run the precapture sequence.
     */
    abstract fun onPrecaptureRequired()

    override fun onCaptureStarted(
        session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long
    ) {
        super.onCaptureStarted(
            session,
            request,
            timestamp,
            frameNumber
        )
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult
    ) {
        super.onCaptureProgressed(
            session,
            request,
            partialResult
        )
        process(partialResult)
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(
            session,
            request,
            result
        )
        process(result)
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
    ) {
        super.onCaptureFailed(
            session,
            request,
            failure
        )
    }

    override fun onCaptureSequenceCompleted(
        session: CameraCaptureSession, sequenceId: Int, frameNumber: Long
    ) {
        super.onCaptureSequenceCompleted(
            session,
            sequenceId,
            frameNumber
        )
    }

    override fun onCaptureSequenceAborted(
        session: CameraCaptureSession, sequenceId: Int
    ) {
        super.onCaptureSequenceAborted(
            session,
            sequenceId
        )
    }

    override fun onCaptureBufferLost(
        session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long
    ) {
        super.onCaptureBufferLost(
            session,
            request,
            target,
            frameNumber
        )
    }

    private fun <T> CaptureResult.hasKeyValues(
        key: CaptureResult.Key<T>, values: List<T>, orNull: Boolean = false, onValueFound: () -> Unit = {},
        onValueNotFound: () -> Unit = {}
    ) {
        val value: T? = get(key)
        if (orNull && value == null) {
            onValueFound.invoke()
        } else {
            if (values.contains(value)) {
                onValueFound.invoke()
            } else {
                onValueNotFound.invoke()
            }
        }
    }

    companion object {
        const val STATE_PREVIEW = 0
        const val STATE_LOCKING = 1
        const val STATE_LOCKED = 2
        const val STATE_PRECAPTURE = 3
        const val STATE_WAITING = 4
        const val STATE_CAPTURING = 5
    }
}