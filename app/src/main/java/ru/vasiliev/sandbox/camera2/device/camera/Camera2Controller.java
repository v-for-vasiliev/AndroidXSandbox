package ru.vasiliev.sandbox.camera2.device.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ru.vasiliev.sandbox.camera2.device.camera.common.CameraController;
import ru.vasiliev.sandbox.camera2.device.camera.common.CameraView;
import ru.vasiliev.sandbox.camera2.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera2.device.camera.util.CameraFlash;
import ru.vasiliev.sandbox.camera2.device.camera.util.Size;
import timber.log.Timber;

import static ru.vasiliev.sandbox.camera2.device.camera.Camera2Config.ASPECT_RATIO_4_3;
import static ru.vasiliev.sandbox.camera2.device.camera.Camera2Config.CAPTURE_IMAGE_BUFFER_SIZE;

public class Camera2Controller extends Camera2StateMachine implements CameraController {

    private CameraControllerCallback controllerCallback;

    private CameraManager cameraManager;

    private String cameraId;

    private CameraDevice cameraDevice;

    private CameraCharacteristics cameraCharacteristics;

    private Camera2RequestManager camera2RequestManager;

    private CameraCaptureSession cameraCaptureSession;

    private ImageReader captureImageReader;

    private CameraView cameraView;

    private Camera2Config camera2Config;

    private CameraFacing cameraFacing;

    private AspectRatio aspectRatio = ASPECT_RATIO_4_3;

    private boolean autoFocus;

    private CameraFlash cameraFlash;

    private boolean autoWhiteBalance;

    private int displayOrientation;

    public Camera2Controller(@NonNull Context context, @NonNull CameraView cameraView,
                             @Nullable CameraControllerCallback controllerCallback) {
        this.cameraView = cameraView;
        this.controllerCallback = controllerCallback;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraFacing.BACK;
        cameraView.setCameraViewChangedListener(this::startPreviewSession);
    }

    @Override
    public boolean start() {
        if (!chooseCameraIdByFacing()) {
            return false;
        }
        camera2Config = new Camera2Config(cameraCharacteristics, aspectRatio, cameraView);
        camera2RequestManager = new Camera2RequestManager(cameraDevice, camera2Config);
        setupCaptureImageReader();
        openCamera();
        return true;
    }

