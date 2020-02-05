package ru.vasiliev.sandbox.di.components;

import dagger.Subcomponent;
import ru.vasiliev.sandbox.camera2.presentation.camera.CameraActivity;
import ru.vasiliev.sandbox.di.modules.CameraModule;
import ru.vasiliev.sandbox.di.scopes.ActivityScope;

@Subcomponent(modules = {CameraModule.class})
@ActivityScope
public interface CameraComponent {

    void inject(CameraActivity cameraActivity);

}