package ru.vasiliev.sandbox.camera2.device.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;

import ru.vasiliev.sandbox.camera2.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera2.device.camera2.CameraInfo;
import ru.vasiliev.sandbox.camera2.device.camera2.SizeMap;

public class CameraController extends CameraStateMachine {

    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    CameraDevice mCamera;

    CameraCaptureSession mCaptureSession;

    CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private int mFacing;

    private AspectRatio mAspectRatio = CameraInfo.DEFAULT_ASPECT_RATIO;

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
