package ru.vasiliev.sandbox.camera2.presentation;

public interface CameraActionListener {

    void onActionCompleted(String completedActionHashKey);

    void onNextAction();

    void onActionDiscarded(String actionHashKey);

    void onNavigateBack();
}