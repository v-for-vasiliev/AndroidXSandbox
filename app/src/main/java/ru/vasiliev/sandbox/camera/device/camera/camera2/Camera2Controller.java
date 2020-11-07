package ru.vasiliev.sandbox.camera.device.camera.camera2;

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

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import ru.vasiliev.sandbox.camera.device.camera.common.CameraController;
import ru.vasiliev.sandbox.camera.device.camera.common.CameraPreview;
import ru.vasiliev.sandbox.camera.device.camera.data.CaptureMetadata;
import ru.vasiliev.sandbox.camera.device.camera.util.AspectRatio;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash;
import ru.vasiliev.sandbox.camera.device.camera.util.Size;
import timber.log.Timber;

import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2Config.ASPECT_RATIO_16_9;
import static ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2Config.CAPTURE_IMAGE_BUFFER_SIZE;

public class Camera2Controller extends Camera2StateMachine implements CameraController {

    // Android Camera2 API objects
    private CameraManager cameraManager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCharacteristics cameraCharacteristics;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader captureImageReader;

    // Camera2 objects
    private CameraPreview cameraPreview;
    private Camera2Config camera2Config;
    private Camera2RequestManager camera2RequestManager;

    // Camera preview/capture params
    private CameraFacing cameraFacing;
    private AspectRatio aspectRatio;
    private boolean autoFocus;
    private CameraFlash cameraFlash;
    private boolean autoWhiteBalance;

    // Result streams
    private PublishSubject<byte[]> imageCaptureStream = PublishSubject.create();
    private PublishSubject<CaptureMetadata> imageMetadataStream = PublishSubject.create();
    private PublishSubject<byte[]> imageProcessorStream = PublishSubject.create();

    public Camera2Controller(@NonNull Context context, @NonNull CameraPreview cameraPreview) {
        setupCameraDefaultParams();
        this.cameraPreview = cameraPreview;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.cameraPreview.setPreviewSurfaceChangedListener(this::startPreviewSession);
    }

    private void setupCameraDefaultParams() {
        autoFocus = false;
        cameraFacing = CameraFacing.BACK;
        aspectRatio = ASPECT_RATIO_16_9;
        cameraFlash = CameraFlash.FLASH_OFF;
    }

    @Override
    public boolean start() {
        if (!chooseCameraIdByFacing()) {
            return false;
        }
        camera2Config = new Camera2Config(cameraCharacteristics, aspectRatio, cameraPreview);
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

    CameraDevice getCameraDevice() {
        return cameraDevice;
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

        if (isCameraOpened()) {
            try {
                CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                        .setOutputSurface(cameraPreview.getSurface())
                        .build();

                if (cameraCaptureSession != null) {
                    cameraCaptureSession.setRepeatingRequest(previewRequest, this, null);
                }
            } catch (CameraAccessException e) {
                this.autoFocus = !this.autoFocus; // Revert
            }
        }
    }

    @Override
    public boolean getAutoFocus() {
        return autoFocus;
    }

    @Override
    public void setFlash(CameraFlash cameraFlash) {
        if (!camera2Config.isFlashSupported() || this.cameraFlash == cameraFlash) {
            return;
        }

        CameraFlash saved = this.cameraFlash;
        this.cameraFlash = cameraFlash;

        if (isCameraOpened()) {
            try {
                CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                        .setOutputSurface(cameraPreview.getSurface())
                        .build();

                if (cameraCaptureSession != null) {
                    cameraCaptureSession.setRepeatingRequest(previewRequest, this, null);
                }
            } catch (CameraAccessException e) {
                this.cameraFlash = saved; // Revert
            }
        }
    }

    @Override
    public CameraFlash getFlash() {
        return cameraFlash;
    }

    @Override
    public void setAutoWhiteBalance(boolean autoWhiteBalance) {
        this.autoWhiteBalance = autoWhiteBalance;
    }

    @Override
    public boolean getAutoWhiteBalance() {
        return autoWhiteBalance;
    }

    @Override
    public void takePicture() {
        if (!autoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    @Override
    public Observable<byte[]> getImageCaptureStream() {
        return imageCaptureStream;
    }

    @Override
    public Observable<CaptureMetadata> getImageMetadataStream() {
        return imageMetadataStream;
    }

    @Override
    public Observable<byte[]> getImageProcessorStream() {
        return imageProcessorStream;
    }

    @Override
    public void onReadyForStillPicture() {
        captureStillPicture();
    }

    @Override
    public void onPrecaptureRequired() {
        try {
            CaptureRequest previewRequest = camera2RequestManager.newPreviewRequestBuilder()
                    .setOutputSurface(cameraPreview.getSurface())
                    // Trigger precapture
                    .setKeyValue(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                 CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
                    .build();
            cameraCaptureSession.capture(previewRequest, this, null);
            setCaptureState(STATE_PRECAPTURE);
        } catch (CameraAccessException e) {
            Timber.e("Failed to run precapture sequence.", e);
        }

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
                    camera2RequestManager = new Camera2RequestManager(Camera2Controller.this,
                                                                      camera2Config,
                                                                      cameraPreview);
                    setupCaptureImageReader();
                    startPreviewSession();
                }

                @Override
                public void onClosed(@NonNull CameraDevice camera) {
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
                    imageCaptureStream.onNext(data);
                }
            }
        }, null);
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>The result will be continuously processed in {@link #previewSessionCallback}.</p>
     */
    private void startPreviewSession() {
        if (!isCameraOpened() || !cameraPreview.isReady() || captureImageReader == null) {
            return;
        }
        Size previewSize = camera2Config.getPreviewOptimalSize();
        cameraPreview.setPreviewBufferSize(previewSize.getWidth(), previewSize.getHeight());
        try {
            cameraDevice.createCaptureSession(Arrays.asList(cameraPreview.getSurface(),
                                                            captureImageReader.getSurface()),
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
                        .setOutputSurface(cameraPreview.getSurface())
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
                    .setOutputSurface(cameraPreview.getSurface())
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
                    .setOutputSurface(cameraPreview.getSurface())
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
                    .setOutputSurface(cameraPreview.getSurface())
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
