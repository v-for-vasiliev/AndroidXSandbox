package ru.vasiliev.sandbox.camera2.presentation.action;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import moxy.MvpAppCompatFragment;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import ru.vasiliev.sandbox.BuildConfig;
import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera2.data.action.CameraAction;
import ru.vasiliev.sandbox.camera2.data.action.CameraActionKind;
import ru.vasiliev.sandbox.camera2.device.camera2.Camera2Api;
import ru.vasiliev.sandbox.camera2.device.camera2.Camera2ApiListener;
import ru.vasiliev.sandbox.camera2.device.camera2.Camera2Mode;
import ru.vasiliev.sandbox.camera2.device.scanner.BarcodeScanner;
import ru.vasiliev.sandbox.camera2.device.scanner.GMSBarcodeScanner;
import ru.vasiliev.sandbox.camera2.presentation.CameraActionListener;
import ru.vasiliev.sandbox.camera2.presentation.view.AutoFitTextureView;
import ru.vasiliev.sandbox.camera2.presentation.view.FocusSurfaceView;
import ru.vasiliev.sandbox.camera2.presentation.view.ScannerOverlayView;

import static ru.vasiliev.sandbox.camera2.device.camera2.Camera2Api.FLASH_AUTO;
import static ru.vasiliev.sandbox.camera2.device.camera2.Camera2Api.FLASH_TURN_OFF;
import static ru.vasiliev.sandbox.camera2.device.camera2.Camera2Api.FLASH_TURN_ON;


public class CameraActionFragment extends MvpAppCompatFragment implements CameraActionView, Camera2ApiListener {

    public static final String TAG = CameraActionFragment.class.getSimpleName();

    public static final String EXTRA_KEY_CAMERA_ACTION = "extra_key_camera_action";

    @BindView(R.id.previewLayer)
    AutoFitTextureView previewLayer;
    @BindView(R.id.focusLayer)
    FocusSurfaceView focusLayer;
    @BindView(R.id.scannerOverlay)
    ScannerOverlayView scannerOverlay;
    @BindView(R.id.description)
    TextView descriptionText;
    @BindView(R.id.barcodeErrorPopUp)
    TextView barcodeErrorPopUp;
    @BindView(R.id.barcodeLayout)
    LinearLayout barcodeLayout;
    @BindView(R.id.barcodeText)
    TextView barcodeText;
    @BindView(R.id.barcodeDiscardButton)
    ImageView barcodeDiscardButton;
    @BindView(R.id.controlPanel)
    RelativeLayout controlPanel;
    @BindView(R.id.backButton)
    ImageView backButton;
    @BindView(R.id.captureButton)
    ImageView captureButton;
    @BindView(R.id.captureProgress)
    ProgressBar captureProgress;
    @BindView(R.id.flashButton)
    ImageView flashButton;
    @InjectPresenter
    CameraActionPresenter presenter;
    private CameraActionListener cameraActionListener;
    private Camera2Api camera2Api;
    private BarcodeScanner barcodeScanner;
    private RxPermissions rxPermissions;
    private CompositeDisposable rxSubscriptions = new CompositeDisposable();
    private boolean cameraPermissionGranted = false;

