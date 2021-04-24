package ru.vasiliev.sandbox.camera.device.camera.camera2

import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.view.Surface
import ru.vasiliev.sandbox.camera.device.camera.util.Debug

abstract class Camera2StateMachine internal constructor() : CaptureCallback() {
    private var state = 0
    fun setCaptureState(state: Int) {
        this.state = state
        Debug.logCamera2State(state)
    }

    private fun process(result: CaptureResult) {
        when (state) {
            STATE_LOCKING -> {
                val af = result.get(CaptureResult.CONTROL_AF_STATE) ?: break
                if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setCaptureState(STATE_CAPTURING)
                        onReadyForStillPicture()
                    } else {
                        setCaptureState(STATE_LOCKED)
                        onPrecaptureRequired()
                    }
                }
            }
            STATE_PRECAPTURE -> {
                val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    setCaptureState(STATE_WAITING)
                }
            }
            STATE_WAITING -> {
                val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    setCaptureState(STATE_CAPTURING)
                    onReadyForStillPicture()
                }
            }
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
    override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long,
                                  frameNumber: Long) {
        super.onCaptureStarted(session, request, timestamp, frameNumber)
    }

    override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest,
                                     partialResult: CaptureResult) {
        super.onCaptureProgressed(session, request, partialResult)
        process(partialResult)
    }

    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
        super.onCaptureCompleted(session, request, result)
        process(result)
    }

    override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
        super.onCaptureFailed(session, request, failure)
    }

    override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        super.onCaptureSequenceAborted(session, sequenceId)
    }

    override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface,
                                     frameNumber: Long) {
        super.onCaptureBufferLost(session, request, target, frameNumber)
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