package ru.vasiliev.sandbox.camera2.device.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;

public abstract class CameraStateMachine extends CameraCaptureSession.CaptureCallback {

    static final int STATE_PREVIEW = 0;
    static final int STATE_LOCKING = 1;
    static final int STATE_LOCKED = 2;
    static final int STATE_PRECAPTURE = 3;
    static final int STATE_WAITING = 4;
    static final int STATE_CAPTURING = 5;

    private int state;

    CameraStateMachine() {
    }

    void setState(int state) {
        this.state = state;
        CameraDbg.dbgCameraState(state);
    }

    private void process(@NonNull CaptureResult result) {
        switch (state) {
            case STATE_LOCKING: {
                Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                if (af == null) {
                    break;
                }
                if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                    af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_CAPTURING);
                        onReadyForStillPicture();
                    } else {
                        setState(STATE_LOCKED);
                        onPrecaptureRequired();
                    }
                }
                break;
            }
            case STATE_PRECAPTURE: {
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                    ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                    ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    setState(STATE_WAITING);
                }
                break;
            }
            case STATE_WAITING: {
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    setState(STATE_CAPTURING);
                    onReadyForStillPicture();
                }
                break;
            }
        }
    }

    /**
     * Called when it is ready to take a still picture.
     */
    public abstract void onReadyForStillPicture();

    /**
     * Called when it is necessary to run the precapture sequence.
     */
    public abstract void onPrecaptureRequired();

    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp,
                                 long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                    @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        process(result);
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
    }

    @Override
    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
    }

    @Override
    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        super.onCaptureSequenceAborted(session, sequenceId);
    }

    @Override
    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                    @NonNull Surface target, long frameNumber) {
        super.onCaptureBufferLost(session, request, target, frameNumber);
    }
}
