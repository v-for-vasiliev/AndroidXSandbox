package ru.vasiliev.sandbox.camera2.presentation.preview;

import java.util.List;

import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.StateStrategyType;
import ru.vasiliev.sandbox.camera2.presentation.preview.viewmodel.PreviewModel;

@StateStrategyType(AddToEndSingleStrategy.class)
interface PreviewView extends MvpView {

    void showPreview(List<PreviewModel> previewModels);

    void onPhotoLoadError();
}