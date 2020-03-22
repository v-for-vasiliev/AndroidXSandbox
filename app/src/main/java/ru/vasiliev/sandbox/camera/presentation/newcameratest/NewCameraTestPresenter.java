package ru.vasiliev.sandbox.camera.presentation.newcameratest;

import com.bumptech.glide.Glide;

import javax.inject.Inject;

import moxy.InjectViewState;
import ru.vasiliev.sandbox.App;
import ru.vasiliev.sandbox.camera.data.result.CameraResultProvider;
import ru.vasiliev.sandbox.common.presentation.BaseMoxyPresenter;

@InjectViewState
public class NewCameraTestPresenter extends BaseMoxyPresenter<NewCameraTestView> {

    private CameraResultProvider cameraResultProvider = CameraResultProvider.getInstance();

    @Inject
    NewCameraTestPresenter() {
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        CameraResultProvider.getInstance().clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.get(App.Companion.getInstance()).clearMemory();
    }
}
