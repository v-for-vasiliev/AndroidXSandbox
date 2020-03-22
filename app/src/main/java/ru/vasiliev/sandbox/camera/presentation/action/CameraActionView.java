package ru.vasiliev.sandbox.camera.presentation.action;

import moxy.MvpView;
import moxy.viewstate.strategy.OneExecutionStateStrategy;
import moxy.viewstate.strategy.StateStrategyType;

@StateStrategyType(OneExecutionStateStrategy.class)
interface CameraActionView extends MvpView {

    void showProgress();

    void showCaptureControls();

    void hideControlPanel();

    void showControlPanel();

    void showBarcode(String barcode);

    void showBarcodeFormatError();

    void showBarcodeAlreadyScannedError(String documentDescription);

    void showBarcodeNotFoundError();

    void onActionCompleted();

    void showError(String error);

    void showErrorAndCloseCamera(String error);
}