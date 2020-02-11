package ru.vasiliev.sandbox.camera2.device.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;

public class CameraController extends CameraStateMachine {

    private String cameraId;

    private CameraCharacteristics cameraCharacteristics;

    CameraDevice cameraDevice;

    CameraCaptureSession mCaptureSession;

    CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private int mFacing;

    private boolean mAutoFocus;

    private int mFlash;

    private int mDisplayOrientation;

    @Override
    public void onReadyForStillPicture() {

    }

    @Override
    public void onPrecaptureRequired() {

    }
}