    public static CameraActionFragment newInstance(CameraAction cameraAction) {
        CameraActionFragment fragment = new CameraActionFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_KEY_CAMERA_ACTION, cameraAction);
        fragment.setArguments(args);
        return fragment;
    }

    @ProvidePresenter
    CameraActionPresenter providePresenter() {
        return new CameraActionPresenter(FLASH_TURN_OFF);
    }

    public CameraActionFragment setAction(CameraAction cameraAction) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_KEY_CAMERA_ACTION, cameraAction);
        setArguments(args);
        return this;
    }

    @OnClick({R.id.backButton, R.id.captureButton, R.id.flashButton, R.id.barcodeDiscardButton})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.backButton:
                back();
                break;
            case R.id.captureButton:
                capturePhoto();
                break;
            case R.id.flashButton:
                toggleFlash();
                break;
            case R.id.barcodeDiscardButton:
                discardBarcode();
                break;
            default:
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_action, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        extractArguments(getArguments());
        rxPermissions = new RxPermissions(this);
        rxSubscriptions.add(rxPermissions.request(Manifest.permission.CAMERA)
                                    .subscribe(granted -> {
                                        cameraPermissionGranted = granted;
                                        if (granted) {
                                            initCamera2Api();
                                            initView();
                                        } else {
                                            showErrorAndCloseCamera("Приложению нужен доступ к камере");
                                        }
                                    }));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraPermissionGranted) {
            camera2Api.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraPermissionGranted) {
            camera2Api.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rxSubscriptions.dispose();
    }

    @Override
    public void onCameraError(String message) {
        showErrorAndCloseCamera(message);
    }

    @Override
    public void onBarcodeFound(String barcode) {
        presenter.handleResult(barcode);
    }

    @Override
    public void onBarcodeLost() {
        barcodeErrorPopUp.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showProgress() {
        backButton.setVisibility(View.INVISIBLE);
        flashButton.setVisibility(View.INVISIBLE);
        captureButton.setVisibility(View.INVISIBLE);
        captureProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void showCaptureControls() {
        backButton.setVisibility(View.VISIBLE);
        flashButton.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
        captureProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void hideControlPanel() {
        controlPanel.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showControlPanel() {
        controlPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void showBarcode(String barcode) {
        // Hide warn if visible
        barcodeErrorPopUp.setVisibility(View.INVISIBLE);
        // Show barcode layout
        barcodeLayout.setVisibility(View.VISIBLE);
        barcodeText.setText(barcode);
        barcodeDiscardButton.setVisibility(View.VISIBLE); // Uncomment if you want user to able discard barcode
        // after it was found.
    }

    @Override
    public void showBarcodeFormatError() {
        barcodeLayout.setVisibility(View.INVISIBLE);
        barcodeErrorPopUp.setText(
                "Неизвестный формат штрих-кода документа. Убедитесь в том, что Вы сканируете правильный документ.");
        barcodeErrorPopUp.setVisibility(View.VISIBLE);
        camera2Api.discardBarcode();
    }

    @Override
    public void showBarcodeAlreadyScannedError(String documentDescription) {
        barcodeLayout.setVisibility(View.INVISIBLE);
        barcodeErrorPopUp.setText(String.format(Locale.getDefault(),
                                                "Штрих-код документа уже был отсканирован ранее (%s).",
                                                documentDescription));
        barcodeErrorPopUp.setVisibility(View.VISIBLE);
        camera2Api.discardBarcode();
    }

    @Override
    public void showBarcodeNotFoundError() {
        showError(
                "Не удалось распознать штрих-код документа. Проверьте документ или сделайте фотографию лучшего качества.");
    }

    private void discardBarcode() {
        camera2Api.discardBarcode();
        barcodeLayout.setVisibility(View.INVISIBLE);
        barcodeText.setText("");
    }

    @Override
    public void onActionCompleted() {
        cameraActionListener.onActionCompleted(presenter.getCameraAction()
                                                       .getHashKey());
    }

    @Override
    public void showError(String error) {
        new AlertDialog.Builder(getActivity()).setTitle("Ошибка")
                .setMessage(error)
                .setCancelable(false)
                .setIcon(R.drawable.ic_camera_red_24dp)
                .setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @Override
    public void showErrorAndCloseCamera(String error) {
        new AlertDialog.Builder(getActivity()).setTitle("Ошибка")
                .setMessage(error)
                .setCancelable(false)
                .setIcon(R.drawable.ic_camera_red_24dp)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    getActivity().setResult(Activity.RESULT_CANCELED);
                    getActivity().finish();
                })
                .create()
                .show();
    }

    private void extractArguments(Bundle args) {
        presenter.setCameraAction(args.getParcelable(EXTRA_KEY_CAMERA_ACTION));
    }

    private void initCamera2Api() {
        Camera2Mode camera2Mode;
        switch (presenter.getCameraAction()
                .getKind()) {
            case PHOTO_AND_BARCODE:
                camera2Mode = Camera2Mode.CAPTURE_AND_SCAN;
                break;
            case PHOTO:
                camera2Mode = Camera2Mode.CAPTURE;
                break;
            case BARCODE:
                camera2Mode = Camera2Mode.SCAN;
                break;
            default:
                throw new RuntimeException("INVALID_CAMERA_ACTION");
        }
        //barcodeScanner = new ZBarBarcodeScanner();
        barcodeScanner = new GMSBarcodeScanner(getActivity());
        camera2Api = new Camera2Api(getActivity(), previewLayer, focusLayer, barcodeScanner, camera2Mode, this);
    }

    private void initView() {
        cameraActionListener = (CameraActionListener) getActivity();

        showControlPanel();
        showCaptureControls();

        if (presenter.getCameraAction()
                    .getKind() == CameraActionKind.BARCODE) {
            captureButton.setVisibility(View.INVISIBLE);
            scannerOverlay.setVisibility(View.VISIBLE);
        }

        int flashStatus = presenter.getFlashStatus();
        camera2Api.setFlashStatus(flashStatus);
        if (flashStatus == FLASH_AUTO || flashStatus == FLASH_TURN_OFF) {
            flashButton.setImageResource(R.drawable.ic_flash_off_black_24px);
        } else {
            flashButton.setImageResource(R.drawable.ic_flash_on_black_24px);
        }

        if (presenter.getCameraAction()
                    .getKind() == CameraActionKind.PHOTO_AND_BARCODE) {
            if (presenter.getCameraAction()
                    .isMultiPageDocument()) {
                descriptionText.setText(String.format(Locale.getDefault(),
                                                      "%s, стр. %d (фото и штрих-код)",
                                                      presenter.getCameraAction()
                                                              .getDescription(),
                                                      presenter.getCameraAction()
                                                              .getIndex()));
            } else {
                descriptionText.setText(presenter.getCameraAction()
                                                .getDescription());
            }
        } else {
            if (presenter.getCameraAction()
                    .isMultiPageDocument()) {
                descriptionText.setText(String.format(Locale.getDefault(),
                                                      "%s, стр. %d",
                                                      presenter.getCameraAction()
                                                              .getDescription(),
                                                      presenter.getCameraAction()
                                                              .getIndex()));
            } else {
                descriptionText.setText(presenter.getCameraAction()
                                                .getDescription());
            }
        }
    }

    private void capturePhoto() {
        barcodeDiscardButton.setVisibility(View.GONE);
        presenter.handleResult(camera2Api.capture(presenter.getCameraAction()
                                                          .getCaptureQuality(),
                                                  BuildConfig.CAMERA2_AUTO_LOCK_FOCUS_BEFORE_CAPTURE));
    }

    private void back() {
        cameraActionListener.onNavigateBack();
    }

    private void toggleFlash() {
        if (!cameraPermissionGranted) {
            return;
        }
        int flashStatus = presenter.getFlashStatus();
        if (flashStatus == FLASH_AUTO || flashStatus == FLASH_TURN_OFF) {
            flashStatus = FLASH_TURN_ON;
            flashButton.setImageResource(R.drawable.ic_flash_on_black_24px);
        } else {
            flashStatus = FLASH_TURN_OFF;
            flashButton.setImageResource(R.drawable.ic_flash_off_black_24px);
        }

        presenter.setFlashStatus(flashStatus);
        camera2Api.updateFlashStatus(flashStatus);
    }
}
