package ru.vasiliev.sandbox.camera2.framework.camera2.exception;

public class CameraNotReadyToCaptureException extends Exception {

    public CameraNotReadyToCaptureException() {
        super("CAMERA_NOT_READY_TO_CAPTURE");
    }
}
