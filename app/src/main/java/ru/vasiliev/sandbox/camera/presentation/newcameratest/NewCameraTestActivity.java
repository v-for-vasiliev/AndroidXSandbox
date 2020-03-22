package ru.vasiliev.sandbox.camera.presentation.newcameratest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import javax.inject.Inject;

import moxy.MvpAppCompatActivity;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import ru.vasiliev.sandbox.App;
import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera.device.camera.camera2.Camera2Controller;
import ru.vasiliev.sandbox.camera.device.camera.common.CameraController;
import ru.vasiliev.sandbox.camera.device.camera.common.CameraPreview;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing;
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash;
import ru.vasiliev.sandbox.di.components.CameraComponent;

public class NewCameraTestActivity extends MvpAppCompatActivity implements NewCameraTestView {

    private CameraController cameraController;
    private CameraPreview cameraPreview;

    @Inject
    @InjectPresenter
    NewCameraTestPresenter presenter;

    public static void start(@NonNull Context context) {
        Intent startIntent = new Intent(context, NewCameraTestActivity.class);
        context.startActivity(startIntent);
    }

    @ProvidePresenter
    NewCameraTestPresenter providePresenter() {
        return presenter;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        CameraComponent component = App.Companion.getAppComponent().getCameraComponent();
        component.inject(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        initCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        cameraController.start();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                                                                   PermissionToken token) {

                    }
                }).check();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraController.stop();
    }

    private void initCamera() {
        cameraPreview = findViewById(R.id.camera_preview);
        cameraController = new Camera2Controller(this, cameraPreview);
        cameraController.setFacing(CameraFacing.BACK);
        cameraController.setAutoFocus(true);
        cameraController.setFlash(CameraFlash.FLASH_AUTO);
        cameraController.setAutoWhiteBalance(true);
    }
}
