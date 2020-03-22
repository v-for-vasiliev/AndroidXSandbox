package ru.vasiliev.sandbox.camera.presentation.newcameratest;

import moxy.MvpView;
import moxy.viewstate.strategy.AddToEndSingleStrategy;
import moxy.viewstate.strategy.StateStrategyType;

@StateStrategyType(AddToEndSingleStrategy.class)
interface NewCameraTestView extends MvpView {

}