package ru.vasiliev.sandbox.camera2.framework.camera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import ru.vasiliev.sandbox.camera2.framework.camera2.exception.CameraInvalidModeException;
import ru.vasiliev.sandbox.camera2.framework.camera2.exception.CameraNotReadyToCaptureException;
import ru.vasiliev.sandbox.camera2.framework.scanner.BarcodeScanner;
import ru.vasiliev.sandbox.camera2.presentation.view.AutoFitTextureView;
import ru.vasiliev.sandbox.camera2.presentation.view.FocusSurfaceView;
import ru.vasiliev.sandbox.camera2.utils.ImageUtils;
import ru.vasiliev.sandbox.common.java.Optional;
import timber.log.Timber;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_CONVERGED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_PRECAPTURE;
import static ru.vasiliev.sandbox.camera2.framework.camera2.CameraDbg.dbg;
import static timber.log.Timber.e;

// Helpful topics:

// Lock focus transitions
// https://developer.android.com/reference/android/hardware/camera2/CaptureResult.html#CONTROL_AF_STATE

// Samsung auto focus bug, Camera2Basic Google sample stuck on {STATE_WAITING_LOCK && AF_STATE =
// CONTROL_AF_STATE_ACTIVE_SCAN} when using CONTROL_AF_MODE_CONTINUOUS_PICTURE.
// On Samsung devices seems to be too little precision at the lens clearance, and sometimes they are stuck at focus
// lock/crear.
// https://stackoverflow.com/questions/33922670/camera2-api-autofocus-with-samsung-s5/36251851
// https://stackoverflow.com/questions/50261205/autofocus-in-samsung-s8

// LIMITED device level capabilities:
// https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html#REQUEST_AVAILABLE_CAPABILITIES
// https://developer.android.com/reference/android/hardware/camera2/CameraMetadata.html#REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE

@SuppressWarnings("ALL")
public class Camera2Api implements View.OnTouchListener {

    // Flash modes
    public static final int FLASH_AUTO = 0;
    public static final int FLASH_TURN_ON = 1;
    public static final int FLASH_TURN_OFF = 2;
    private static final boolean DBG = true;
    /**
     * Max empty scans count indicates barcode lost from camera.
     */
    private static final int BARCODE_LOST_EMPTY_SCANS_COUNT_FACTOR = 15;
    // Focus constants
    private static final int FOCUS_RECT_SIZE = 120;
    private static final int FOCUS_RECT_CORNER_SIZE = 30;
    private static final int FOCUS_CLEAR_TIMEOUT_MS = 1000;
    private static final int FOCUS_LOCKING_COLOR = Color.LTGRAY;
    private static final int FOCUS_LOCKED_COLOR = Color.WHITE;
    private static final int FOCUS_LOCK_ERROR_COLOR = Color.RED;
    /**
     * Default quality
     */
    private static final int DEFAULT_QUALITY = -1;
    /**
     * Default focus state
     */
    private static final boolean DEFAULT_FOCUS_BEFORE_CAPTURE_STATE = true;
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_TAKING_PICTURE = 4;
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_CAPTURE_WIDTH = 1920;
    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_CAPTURE_HEIGHT = 1080;
    /**
     * Barcode image width
     */
    private static final int MAX_SCAN_IMAGE_WIDTH = 1920;
    /**
     * Barcode image height
     */
    private static final int MAX_SCAN_IMAGE_HEIGHT = 1080;
    /**
     * Hardware camera max size delta
     */
    private static final int MAX_OUTPUT_SIZE_DELTA = 20;
    /**
     * Max allow apspect ratio delta
     */
    private static final float MAX_ASPECT_RATIO_DELTA = 0.15f;
    /**
     * Capture image reader buffer size
     */
    private static final int CAPTURE_BUFFER_SIZE = 2;
    /**
     * Scan image reader buffer size
     */
    private static final int SCAN_BUFFER_SIZE = 6;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundTaskHandler;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundTaskThread;
    /**
     * ID of the current {@link CameraDevice}.
     */
    private String cameraId;
    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession cameraCaptureSession;
    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice cameraDevice;
    /**
     * The {@link Size} of camera preview.
     */
    private Size previewLayerOptimalSize;
    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader capturedImageReader;
    /**
     * An {@link ImageReader} that handles image scan for barcode.
     */
    private ImageReader barcodeImageReader;
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder precaptureRequestBuilder;
    /**
     * {@link CaptureRequest} generated by {@link #precaptureRequestBuilder}
     */
    private CaptureRequest precaptureRequest;
    /**
     * The current state of camera state for taking pictures.
     *
     * @see #precaptureCallback
     */
    private int cameraState = STATE_PREVIEW;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    /**
     * Indicator of the current camera device supports flash or not.
     */
    private boolean flashSupported;
    /**
     * Flash status
     */
    private int flashStatus = 0;
    /**
     * Orientation of the camera sensor
     */
    private int sensorOrientation;
    /**
     * Camera characteristics
     */
    private CameraCharacteristics cameraCharacteristics;
    private CameraConfig cameraConfig;
    /**
     * Manual focus indicator recatangle
     */
    private Rect manualFocusIndicatorRect;
    /**
     * Manual focus in-progress indicator
     */
    private boolean manualFocusEngaged = false;
    /**
     * Manual focus locked indicator
     */
    private boolean manualFocusLocked = false;
    /**
     * Context of host activity/fragment
     */
    private Context context;
    /**
     * Preview layout
     */
    private AutoFitTextureView previewLayer;
    /**
     * Focus drawing layout
     */
    private FocusSurfaceView focusLayer;
    /**
     * Barcode scanner
     */
    private BarcodeScanner barcodeScanner;
    /**
     * Capture event's listener
     */
    private Camera2ApiListener camera2ApiListener;
    /**
     * Capturing image quality (1..100)
     */
    private int captureQuality = DEFAULT_QUALITY;
    /**
     * Flag which indicates lock focus before capture if it not locked manually or not
     */
    private boolean lockFocusBeforeCapture = DEFAULT_FOCUS_BEFORE_CAPTURE_STATE;
    /**
     * Subject for captured images
     */
    private PublishSubject<Image> imageSubject = PublishSubject.create();
    /**
     * Callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener capturedImageAvailableListener = getCapturedImageAvailableListener();
    /**
     * Subject for captured images metadata
     */
    private PublishSubject<Camera2Metadata> metadataSubject = PublishSubject.create();
    /**
     * Flag which indicates barcode scan in progress
     */
    private volatile boolean scanningBarcode = false;
    /**
     * Flag which indicates capture in progress
     */
    private volatile boolean capturingPhoto = false;
    /**
     * Executor service for scanning operations
     */
    private ExecutorService scanExecutor = Executors.newFixedThreadPool(1);
    /**
     * Camera work mode {@link Camera2Mode}
     */
    private Camera2Mode camera2Mode;
    /**
     * Barcode scan result
     */
    private String barcode;
    /**
     * Scans with no barcode found count. Need to indicate barcode lost more precisely
     */
    private int barcodeEmptyScansCount;
    /**
     * Callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * preview frame read.
     */
    private final ImageReader.OnImageAvailableListener barcodeImageAvailableListener = getBarcodeImageAvailableListener();