    @Override
    public void stop() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (captureImageReader != null) {
            captureImageReader.close();
            captureImageReader = null;
        }
    }

    @Override
    public boolean isCameraOpened() {
        return cameraDevice != null;
    }

    @Override
    public void setFacing(CameraFacing cameraFacing) {
        if (this.cameraFacing == cameraFacing) {
            return;
        }
        this.cameraFacing = cameraFacing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    public CameraFacing getFacing() {
        return cameraFacing;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if (this.autoFocus == autoFocus) {
            return;
        }
        this.autoFocus = autoFocus;

        try {
            CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                    .setAutoFocus(autoFocus)
                    .setCameraFlash(cameraFlash)
                    .setAutoWhiteBalance(autoWhiteBalance)
                    .setOutputSurface(cameraView.getSurface())
                    .build();

            if (cameraCaptureSession != null) {
                cameraCaptureSession.setRepeatingRequest(previewRequest, this, null);
            }
        } catch (CameraAccessException e) {
            this.autoFocus = !this.autoFocus; // Revert
        }
    }

    @Override
    public boolean getAutoFocus() {
        return autoFocus;
    }

    @Override
    public void setFlash(CameraFlash cameraFlash) {
        if (this.cameraFlash == cameraFlash) {
            return;
        }
        CameraFlash saved = this.cameraFlash;
        this.cameraFlash = cameraFlash;

        try {
            CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                    .setAutoFocus(autoFocus)
                    .setCameraFlash(cameraFlash)
                    .setAutoWhiteBalance(autoWhiteBalance)
                    .setOutputSurface(cameraView.getSurface())
                    .build();

            if (cameraCaptureSession != null) {
                cameraCaptureSession.setRepeatingRequest(previewRequest, this, null);
            }
        } catch (CameraAccessException e) {
            this.cameraFlash = saved; // Revert
        }
    }

    @Override
    public CameraFlash getFlash() {
        return cameraFlash;
    }

    @Override
    public void takePicture() {
        if (autoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    @Override
    public void onReadyForStillPicture() {

    }

    @Override
    public void onPrecaptureRequired() {

    }

    /**
     * <p>Chooses a camera ID by the specified camera cameraFacing ({@link #cameraFacing}).</p>
     * <p>This rewrites {@link #cameraId}, {@link #cameraCharacteristics}, and optionally
     * {@link #cameraFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() throws NullPointerException {
        try {
            final String[] ids = cameraManager.getCameraIdList();

            if (cameraFacing == CameraFacing.BACK || cameraFacing == CameraFacing.FRONT) {
                if (ids.length == 0) { // No camera
                    throw new RuntimeException("No camera available.");
                }
                for (String id : ids) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    // Legacy hardware level doesn't support Camera2API (only in compatibility mode without auto-focus)
                    if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        continue;
                    }
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing == null) {
                        throw new NullPointerException("Unexpected state: LENS_FACING null");
                    }
                    if (lensFacing == cameraFacing.getLensFacing()) {
                        cameraId = id;
                        cameraCharacteristics = characteristics;
                        return true;
                    }
                }
            } else {
                cameraId = ids[0];
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer level = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    return false;
                }
                Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                cameraFacing = CameraFacing.byLensFacing(lensFacing);
                if (cameraFacing == CameraFacing.EXTERNAL || cameraFacing == CameraFacing.UNKNOWN) {
                    // The operation can reach here when the only camera device is an external one.
                    // We treat it as cameraFacing back.
                    cameraFacing = CameraFacing.BACK;
                }
                return true;
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
        return false;
    }

    /**
     * <p>Starts opening a camera device.</p>
     */
    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    controllerCallback.onCameraOpened();
                    setupCaptureImageReader();
                    startPreviewSession();
                }

                @Override
                public void onClosed(@NonNull CameraDevice camera) {
                    controllerCallback.onCameraClosed();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Timber.e("onError: " + camera.getId() + " (" + error + ")");
                    cameraDevice = null;
                }

            }, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + cameraId, e);
        }
    }

    private void setupCaptureImageReader() {
        if (captureImageReader != null) {
            captureImageReader.close();
        }
        Size largest = camera2Config.getLargestCaptureSize();
        captureImageReader = ImageReader.newInstance(largest.getWidth(),
                                                     largest.getHeight(),
                                                     ImageFormat.JPEG,
                                                     CAPTURE_IMAGE_BUFFER_SIZE);

        captureImageReader.setOnImageAvailableListener(reader -> {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                }
            }
        }, null);
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>The result will be continuously processed in {@link #previewSessionCallback}.</p>
     */
    private void startPreviewSession() {
        if (!isCameraOpened() || !cameraView.isReady() || captureImageReader == null) {
            return;
        }
        Size previewSize = camera2Config.getPreviewOptimalSize();
        cameraView.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        try {
            cameraDevice.createCaptureSession(Arrays.asList(cameraView.getSurface(), captureImageReader.getSurface()),
                                              previewSessionCallback,
                                              null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to start camera session");
        }
    }

    private final CameraCaptureSession.StateCallback previewSessionCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (cameraDevice == null) {
                return;
            }
            cameraCaptureSession = session;
            try {
                CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                        .setAutoFocus(autoFocus)
                        .setCameraFlash(cameraFlash)
                        .setAutoWhiteBalance(autoWhiteBalance)
                        .setOutputSurface(cameraView.getSurface())
                        .build();

                cameraCaptureSession.setRepeatingRequest(previewRequest, Camera2Controller.this, null);
            } catch (CameraAccessException e) {
                Timber.e("Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Timber.e("Failed to start camera preview.", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Timber.e("Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            if (cameraCaptureSession != null && cameraCaptureSession.equals(session)) {
                cameraCaptureSession = null;
            }
        }
    };

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            CaptureRequest lockFocusRequest = camera2RequestManager.newPreviewRequestBuilder()
                    .setAutoFocus(autoFocus)
                    .setCameraFlash(cameraFlash)
                    .setAutoWhiteBalance(autoWhiteBalance)
                    .setOutputSurface(cameraView.getSurface())
                    // Trigger auto focus
                    .setKeyValue(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
                    .build();

            setCaptureState(STATE_LOCKING);
            cameraCaptureSession.capture(lockFocusRequest, this, null);
        } catch (CameraAccessException e) {
            Timber.e("Failed to lock focus.", e);
        }
    }

    /**
     * Captures a still picture.
     */
    private void captureStillPicture() {
        try {
            CaptureRequest stillPictureRequest = camera2RequestManager.newCaptureRequestBuilder()
                    .setAutoFocus(autoFocus)
                    .setCameraFlash(cameraFlash)
                    .setAutoWhiteBalance(autoWhiteBalance)
                    .setOutputSurface(cameraView.getSurface())
                    .setDisplayOrientation(cameraFacing, cameraView.getDisplayOrientation())
                    .build();
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.capture(stillPictureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            }, null);
        } catch (CameraAccessException e) {
            Timber.e("Cannot capture a still picture.", e);
        }
    }

    /**
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still picture.
     */
    private void unlockFocus() {
        try {
            Camera2RequestManager.PreviewRequestBuilder unlockFocusBuilder = camera2RequestManager.newPreviewRequestBuilder()
                    .setAutoFocus(autoFocus)
                    .setCameraFlash(cameraFlash)
                    .setAutoWhiteBalance(autoWhiteBalance)
                    .setOutputSurface(cameraView.getSurface())
                    // Trigger auto focus
                    .setKeyValue(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            cameraCaptureSession.capture(unlockFocusBuilder.build(), this, null);
            unlockFocusBuilder.setKeyValue(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            cameraCaptureSession.setRepeatingRequest(unlockFocusBuilder.build(), this, null);
            setCaptureState(STATE_PREVIEW);
        } catch (CameraAccessException e) {
            Timber.e("Failed to restart camera preview.", e);
        }
    }
}
