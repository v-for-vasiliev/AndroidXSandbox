package ru.vasiliev.sandbox.camera.presentation.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

import javax.inject.Inject;

import moxy.MvpAppCompatActivity;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import ru.vasiliev.sandbox.App;
import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera.data.action.CameraAction;
import ru.vasiliev.sandbox.camera.data.result.CameraResult;
import ru.vasiliev.sandbox.camera.presentation.CameraActionListener;
import ru.vasiliev.sandbox.camera.presentation.action.CameraActionFragment;
import ru.vasiliev.sandbox.camera.presentation.preview.PreviewFragment;
import ru.vasiliev.sandbox.di.components.CameraComponent;

public class CameraActivity extends MvpAppCompatActivity implements CameraView, CameraActionListener {

    public static final int REQUEST_CODE_MIXED_DOCUMENTS = 4201;
    public static final int REQUEST_CODE_PHOTO = 4202;
    public static final int REQUEST_CODE_BARCODE = 4203;
    private static final String EXTRA_KEY_ACTIONS = "extra_key_actions";
    @Inject
    @InjectPresenter
    CameraPresenter presenter;
    private CameraActionFragment cameraActionFragment = new CameraActionFragment();
    private PreviewFragment previewFragment = new PreviewFragment();

    public static void start(Activity activity, ArrayList<CameraAction> actions, RequestKind requestKind) {
        if (actions == null || actions.size() == 0) {
            return;
        }
        Intent startIntent = new Intent(activity, CameraActivity.class);
        startIntent.putParcelableArrayListExtra(EXTRA_KEY_ACTIONS, actions);
        activity.startActivityForResult(startIntent, requestKind.getRequestCode());
    }

    public static void start(Fragment fragment, ArrayList<CameraAction> actions, RequestKind requestKind) {
        if (actions == null || actions.size() == 0) {
            return;
        }
        Intent startIntent = new Intent(fragment.getActivity(), CameraActivity.class);
        startIntent.putParcelableArrayListExtra(EXTRA_KEY_ACTIONS, actions);
        fragment.startActivityForResult(startIntent, requestKind.getRequestCode());
    }

    public static void start(android.app.Fragment fragment, ArrayList<CameraAction> actions, RequestKind requestKind) {
        if (actions == null || actions.size() == 0) {
            return;
        }
        Intent startIntent = new Intent(fragment.getActivity(), CameraActivity.class);
        startIntent.putParcelableArrayListExtra(EXTRA_KEY_ACTIONS, actions);
        fragment.startActivityForResult(startIntent, requestKind.getRequestCode());
    }

    @ProvidePresenter
    CameraPresenter providePresenter() {
        return presenter;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        CameraComponent component = App.Companion.getAppComponent()
                .getCameraComponent();
        component.inject(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        extractArguments(getIntent().getExtras());
        setResult(Activity.RESULT_CANCELED);
    }

    public void extractArguments(Bundle extras) {
        presenter.setActions(extras.getParcelableArrayList(EXTRA_KEY_ACTIONS));
    }

    @Override
    public void setupActionFragment(CameraAction cameraAction) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, cameraActionFragment.setAction(cameraAction));
        transaction.commitAllowingStateLoss();
        presenter.setShowingPreview(false);
    }

    @Override
    public void setupPreviewFragment(ArrayList<CameraResult> cameraResults, boolean hasNextAction,
                                     int visiblePreviewIndex) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container,
                            previewFragment.setResult(cameraResults, hasNextAction, visiblePreviewIndex));
        transaction.commitAllowingStateLoss();
        presenter.setShowingPreview(true);
    }

    @Override
    public void onCaptureError(String error) {
        new AlertDialog.Builder(this).setTitle("Камера")
                .setMessage(error)
                .setIcon(R.drawable.ic_camera_black_24dp)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    presenter.recapturePhoto();
                })
                .create()
                .show();
    }

    @Override
    public void onNavigateBack() {
        presenter.onNavigateBack();
    }

    @Override
    public void onCaptureComplete() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onCaptureCancelled() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void onActionCompleted(String completedActionHashKey) {
        presenter.onActionCompleted(completedActionHashKey);
    }

    // CameraActionListener callbacks

    @Override
    public void onNextAction() {
        presenter.onNextAction();
    }

    @Override
    public void onActionDiscarded(String actionHashKey) {
        presenter.onActionDiscarded(actionHashKey);
    }

    @Override
    public void onBackPressed() {
        presenter.onNavigateBack();
    }

    public enum RequestKind {

        REQUEST_KIND_MIXED(REQUEST_CODE_MIXED_DOCUMENTS), REQUEST_KIND_PHOTO(REQUEST_CODE_PHOTO), REQUEST_KIND_BARCODE(
                REQUEST_CODE_BARCODE);

        private int requestCode;

        RequestKind(int requestCode) {
            this.requestCode = requestCode;
        }

        public int getRequestCode() {
            return requestCode;
        }
    }
}
