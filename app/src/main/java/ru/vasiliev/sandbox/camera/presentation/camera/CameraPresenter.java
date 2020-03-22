package ru.vasiliev.sandbox.camera.presentation.camera;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import moxy.InjectViewState;
import ru.vasiliev.sandbox.App;
import ru.vasiliev.sandbox.camera.data.action.CameraAction;
import ru.vasiliev.sandbox.camera.data.result.CameraResult;
import ru.vasiliev.sandbox.camera.data.result.CameraResultProvider;
import ru.vasiliev.sandbox.common.presentation.BaseMoxyPresenter;

@InjectViewState
public class CameraPresenter extends BaseMoxyPresenter<CameraView> {

    private CameraResultProvider cameraResultProvider = CameraResultProvider.getInstance();
    private List<CameraAction> actions = new ArrayList<>();
    private boolean isShowingPreview = false;

    @Inject
    CameraPresenter() {
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        CameraResultProvider.getInstance()
                .clear();
        getViewState().setupActionFragment(getNextAction());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.get(App.Companion.getInstance())
                .clearMemory();
    }

    void setActions(List<CameraAction> actions) {
        this.actions.clear();
        this.actions.addAll(actions);
    }

    private boolean hasNext() {
        for (CameraAction action : actions) {
            if (!cameraResultProvider.contains(action.getHashKey())) {
                return true;
            }
        }
        return false;
    }

    private CameraAction getNextAction() {
        for (CameraAction action : actions) {
            if (!cameraResultProvider.contains(action.getHashKey())) {
                return action;
            }
        }
        return null;
    }

    void recapturePhoto() {
        getViewState().setupActionFragment(getNextAction());
    }

    private void recapturePhoto(String actionHashKey) {
        if (actionHashKey == null || ("").equals(actionHashKey)) {
            return;
        }
        for (CameraAction action : actions) {
            if (actionHashKey.equals(action.getHashKey())) {
                getViewState().setupActionFragment(action);
            }
        }
    }

    void onNavigateBack() {
        if (isShowingPreview()) {
            getViewState().onCaptureCancelled();
        } else {
            ArrayList<CameraResult> completedActionResultList = (ArrayList<CameraResult>) cameraResultProvider.getAll();
            if (completedActionResultList.size() > 0) {
                getViewState().setupPreviewFragment(completedActionResultList,
                                                    hasNext(),
                                                    completedActionResultList.size() - 1);
            } else {
                getViewState().onCaptureCancelled();
            }
        }
    }

    void onActionCompleted(String completedActionHashKey) {
        ArrayList<CameraResult> completedActionResultList = (ArrayList<CameraResult>) cameraResultProvider.getAll();
        if (completedActionResultList.size() > 0) {
            getViewState().setupPreviewFragment(completedActionResultList,
                                                hasNext(),
                                                getVisiblePreviewIndex(completedActionResultList,
                                                                       completedActionHashKey));
        } else {
            getViewState().onCaptureError("Ошибка обработки, пожалуйста переделайте фото");
        }
    }

    void onNextAction() {
        if (hasNext()) {
            getViewState().setupActionFragment(getNextAction());
        } else {
            getViewState().onCaptureComplete();
        }
    }

    private int getVisiblePreviewIndex(ArrayList<CameraResult> completedActions, String completedActionHashKey) {
        if (completedActions.size() > 0) {
            for (int index = 0; index < completedActions.size(); index++) {
                if (completedActions.get(index)
                        .getAction()
                        .getHashKey()
                        .equals(completedActionHashKey)) {
                    return index;
                }
            }
        }
        return 0;
    }

    void onActionDiscarded(String actionHashKey) {
        cameraResultProvider.discard(actionHashKey);
        recapturePhoto(actionHashKey);
    }

    public boolean isShowingPreview() {
        return isShowingPreview;
    }

    public void setShowingPreview(boolean showingPreview) {
        isShowingPreview = showingPreview;
    }
}
