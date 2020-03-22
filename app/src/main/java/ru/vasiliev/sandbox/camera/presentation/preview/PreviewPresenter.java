package ru.vasiliev.sandbox.camera.presentation.preview;

import android.graphics.Bitmap;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import moxy.InjectViewState;
import ru.vasiliev.sandbox.camera.data.action.CameraActionKind;
import ru.vasiliev.sandbox.camera.data.result.CameraResult;
import ru.vasiliev.sandbox.camera.presentation.preview.viewmodel.PreviewModel;
import ru.vasiliev.sandbox.camera.utils.ImageUtils;
import ru.vasiliev.sandbox.common.presentation.BaseMoxyPresenter;
import timber.log.Timber;


@InjectViewState
public class PreviewPresenter extends BaseMoxyPresenter<PreviewView> {

    private ArrayList<CameraResult> cameraResultList;
    private boolean hasNextAction;
    private int displayRotation;
    private List<PreviewModel> previewModels;
    private int visiblePreviewIndex = 0;

    PreviewPresenter() {
    }

    void setDisplayRotation(int displayRotation) {
        this.displayRotation = displayRotation;
    }

    @Override
    public void attachView(PreviewView view) {
        super.attachView(view);
        load();
    }

    private void load() {
        addSubscription(Observable.fromCallable(() -> {
            List<PreviewModel> previewModels = new ArrayList<>();
            for (CameraResult result : cameraResultList) {
                if (result.getAction()
                            .getKind() == CameraActionKind.BARCODE) {
                    Bitmap previewBitmap = ImageUtils.createBarcodeBitmap(result.getBarcode(), getDisplayWidth());
                    previewModels.add(new PreviewModel(result, previewBitmap, System.currentTimeMillis()));
                } else {
                    Bitmap previewBitmap = ImageUtils.base64ToBitmap(result.getPhotoBase64());
                    previewBitmap = fixPreviewMaybe(previewBitmap);
                    previewModels.add(new PreviewModel(result, previewBitmap, System.currentTimeMillis()));
                }
            }
            this.previewModels = previewModels;
            return previewModels;
        })
                                .subscribe(previewModels -> getViewState().showPreview(previewModels), throwable -> {
                                    Timber.e("", throwable);
                                    getViewState().onPhotoLoadError();
                                }));
    }

    private Bitmap fixPreviewMaybe(Bitmap preview) {
        if (android.os.Build.MANUFACTURER.contains("samsung") && preview.getWidth() > preview.getHeight()) {
            preview = fixSamsungPreviewOrientation(displayRotation, preview);
        }
        return preview;
    }

    private int getDisplayWidth() {
        if (android.os.Build.MANUFACTURER.contains("samsung")) {
            return 720;
        } else {
            return 1080;
        }
    }

    private Bitmap fixSamsungPreviewOrientation(int displayRotation, Bitmap preview) {
        int fixedRotation = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                fixedRotation = 90;
                break;
            case Surface.ROTATION_90:
                fixedRotation = 0;
                break;
            case Surface.ROTATION_180:
                fixedRotation = 270;
                break;
            case Surface.ROTATION_270:
                fixedRotation = 180;
                break;
        }
        return ImageUtils.rotateBitmap(preview, fixedRotation);
    }

    public ArrayList<CameraResult> getCameraResultList() {
        return cameraResultList;
    }

    void setCameraResultList(ArrayList<CameraResult> cameraResultList) {
        this.cameraResultList = cameraResultList;
    }

    boolean hasNextAction() {
        return hasNextAction;
    }

    void setHasNextAction(boolean hasNextAction) {
        this.hasNextAction = hasNextAction;
    }

    int getVisiblePreviewIndex() {
        return visiblePreviewIndex;
    }

    void setVisiblePreviewIndex(int index) {
        visiblePreviewIndex = index;
    }

    List<PreviewModel> getPreviewModels() {
        return previewModels;
    }

    PreviewModel getVisiblePreviewModel() {
        return previewModels.get(visiblePreviewIndex);
    }
}
