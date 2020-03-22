package ru.vasiliev.sandbox.camera.device.camera.common;

import io.reactivex.Observable;
import ru.vasiliev.sandbox.camera.device.camera.data.CaptureMetadata;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash;

public interface CameraController {

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

    void setAutoWhiteBalance(boolean autoWhiteBalance);

    boolean getAutoWhiteBalance();

    void takePicture();

    Observable<byte[]> getImageCaptureStream();

    Observable<CaptureMetadata> getImageMetadataStream();

    Observable<byte[]> getImageProcessorStream();
}