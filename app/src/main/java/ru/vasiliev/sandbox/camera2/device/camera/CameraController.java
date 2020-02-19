package ru.vasiliev.sandbox.camera2.device.camera;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;

import androidx.annotation.NonNull;

import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFacing;
import timber.log.Timber;

public class CameraController extends CameraStateMachine {

    private final CameraManager mCameraManager;

    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    private CameraDevice mCamera;

    private CameraCaptureSession mCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;

    private CameraFacing mFacing = CameraFacing.BACK;

    private boolean mAutoFocus;

    private int mFlash;

    private int mDisplayOrientation;


    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            // mCallback.onCameraOpened();
            // startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            // mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Timber.e("onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    public CameraController(@NonNull CameraManager cameraManager) {
        mCameraManager = cameraManager;
        mFacing = CameraFacing.ANY;
    }

    public CameraController(@NonNull CameraManager cameraManager, CameraFacing facing) {
        mCameraManager = cameraManager;
        mFacing = facing;
    }

    @Override
    public void onReadyForStillPicture() {

    }

    @Override
    public void onPrecaptureRequired() {

    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() {
        try {
            final String[] ids = mCameraManager.getCameraIdList();

            if (mFacing == CameraFacing.BACK || mFacing == CameraFacing.FRONT) {
                int requestedInternalFacing = CameraConfig.getCameraInternalFacing(mFacing);
                if (ids.length == 0) { // No camera
                    throw new RuntimeException("No camera available.");
                }
                for (String id : ids) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                    Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        continue;
                    }
                    Integer cameraInternalFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (cameraInternalFacing == null) {
                        throw new NullPointerException("Unexpected state: LENS_FACING null");
                    }
                    if (cameraInternalFacing == requestedInternalFacing) {
                        mCameraId = id;
                        mCameraCharacteristics = characteristics;
                        return true;
                    }
                }
            } else {
                mCameraId = ids[0];
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
                Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    return false;
                }
                Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                mFacing = CameraConfig.getCameraFacing(internal);
                if (mFacing == CameraFacing.UNKNOWN) {
                    // The operation can reach here when the only camera device is an external one.
                    // We treat it as facing back.
                    mFacing = CameraFacing.BACK;
                }
                return true;
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
        return false;
    }
}
