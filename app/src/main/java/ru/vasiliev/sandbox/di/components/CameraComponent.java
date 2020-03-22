package ru.vasiliev.sandbox.di.components;

import dagger.Subcomponent;
import ru.vasiliev.sandbox.camera.presentation.camera.CameraActivity;
import ru.vasiliev.sandbox.camera.presentation.newcameratest.NewCameraTestActivity;
import ru.vasiliev.sandbox.di.modules.CameraModule;
import ru.vasiliev.sandbox.di.scopes.ActivityScope;

@Subcomponent(modules = {CameraModule.class})
@ActivityScope
public interface CameraComponent {

    void inject(CameraActivity cameraActivity);

    void inject(NewCameraTestActivity newCameraTestActivity);
}