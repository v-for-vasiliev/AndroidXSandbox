package ru.vasiliev.sandbox.camera2.device.camera.common;

import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFlash;

public interface CameraController {

    /**
     * @return {@code true} if the implementation was able to start the camera session.
     */
    boolean start();

    void stop();

    boolean isCameraOpened();

    void setFacing(CameraFacing facing);

    CameraFacing getFacing();

    /*
    Set<AspectRatio> getSupportedAspectRatios();

    boolean setAspectRatio(AspectRatio ratio);

    AspectRatio getAspectRatio();
    */

    void setAutoFocus(boolean autoFocus);

    boolean getAutoFocus();

    void setFlash(CameraFlash cameraFlash);

    CameraFlash getFlash();

    void takePicture();

    // abstract void setDisplayOrientation(int displayOrientation);

    interface CameraControllerCallback {

        void onCameraOpened();

        void onCameraClosed();
    }
}