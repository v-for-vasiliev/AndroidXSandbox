package ru.vasiliev.sandbox.camera.presentation.camera;

import java.util.ArrayList;

import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import ru.vasiliev.sandbox.camera.data.action.CameraAction;
import ru.vasiliev.sandbox.camera.data.result.CameraResult;

@StateStrategyType(AddToEndSingleStrategy.class)
interface CameraView extends MvpView {

    void setupActionFragment(CameraAction cameraAction);

    void setupPreviewFragment(ArrayList<CameraResult> cameraResults, boolean hasNextAction, int visiblePreviewIndex);

    void onCaptureError(String error);

    void onNavigateBack();

    void onCaptureComplete();

    void onCaptureCancelled();
}