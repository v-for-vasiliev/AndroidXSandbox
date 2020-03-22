package ru.vasiliev.sandbox.legacycamera.camera2.exception;

public class CameraNotReadyToCaptureException extends Exception {

    public CameraNotReadyToCaptureException() {
        super("CAMERA_NOT_READY_TO_CAPTURE");
    }
}
