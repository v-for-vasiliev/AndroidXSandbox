package ru.vasiliev.sandbox.camera2.presentation.action;

import android.graphics.Bitmap;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import moxy.InjectViewState;
import ru.vasiliev.sandbox.App;
import ru.vasiliev.sandbox.BuildConfig;
import ru.vasiliev.sandbox.camera2.data.action.CameraAction;
import ru.vasiliev.sandbox.camera2.data.action.CameraActionKind;
import ru.vasiliev.sandbox.camera2.data.result.CameraResult;
import ru.vasiliev.sandbox.camera2.data.result.CameraResultProvider;
import ru.vasiliev.sandbox.camera2.framework.camera2.Camera2Result;
import ru.vasiliev.sandbox.camera2.framework.camera2.exception.CameraInvalidModeException;
import ru.vasiliev.sandbox.camera2.framework.camera2.exception.CameraNotReadyToCaptureException;
import ru.vasiliev.sandbox.camera2.utils.ImageUtils;
import ru.vasiliev.sandbox.common.presentation.BaseMoxyPresenter;
import timber.log.Timber;


@InjectViewState
public class CameraActionPresenter extends BaseMoxyPresenter<CameraActionView> {

    private int flashStatus;
    private CameraAction cameraAction;
    private CameraResultProvider cameraResultProvider = CameraResultProvider.getInstance();
    private boolean postCaptureBarcodeDetectionEnabled;
    private BarcodeDetector postCaptureBarcodeDetector;

    CameraActionPresenter(int flashStatus) {
        postCaptureBarcodeDetectionEnabled = BuildConfig.CAMERA2_POST_CAPTURE_BARCODE_DETECTION_ENABLED;
        if (postCaptureBarcodeDetectionEnabled) {
            postCaptureBarcodeDetector = new BarcodeDetector.Builder(App.Companion.getInstance()).setBarcodeFormats(
                    Barcode.CODE_128)
                    .build();
        }
        this.flashStatus = flashStatus;
    }

    void handleResult(Observable<Camera2Result> resultObservable) {
        getViewState().showProgress();
        addSubscription(resultObservable.doOnNext(camera2Result -> {
            if (postCaptureBarcodeDetectionEnabled) {
                detectBarcodeAndUpdateResult(camera2Result);
            }
        })
                                .subscribe(camera2Result -> {
                                    CameraActionKind kind = cameraAction.getKind();
                                    if (kind == CameraActionKind.PHOTO_AND_BARCODE) {
                                        if (!camera2Result.hasBarcode() ||
                                            isNotValidBarcode(camera2Result.getBarcode())) {
                                            getViewState().showBarcodeNotFoundError();
                                            getViewState().showCaptureControls();
                                        } else if (cameraResultProvider.searchBarcode(camera2Result.getBarcode()) !=
                                                   null) {
                                            CameraResult alreadyScannedResult = cameraResultProvider.searchBarcode(
                                                    camera2Result.getBarcode());
                                            if (alreadyScannedResult.getAction()
                                                    .isMultiPageDocument()) {
                                                getViewState().showBarcodeAlreadyScannedError(String.format(Locale.getDefault(),
                                                                                                            "%s, стр. %d",
                                                                                                            alreadyScannedResult.getAction()
                                                                                                                    .getDescription(),
                                                                                                            alreadyScannedResult.getAction()
                                                                                                                    .getIndex()));
                                            } else {
                                                getViewState().showBarcodeAlreadyScannedError(alreadyScannedResult.getAction()
                                                                                                      .getDescription());
                                            }
                                        } else {
                                            getViewState().hideControlPanel();
                                            cameraResultProvider.put(new CameraResult.Builder(cameraAction).setCamera2Result(
                                                    camera2Result)
                                                                             .build());
                                            getViewState().onActionCompleted();
                                        }
                                    } else { // PHOTO
                                        getViewState().hideControlPanel();
                                        cameraResultProvider.put(new CameraResult.Builder(cameraAction).setCamera2Result(
                                                camera2Result)
                                                                         .build());
                                        getViewState().onActionCompleted();
                                    }
                                }, throwable -> {
                                    if (throwable instanceof CameraNotReadyToCaptureException ||
                                        throwable instanceof CameraInvalidModeException) {
                                        Timber.w(throwable.getMessage());
                                        getViewState().showCaptureControls();
                                    } else {
                                        getViewState().showErrorAndCloseCamera("При работе с камерой произошла ошибка.");
                                    }
                                }));
    }

    void handleResult(String barcode) {
        if (isNotValidBarcode(barcode)) {
            getViewState().showBarcodeFormatError();
        } else if (cameraResultProvider.searchBarcode(barcode) != null) {
            CameraResult alreadyScannedResult = cameraResultProvider.searchBarcode(barcode);
            if (alreadyScannedResult.getAction()
                    .isMultiPageDocument()) {
                getViewState().showBarcodeAlreadyScannedError(String.format(Locale.getDefault(),
                                                                            "%s, стр. %d",
                                                                            alreadyScannedResult.getAction()
                                                                                    .getDescription(),
                                                                            alreadyScannedResult.getAction()
                                                                                    .getIndex()));
            } else {
                getViewState().showBarcodeAlreadyScannedError(alreadyScannedResult.getAction()
                                                                      .getDescription());
            }
        } else if (cameraAction.getKind() == CameraActionKind.BARCODE) {
            cameraResultProvider.put(new CameraResult.Builder(cameraAction).setBarcode(barcode)
                                             .build());
            getViewState().onActionCompleted();
        } else {
            getViewState().showBarcode(barcode);
        }
    }

    private boolean isNotValidBarcode(String barcode) {
        if (cameraAction.getScanPattern() == null) {
            return false;
        }
        Pattern pattern = Pattern.compile(cameraAction.getScanPattern());
        Matcher matcher = pattern.matcher(barcode);
        return !matcher.matches();
    }

    private void detectBarcodeAndUpdateResult(Camera2Result camera2Result) {
        CameraActionKind kind = cameraAction.getKind();
        if (kind == CameraActionKind.PHOTO_AND_BARCODE/* && !camera2Result.hasBarcode()*/) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(camera2Result.getImageBase64());
            SparseArray<Barcode> barcodes = postCaptureBarcodeDetector.detect(new Frame.Builder().setBitmap(bitmap)
                                                                                      .build());
            for (int i = 0; i < barcodes.size(); i++) {
                int key = barcodes.keyAt(i);
                Barcode barcode = barcodes.get(key);
                if (barcode != null && barcode.rawValue != null &&
                    !barcode.rawValue.equals(camera2Result.getBarcode())) {
                    camera2Result.updateBarcode(barcode.rawValue);
                    break;
                }
            }
        }
    }

    int getFlashStatus() {
        return flashStatus;
    }

    void setFlashStatus(int flashStatus) {
        this.flashStatus = flashStatus;
    }

    CameraAction getCameraAction() {
        return cameraAction;
    }

    void setCameraAction(CameraAction cameraAction) {
        this.cameraAction = cameraAction;
    }
}