    // Callbacks
    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback precaptureCallback = getPrecaptureResultCallback();
    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback cameraStateCallback = getCameraOpenStateCallback();
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener previewSurfaceListener = getPreviewSurfaceListener();
    /**
     * Manual focus request callback
     */
    private CameraCaptureSession.CaptureCallback manualFocusCallback = getManualFocusResultCallback();

    /**
     * Constructor
     *
     * @param context            context
     * @param previewLayer       {@link AutoFitTextureView} preview of camera capture view layout
     * @param focusLayer         {@link FocusSurfaceView} view of camera capture view layout
     * @param camera2ApiListener listener to handle capture events
     */
    public Camera2Api(Context context, AutoFitTextureView previewLayer, FocusSurfaceView focusLayer,
                      BarcodeScanner barcodeScanner, Camera2Mode camera2Mode, Camera2ApiListener camera2ApiListener) {
        this.context = context;
        this.previewLayer = previewLayer;
        this.focusLayer = focusLayer;
        this.barcodeScanner = barcodeScanner;
        this.camera2Mode = camera2Mode;
        this.camera2ApiListener = camera2ApiListener;
    }

    private static Camera2Metadata createMetadata(TotalCaptureResult result, Camera2FocusMode focusMode) {
        Camera2Metadata.Builder metadataBuilder = new Camera2Metadata.Builder();
        try {
            metadataBuilder.setQuality(
                    Objects.requireNonNull(result.get(CaptureResult.JPEG_QUALITY)) & 0xFF); // Remove sign bits
        } catch (final Throwable ignore) {
        }
        metadataBuilder.setTimestamp(System.currentTimeMillis());
        metadataBuilder.setFocusMode(focusMode);
        return metadataBuilder.build();
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as
     * the respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices              The list of sizes that the camera supports for the intended output
     *                             class
     * @param previewSurfaceWidth  The width of the texture view relative to sensor coordinate
     * @param previewSurfaceHeight The height of the texture view relative to sensor coordinate
     * @param maxPreviewWidth      The maximum width that can be chosen
     * @param maxPreviewHeight     The maximum height that can be chosen
     * @param captureSize          The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int previewSurfaceWidth, int previewSurfaceHeight,
                                          int maxPreviewWidth, int maxPreviewHeight, Size captureSize) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() <= maxPreviewWidth && option.getHeight() <= maxPreviewHeight &&
                isAcceptableAspectRatio(captureSize, option)) {
                if (option.getWidth() >= previewSurfaceWidth && option.getHeight() >= previewSurfaceHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new ViewSizeComparator());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new ViewSizeComparator());
        } else {
            e("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private static boolean isAcceptableAspectRatio(Size captureSize, Size targetSize) {
        float captureAspectRatio = captureSize.getWidth() / (float) captureSize.getHeight();
        float targetAspectRatio = targetSize.getWidth() / (float) targetSize.getHeight();
        return Math.abs(captureAspectRatio - targetAspectRatio) < MAX_ASPECT_RATIO_DELTA;
    }

    /**
     * Capture image with default Camera2API quality
     */
    public Observable<Camera2Result> capture() {
        return capture(DEFAULT_QUALITY, DEFAULT_FOCUS_BEFORE_CAPTURE_STATE);
    }

    /**
     * Capture image with specified quality
     *
     * @param captureQuality image quality percent (0..100)
     */
    public Observable<Camera2Result> capture(int captureQuality, boolean lockFocusBeforeCapture) {
        dbg("capture()");
        if (getCameraState() != STATE_PREVIEW || isCapturingPhoto() || manualFocusEngaged) {
            dbg("CameraNotReadyToCaptureException()");
            return Observable.error(new CameraNotReadyToCaptureException());
        }

        if (camera2Mode == Camera2Mode.SCAN) {
            dbg("CameraInvalidModeException()");
            return Observable.error(new CameraInvalidModeException());
        }

        setpCapturingPhoto(true);

        this.captureQuality = captureQuality;
        this.lockFocusBeforeCapture = lockFocusBeforeCapture;

        if (manualFocusLocked || !lockFocusBeforeCapture) {
            captureStillPicture();
        } else {
            captureStillPictureLocked();
        }

        return Observable.zip(imageSubject.cast(Image.class)
                                      .take(1)
                                      .map(ImageUtils::imageToBase64),
                              metadataSubject.cast(Camera2Metadata.class)
                                      .take(1),
                              Observable.just(Optional.ofNullable(barcode)),
                              Camera2Result::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void stop() {
        closeCamera();
        stopBackgroundThread();
    }

    public void start() {
        startBackgroundThread();
        if (previewLayer.isAvailable()) {
            openCamera(previewLayer.getWidth(), previewLayer.getHeight());
        } else {
            previewLayer.setSurfaceTextureListener(previewSurfaceListener);
        }
    }

    public void setFlashStatus(int flashStatus) {
        this.flashStatus = flashStatus;
    }

    public void updateFlashStatus(int flashStatus) {
        setFlashStatus(flashStatus);
        createCameraPreviewSession();
    }

    private synchronized boolean isCapturingPhoto() {
        return capturingPhoto;
    }

    private synchronized void setpCapturingPhoto(boolean capturingPhoto) {
        this.capturingPhoto = capturingPhoto;
    }

    public synchronized void discardBarcode() {
        barcode = null;
    }

    private synchronized boolean isScanningBarcode() {
        return scanningBarcode;
    }

    private synchronized void setScanningBarcode(boolean scanningBarcode) {
        this.scanningBarcode = scanningBarcode;
    }

    private synchronized boolean isBarcodeScanned() {
        return barcode != null;
    }

    /* Callbacks */

    private synchronized int getCameraState() {
        return cameraState;
    }

    private synchronized void setCameraState(int cameraState) {
        if (this.cameraState != cameraState) {
            this.cameraState = cameraState;
            switch (cameraState) {
                case STATE_PREVIEW:
                    dbg("State changed: STATE_PREVIEW");
                    break;
                case STATE_WAITING_LOCK:
                    dbg("State changed: STATE_WAITING_LOCK");
                    break;
                case STATE_WAITING_NON_PRECAPTURE:
                    dbg("State changed: STATE_WAITING_NON_PRECAPTURE");
                    break;
                case STATE_WAITING_PRECAPTURE:
                    dbg("State changed: STATE_WAITING_PRECAPTURE");
                    break;
                case STATE_TAKING_PICTURE:
                    dbg("State changed: STATE_TAKING_PICTURE");
                    break;
                default:
                    dbg("State changed: %d", cameraState);
                    break;
            }
        }
    }

    private final CameraDevice.StateCallback getCameraOpenStateCallback() {
        return new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                cameraConfig.printCameraCharacteristics();
                if (cameraConfig.isDeviceCameraSupported(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)) {
                    // This method is called when the camera is opened.  We start camera preview here.
                    cameraOpenCloseLock.release();
                    Camera2Api.this.cameraDevice = cameraDevice;
                    createCameraPreviewSession();
                    initFocusOverlay();
                } else {
                    cameraOpenCloseLock.release();
                    Camera2Api.this.onError(null, "Данный телефон не поддерживает текущую версию модуля камеры.");
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraOpenCloseLock.release();
                cameraDevice.close();
                Camera2Api.this.cameraDevice = null;
                Camera2Api.this.onError(null, "Камера недоступна.");
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                cameraOpenCloseLock.release();
                cameraDevice.close();
                Camera2Api.this.cameraDevice = null;
                Camera2Api.this.onError(null, "Камера недоступна.");
            }

        };
    }

    private TextureView.SurfaceTextureListener getPreviewSurfaceListener() {
        return new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                openCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                configureTransform(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            }

        };
    }

    private CameraCaptureSession.CaptureCallback getPrecaptureResultCallback() {
        return new CameraCaptureSession.CaptureCallback() {

            private void dbgState(String state, Integer afMode, Integer afState, Integer aeMode, Integer aeState) {
                dbg("PrecaptureCallback {%s, %s, %s, %s, %s}",
                    state,
                    Camera2Utils.getAfModeString(afMode),
                    Camera2Utils.getAfStateString(afState),
                    Camera2Utils.getAeModeString(aeMode),
                    Camera2Utils.getAeStateString(aeState));
            }

            private void process(CaptureResult result) {
                Integer afMode = result.get(CaptureResult.CONTROL_AF_MODE);
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                Integer aeMode = result.get(CaptureResult.CONTROL_AE_MODE);
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

                switch (cameraState) {
                    case STATE_PREVIEW: {
                        // We have nothing to do when the camera preview is working normally.
                        dbgState("STATE_PREVIEW", afMode, afState, aeMode, aeState);
                        break;
                    }
                    case STATE_WAITING_LOCK: {
                        dbgState("STATE_WAITING_LOCK", afMode, afState, aeMode, aeState);
                        if (afState == null) {
                            setCameraState(STATE_TAKING_PICTURE);
                            captureStillPicture();
                        } else {
                            switch (afState) {
                                case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                                case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                                case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                                    if (aeState == null || aeState == CONTROL_AE_STATE_CONVERGED) {
                                        setCameraState(STATE_TAKING_PICTURE);
                                        captureStillPicture();
                                    } else {
                                        runPrecaptureSequence();
                                    }
                                    break;
                                case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                                case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                                case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                                case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {
                        dbgState("STATE_WAITING_PRECAPTURE", afMode, afState, aeMode, aeState);
                        if (aeState == null || aeState == CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CONTROL_AE_STATE_FLASH_REQUIRED) {
                            setCameraState(STATE_WAITING_NON_PRECAPTURE);
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {
                        dbgState("STATE_WAITING_NON_PRECAPTURE", afMode, afState, aeMode, aeState);
                        if (aeState == null || aeState != CONTROL_AE_STATE_PRECAPTURE) {
                            setCameraState(STATE_TAKING_PICTURE);
                            captureStillPicture();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                            @NonNull CaptureResult partialResult) {
                process(partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                process(result);
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                dbg("(Precapture callback) onCaptureFailed(reason: %d)", failure.getReason());
                onError(null, "Камера недоступна");
            }
        };
    }

    /**
     * Returns the callback which receives the CaptureResult metadata.
     * Both the {@link #getCaptureResultCallback()} and the {@link #getCapturedImageAvailableListener()}
     * come with a nanosecond timestamp. They are guaranteed to have the same timestamp for the same frame.
     *
     * @return callback
     */
    private CameraCaptureSession.CaptureCallback getCaptureResultCallback() {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                dbg("(Capture callback) onCaptureCompleted()");
                unlockFocus();
                Camera2FocusMode focusMode;
                if (lockFocusBeforeCapture) { // Focus locks automatically before capture
                    focusMode = manualFocusLocked ? Camera2FocusMode.FOCUS_MODE_MANUAL : Camera2FocusMode.FOCUS_MODE_AUTO;
                } else {
                    focusMode = manualFocusLocked ? Camera2FocusMode.FOCUS_MODE_MANUAL : Camera2FocusMode.FOCUS_MODE_OFF;
                }
                metadataSubject.onNext(createMetadata(result, focusMode));
                setpCapturingPhoto(false);
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                dbg("(Capture callback) onCaptureFailed(reason: %d, imageCaptured: %b)",
                    failure.getReason(),
                    failure.wasImageCaptured());
                setpCapturingPhoto(false);
            }
        };
    }

    private CameraCaptureSession.CaptureCallback getManualFocusResultCallback() {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                dbg("(Focus callback) onCaptureCompleted()");
                manualFocusEngaged = false;
                manualFocusLocked = true;
                drawFocus(manualFocusIndicatorRect, FOCUS_LOCKED_COLOR);
                clearOverlayDelayed(FOCUS_CLEAR_TIMEOUT_MS);
                unlockFocus();
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                manualFocusEngaged = false;
                manualFocusLocked = false;
                drawFocus(manualFocusIndicatorRect, FOCUS_LOCK_ERROR_COLOR);
                clearOverlayDelayed(FOCUS_CLEAR_TIMEOUT_MS);
                dbg("(Focus callback) onCaptureFailed(reson: %d)", failure.getReason());
            }
        };
    }

    /**
     * Returns the callback associated with the surface which receives the actual image pixel data
     * for searching within it barcode.
     *
     * @return callback
     */
    private ImageReader.OnImageAvailableListener getBarcodeImageAvailableListener() {
        return reader -> {
            // if (!isBarcodeScanned()) {} in case we want to stop scanner when barcode found
            if (!isScanningBarcode() && !isCapturingPhoto()) {
                scanImageBarcode(reader);
            }/* else {
                // Dry run, otherwise preview texture layer will be freezed
                Image image = reader.acquireNextImage();
                if (image != null) {
                    image.close();
                }
            }*/
        };
    }

    /**
     * Returns the callback associated with the surface which receives the actual image pixel data.
     * Both the {@link #getCaptureResultCallback()} and the {@link #getCapturedImageAvailableListener()}
     * come with a nanosecond timestamp. They are guaranteed to have the same timestamp for the same frame.
     *
     * @return callback
     */
    private ImageReader.OnImageAvailableListener getCapturedImageAvailableListener() {
        return reader -> imageSubject.onNext(reader.acquireNextImage());
    }

    private void scanImageBarcode(ImageReader reader) {
        setScanningBarcode(true);
        scanExecutor.submit(() -> {
            try {
                Image image = reader.acquireLatestImage();
                String barcode = barcodeScanner.scan(image);
                image.close();
                if (barcode != null) {
                    Camera2Api.this.barcode = barcode;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (camera2ApiListener != null) {
                            camera2ApiListener.onBarcodeFound(barcode);
                            dbg("Barcode found: %s", barcode);
                        }
                    });
                } else {
                    barcodeEmptyScansCount++;
                    if (barcodeEmptyScansCount >= BARCODE_LOST_EMPTY_SCANS_COUNT_FACTOR) {
                        barcodeEmptyScansCount = 0;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (camera2ApiListener != null) {
                                camera2ApiListener.onBarcodeLost();
                                dbg("Barcode lost");
                            }
                        });
                    }
                }
            } catch (final Throwable t) {
                dbg("scanImageBarcode()", t);
            } finally {
                setScanningBarcode(false);
            }
        });
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        dbg("startBackgroundThread()");
        backgroundTaskThread = new HandlerThread("CameraBackground");
        backgroundTaskThread.start();
        backgroundTaskHandler = new Handler(backgroundTaskThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        dbg("stopBackgroundThread()");
        try {
            backgroundTaskThread.quitSafely();
            backgroundTaskThread.join();
            backgroundTaskThread = null;
            backgroundTaskHandler = null;
        } catch (final Throwable ignore) {
        }
    }

    /**
     * Kills the background thread and its {@link Handler}.
     */
    private void killBackgroundThread() {
        dbg("killBackgroundThread()");
        try {
            backgroundTaskThread.interrupt();
            backgroundTaskThread.join();
            backgroundTaskThread = null;
            backgroundTaskHandler = null;
        } catch (final Throwable ignore) {
        }
    }

    private void openCamera(int width, int height) {
        dbg("openCamera(%d, %d)", width, height);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            onError(null, "Не получено разрешение на использование камеры.");
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = (Activity) context;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, cameraStateCallback, backgroundTaskHandler);
        } catch (final Throwable t) {
            dbg("openCamera() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        dbg("closeCamera()");
        try {
            cameraOpenCloseLock.acquire();
            if (null != cameraCaptureSession) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != capturedImageReader) {
                capturedImageReader.close();
                capturedImageReader = null;
            }
            if (null != barcodeImageReader) {
                barcodeImageReader.close();
                barcodeImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        dbg("setUpCameraOutputs(%d, %d)", width, height);
        Activity activity = (Activity) context;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                cameraConfig = new CameraConfig(cameraCharacteristics);

                // We don't use a front facing camera.
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                List<Size> listOfCaptureSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                Collections.sort(listOfCaptureSizes, new ViewSizeComparator());
                Size captureSize = listOfCaptureSizes.get(0); // Init with smallest size
                for (Size s : listOfCaptureSizes) {
                    if (s.getWidth() <= (MAX_CAPTURE_WIDTH + MAX_OUTPUT_SIZE_DELTA) &&
                        s.getHeight() <= (MAX_CAPTURE_HEIGHT + MAX_OUTPUT_SIZE_DELTA)) {
                        captureSize = s;
                    }
                }
                // Capture reader
                capturedImageReader = ImageReader.newInstance(captureSize.getWidth(),
                                                              captureSize.getHeight(),
                                                              ImageFormat.JPEG,
                                                              CAPTURE_BUFFER_SIZE);
                capturedImageReader.setOnImageAvailableListener(capturedImageAvailableListener, backgroundTaskHandler);


                // Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
                int displayRotation = activity.getWindowManager()
                        .getDefaultDisplay()
                        .getRotation();
                // noinspection ConstantConditions
                sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Timber.e("Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager()
                        .getDefaultDisplay()
                        .getSize(displaySize);
                int previewSurfaceWidth = width;
                int previewSurfaceHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    previewSurfaceWidth = height;
                    previewSurfaceHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                if (maxPreviewWidth > MAX_CAPTURE_WIDTH) {
                    maxPreviewWidth = MAX_CAPTURE_WIDTH;
                }
                if (maxPreviewHeight > MAX_CAPTURE_HEIGHT) {
                    maxPreviewHeight = MAX_CAPTURE_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewLayerOptimalSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                                            previewSurfaceWidth,
                                                            previewSurfaceHeight,
                                                            maxPreviewWidth,
                                                            maxPreviewHeight,
                                                            captureSize);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = context.getResources()
                        .getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    previewLayer.setAspectRatio(previewLayerOptimalSize.getWidth(),
                                                previewLayerOptimalSize.getHeight());
                } else {
                    previewLayer.setAspectRatio(previewLayerOptimalSize.getHeight(),
                                                previewLayerOptimalSize.getWidth());
                }

                int maxScanWidth = (previewLayerOptimalSize.getWidth() >
                                    MAX_SCAN_IMAGE_WIDTH) ? MAX_SCAN_IMAGE_WIDTH : previewLayerOptimalSize.getWidth();
                int maxScanHeight = (previewLayerOptimalSize.getHeight() >
                                     MAX_SCAN_IMAGE_HEIGHT) ? MAX_SCAN_IMAGE_HEIGHT : previewLayerOptimalSize.getHeight();
                // Max size of YUV scan target in combination with JPEG capture target can't be bigger than preview
                // layer size.
                // https://developer.android.com/reference/android/hardware/camera2/CameraDevice#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)
                List<Size> listOfScanSizes = Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
                Collections.sort(listOfScanSizes, new ViewSizeComparator());
                Size scanSize = listOfScanSizes.get(0);
                for (Size option : listOfScanSizes) {
                    if (option.getWidth() <= maxScanWidth && option.getHeight() <= maxScanHeight &&
                        isAcceptableAspectRatio(previewLayerOptimalSize, option)) {
                        scanSize = option;
                    }
                }

                // Scan images reader, the google recommendation is to use ImageFormat.YUV_420_888 for frame analysis
                barcodeImageReader = ImageReader.newInstance(scanSize.getWidth(),
                                                             scanSize.getHeight(),
                                                             ImageFormat.YUV_420_888,
                                                             SCAN_BUFFER_SIZE);
                barcodeImageReader.setOnImageAvailableListener(barcodeImageAvailableListener, backgroundTaskHandler);

                // Check if the flash is supported.
                Boolean available = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                flashSupported = available == null ? false : available;

                this.cameraId = cameraId;
                return;
            }
        } catch (final Throwable t) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            dbg("setUpCameraOutputs() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} by providing the target output set of Surfaces
     * to the camera device. The active capture session determines the set of potential output Surfaces
     * for the camera device for each capture request. A given request may use all or only some of the outputs.
     * Once the CameraCaptureSession is created, requests can be submitted
     * with {@link CameraCaptureSession#capture}, {@link CameraCaptureSession#captureBurst},
     * {@link CameraCaptureSession#setRepeatingRequest}, or {@link CameraCaptureSession#setRepeatingBurst}.
     */
    private void createCameraPreviewSession() {
        dbg("createCameraPreviewSession()");
        try {
            SurfaceTexture previewLayerTexture = previewLayer.getSurfaceTexture();

            // We configure the size of default buffer to be the size of camera preview we want.
            previewLayerTexture.setDefaultBufferSize(previewLayerOptimalSize.getWidth(),
                                                     previewLayerOptimalSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface previewLayerSurface = new Surface(previewLayerTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            precaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            precaptureRequestBuilder.addTarget(previewLayerSurface);
            // Check camera action requires scan or capture + scan
            if (camera2Mode == Camera2Mode.SCAN || camera2Mode == Camera2Mode.CAPTURE_AND_SCAN) {
                precaptureRequestBuilder.addTarget(barcodeImageReader.getSurface());
            }

            // Add preview surfaces depending on camera2 mode
            List<Surface> previewSurfaces = new ArrayList<>();
            previewSurfaces.add(previewLayerSurface);
            switch (camera2Mode) {
                case CAPTURE_AND_SCAN:
                    previewSurfaces.add(capturedImageReader.getSurface());
                    previewSurfaces.add(barcodeImageReader.getSurface());
                    break;
                case CAPTURE:
                    previewSurfaces.add(capturedImageReader.getSurface());
                    break;
                case SCAN:
                    previewSurfaces.add(barcodeImageReader.getSurface());
                    break;
            }

            // Create a CameraCaptureSession for camera preview
            cameraDevice.createCaptureSession(previewSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    Camera2Api.this.cameraCaptureSession = cameraCaptureSession;
                    try {
                        // Setup preview params, like auto-focus/exposure
                        setupPreviewRequestParams(precaptureRequestBuilder);

                        // Start displaying the camera preview.
                        precaptureRequest = precaptureRequestBuilder.build();
                        Camera2Api.this.cameraCaptureSession.setRepeatingRequest(precaptureRequest,
                                                                                 precaptureCallback,
                                                                                 backgroundTaskHandler);
                    } catch (final Throwable t) {
                        onError(t, "Камера недоступна.");
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    onError(null, "Камера недоступна.");
                }
            }, null);
        } catch (final Throwable t) {
            dbg("createCameraPreviewSession() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    private void setupPreviewRequestParams(CaptureRequest.Builder requestBuilder) {
        String afMode = null, aeMode = null, awbMode = null;
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        if (cameraConfig.isAutoFocusSupported()) {
            int[] afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, afModes)) {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                afMode = "CONTROL_AF_MODE_CONTINUOUS_PICTURE";
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                afMode = "CONTROL_AF_MODE_AUTO";
            }
        }

        if (cameraConfig.isAutoExposureSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            aeMode = "CONTROL_AE_MODE_ON";
        }

        if (cameraConfig.isAWBSupported()) {
            requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            awbMode = "CONTROL_AWB_MODE_AUTO";
        }

        setupFlashMode(requestBuilder);

        dbg("setupPreviewRequestParams(AF: %s, AE: %s, AWB: %s)", afMode, aeMode, awbMode);
    }

    private void setupFlashMode(CaptureRequest.Builder requestBuilder) {
        if (flashSupported) {
            switch (flashStatus) {
                case FLASH_AUTO:
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case FLASH_TURN_OFF:
                    requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    break;
                case FLASH_TURN_ON:
                    requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    break;
            }
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #precaptureCallback} from {@link #captureStillPictureLocked()}.
     * <p>
     * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER is not functional on LEGACY devices. Instead,
     * every request that includes a JPEG-format output target is treated as triggering a still capture, internally
     * executing a precapture trigger.
     * <p>
     * https://developer.android.com/reference/android/hardware/camera2/CaptureRequest.html#CONTROL_AE_PRECAPTURE_TRIGGER
     */
    private void runPrecaptureSequence() {
        dbg("runPrecaptureSequence()");
        try {
            // Tell the camera to trigger precapture sequence.
            precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                         CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Prevent CONTROL_AE_PRECAPTURE_TRIGGER from calling over and over again
            CaptureRequest precaptureRequest = precaptureRequestBuilder.build();
            precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null);

            // Preview callback: wait for the precapture sequence to be set.
            setCameraState(STATE_WAITING_PRECAPTURE);

            cameraCaptureSession.capture(precaptureRequest, precaptureCallback, backgroundTaskHandler);
        } catch (final Throwable t) {
            dbg("runPrecaptureSequence() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #precaptureCallback} from both {@link #captureStillPictureLocked()}.
     */
    private void captureStillPicture() {
        dbg("captureStillPicture(q = %d)", captureQuality);
        try {
            final Activity activity = (Activity) context;
            if (null == activity || null == cameraDevice) {
                return;
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(capturedImageReader.getSurface());

            // Copy request params from preview request.
            setupCaptureRequestParams(captureRequestBuilder,
                                      activity.getWindowManager()
                                              .getDefaultDisplay()
                                              .getRotation());

            // Discard all captures currently pending and in-progress as fast as possible and
            // stop preview repeating request.
            cameraCaptureSession.stopRepeating();

            // Take a shot.
            cameraCaptureSession.capture(captureRequestBuilder.build(), getCaptureResultCallback(), null);
        } catch (final Throwable t) {
            dbg("captureStillPicture() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    private void setupCaptureRequestParams(CaptureRequest.Builder requestBuilder, int rotation) {
        // Use the same AE/AF/AWB/Flash modes as the preview - just copy them from precaptureRequestBuilder
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        // Focus. When manual focus is locked then the lens are already in position, just take a shot
        if (cameraConfig.isAutoFocusSupported() && !manualFocusLocked) {
            int[] afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, afModes)) {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // Exposure
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, precaptureRequest.get(CaptureRequest.CONTROL_AE_MODE));

        // White balance
        requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, precaptureRequest.get(CaptureRequest.CONTROL_AWB_MODE));

        // Capture quality
        if (captureQuality != DEFAULT_QUALITY && captureQuality > 0 && captureQuality <= 100) {
            requestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) ((0xFF) & captureQuality));
        }

        // Orientation
        requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

        // Flash
        setupFlashMode(requestBuilder);
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void captureStillPictureLocked() {
        dbg("captureStillPictureLocked()");
        try {
            // No auto-focus supported.
            if (!cameraConfig.isAutoFocusSupported()) {
                // Preview capture callback: wait for focus lock state.
                setCameraState(STATE_WAITING_LOCK);

                captureStillPicture();
            } else {
                // Tell the camera to lock focus.
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                             CameraMetadata.CONTROL_AF_TRIGGER_START);

                // Preview capture callback: wait for focus lock state.
                setCameraState(STATE_WAITING_LOCK);

                // Tell capture callback to wait for the lock.
                cameraCaptureSession.capture(precaptureRequestBuilder.build(),
                                             precaptureCallback,
                                             backgroundTaskHandler);

                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
            }
        } catch (final Throwable t) {
            dbg("captureStillPictureLocked() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        dbg("unlockFocus()");
        try {
            if (cameraConfig.isAutoFocusSupported()) {
                // Cancel auto-focus trigger.
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                             CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

                setupFlashMode(precaptureRequestBuilder);

                // Capture once to cancel trigger.
                cameraCaptureSession.capture(precaptureRequestBuilder.build(),
                                             precaptureCallback,
                                             backgroundTaskHandler);

                // Restore AF trigger.
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            }

            // Precapture callback: go back to the normal state of preview.
            setCameraState(STATE_PREVIEW);

            // Resume preview
            cameraCaptureSession.setRepeatingRequest(precaptureRequest, precaptureCallback, backgroundTaskHandler);
        } catch (final Throwable t) {
            dbg("unlockFocus() error: ", t);
            onError(t, "Камера недоступна.");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isCapturingPhoto()) {
            return true;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (manualFocusEngaged) {
            // Focus locking in progress, wait to lock request will be completed.
            return true;
        }
        lockFocus(event);
        return true;
    }

    private void lockFocus(MotionEvent event) {
        dbg("lockFocus(%.2f, %.2f)", event.getX(), event.getY());
        try {
            manualFocusEngaged = true;

            /* Determine focus area */

            // This is the rectangle representing the size of the active region of the sensor (i.e.
            // the region that actually receives light from the scene) after any geometric correction
            // has been applied, and should be treated as the maximum size in pixels of any of the
            // image output formats aside from the raw formats.
            final Rect sensorActiveRegion = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            int viewWidth = previewLayer.getWidth();
            int viewHeight = previewLayer.getHeight();

            int overlayWidth = (sensorActiveRegion != null) ? sensorActiveRegion.width() : viewWidth;
            int overlayHeight = (sensorActiveRegion != null) ? sensorActiveRegion.height() : viewHeight;

            int centerX = (int) event.getX();
            int centerY = (int) event.getY();

            // Touch coordinates projection to sensor active region
            int projectionX = ((centerX * overlayWidth) - FOCUS_RECT_SIZE) / viewWidth;
            int projectionY = ((centerY * overlayHeight) - FOCUS_RECT_SIZE) / viewHeight;
            int focusLeft = clamp(projectionX, 0, overlayWidth);
            int focusBottom = clamp(projectionY, 0, overlayHeight);

            Rect focusRect = new Rect(focusLeft,
                                      focusBottom,
                                      focusLeft + FOCUS_RECT_SIZE,
                                      focusBottom + FOCUS_RECT_SIZE);
            MeteringRectangle focusArea = new MeteringRectangle(focusRect, 500);

            // Draw focus rectangle
            drawFocus(this.manualFocusIndicatorRect = new Rect(Math.max(centerX - (FOCUS_RECT_SIZE / 2), 0),
                                                               Math.max(centerY + (FOCUS_RECT_SIZE / 2), 0),
                                                               Math.max(centerX + (FOCUS_RECT_SIZE / 2), 0),
                                                               Math.max(centerY - (FOCUS_RECT_SIZE / 2), 0)));

            // First stop the existing repeating request
            cameraCaptureSession.stopRepeating();

            // Start AF trigger
            precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            // Capture once to apply your settings
            cameraCaptureSession.capture(precaptureRequestBuilder.build(), manualFocusCallback, backgroundTaskHandler);

            // Check if AF and AE regions are supported. If they are supported then apply focus/exposure regions
            if (cameraConfig.isAeMeteringAreaSupported()) {
                dbg("lockFocus(): AE regions are supported");
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
            }
            if (cameraConfig.isAfMeteringAreaSupported()) {
                dbg("lockFocus(): AF regions are supported");
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusArea});
                precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            }

            // Capture once again to set the focus
            cameraCaptureSession.capture(precaptureRequestBuilder.build(), manualFocusCallback, backgroundTaskHandler);
        } catch (Throwable t) {
            clearOverlay();
            manualFocusEngaged = false;
            Timber.e(t, "");
            Toast.makeText(context, "Ошибка фокусировки, перезапустите камеру", Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Initializes overlay, which will be used to show focus area
     */

    private void initFocusOverlay() {
        previewLayer.setOnTouchListener(this);
        clearOverlay();
    }

    /**
     * Clears overlay, removes previously draw focus
     */
    private void clearOverlay() {
        try {
            SurfaceHolder overlayHolder = focusLayer.getHolder();
            if (overlayHolder != null) {
                Canvas canvas = overlayHolder.lockCanvas(null);
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    overlayHolder.unlockCanvasAndPost(canvas);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * Clear overlay with delay
     *
     * @param ms - delay, milliseconds
     */
    private void clearOverlayDelayed(int ms) {
        Observable.timer(ms, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(aLong -> clearOverlay());
    }

    /**
     * Draw focus with default color
     *
     * @param rect focus rectangle
     */
    private void drawFocus(Rect rect) {
        drawFocus(rect, FOCUS_LOCKING_COLOR);
    }

    /**
     * Draw focus
     *
     * @param rect  focus rectangle
     * @param color focus rectangle color
     */
    private void drawFocus(Rect rect, int color) {
        SurfaceHolder overlayHolder = focusLayer.getHolder();
        if (overlayHolder != null) {
            Canvas canvas = overlayHolder.lockCanvas(null);
            if (canvas != null) {
                // Clear canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                if (Build.MANUFACTURER.equalsIgnoreCase("LENOVO")) {
                    paint.setStrokeWidth(4);
                } else {
                    paint.setStrokeWidth(2);
                }
                paint.setColor(color);
                paint.setStrokeJoin(Paint.Join.MITER);
                canvas.drawPath(createFocusCornerPath(rect, FOCUS_RECT_CORNER_SIZE), paint);
                overlayHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private Path createFocusCornerPath(Rect rect, int cornerWidth) {
        Path path = new Path();

        path.moveTo(rect.left, rect.top - cornerWidth);
        path.lineTo(rect.left, rect.top);
        path.lineTo(rect.left + cornerWidth, rect.top);

        path.moveTo(rect.right - cornerWidth, rect.top);
        path.lineTo(rect.right, rect.top);
        path.lineTo(rect.right, rect.top - cornerWidth);

        path.moveTo(rect.left, rect.bottom + cornerWidth);
        path.lineTo(rect.left, rect.bottom);
        path.lineTo(rect.left + cornerWidth, rect.bottom);

        path.moveTo(rect.right - cornerWidth, rect.bottom);
        path.lineTo(rect.right, rect.bottom);
        path.lineTo(rect.right, rect.bottom + cornerWidth);

        return path;
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = (Activity) context;
        if (null == previewLayer || null == previewLayerOptimalSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager()
                .getDefaultDisplay()
                .getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewLayerOptimalSize.getHeight(), previewLayerOptimalSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / previewLayerOptimalSize.getHeight(),
                                   (float) viewWidth / previewLayerOptimalSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        previewLayer.setTransform(matrix);
    }

    private void onError(Throwable t, String message) {
        Timber.e(message);
        if (t != null) {
            Timber.e(t, "");
        }

        if (camera2ApiListener != null) {
            camera2ApiListener.onCameraError(message);
        }

        cameraOpenCloseLock.release();
        closeCamera();
        killBackgroundThread();

    }

    private boolean isLenovo() {
        return Build.MANUFACTURER.equalsIgnoreCase("LENOVO");
    }

    private int clamp(int x, int min, int max) {
        if (x < min) {
            return min;
        } else if (x > max) {
            return max;
        } else {
            return x;
        }
    }

    private boolean contains(int value, int[] array) {
        if (array == null) {
            return false;
        }
        for (int v : array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class ViewSizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
