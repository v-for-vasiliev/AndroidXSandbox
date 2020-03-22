package ru.vasiliev.sandbox.camera.presentation;

public interface CameraActionListener {

    void onActionCompleted(String completedActionHashKey);

    void onNextAction();

    void onActionDiscarded(String actionHashKey);

    void onNavigateBack();
}