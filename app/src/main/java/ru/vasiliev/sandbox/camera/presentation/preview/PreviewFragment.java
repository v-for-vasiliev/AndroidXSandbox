package ru.vasiliev.sandbox.camera.presentation.preview;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import moxy.MvpAppCompatFragment;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;
import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera.data.action.CameraActionKind;
import ru.vasiliev.sandbox.camera.data.result.CameraResult;
import ru.vasiliev.sandbox.legacycamera.camera2.Camera2Metadata;
import ru.vasiliev.sandbox.camera.presentation.CameraActionListener;
import ru.vasiliev.sandbox.camera.presentation.preview.viewmodel.PreviewModel;
import ru.vasiliev.sandbox.camera.presentation.view.PhotoViewPager;

public class PreviewFragment extends MvpAppCompatFragment implements PreviewView {

    public static final String TAG = PreviewFragment.class.getSimpleName();

    private static final String EXTRA_KEY_RESULT_LIST = "extra_key_result_list";
    private static final String EXTRA_KEY_HAS_NEXT_ACTION = "extra_key_next_action";
    private static final String EXTRA_KEY_VISIBLE_PREVIEW_INDEX = "extra_key_visible_preview_index";

    @BindView(R.id.previewPager)
    PhotoViewPager previewPager;
    @BindView(R.id.previewPagerDots)
    TabLayout previewDots;
    @BindView(R.id.description)
    TextView descriptionText;
    @BindView(R.id.infoButton)
    ImageView infoButton;
    @BindView(R.id.barcodeText)
    TextView barcodeText;
    @BindView(R.id.controlPanel)
    RelativeLayout controlPanel;
    @BindView(R.id.recaptureButton)
    TextView recaptureButton;
    @BindView(R.id.acceptPhotoButton)
    TextView acceptPhotoButton;

    // Info view
    @BindView(R.id.photoInfoLayout)
    LinearLayout photoInfoLayout;
    @BindView(R.id.photoInfoTime)
    TextView photoInfoTime;
    @BindView(R.id.photoInfoQuality)
    TextView photoInfoQuality;
    @BindView(R.id.photoInfoSize)
    TextView photoInfoSize;
    @BindView(R.id.photoInfoFocus)
    TextView photoInfoFocus;
    @BindView(R.id.photoInfoCloseButton)
    TextView photoInfoCloseButton;

    @InjectPresenter
    PreviewPresenter presenter;
    private CameraActionListener cameraActionListener;

    private static String fmt(String template, Object... args) {
        return String.format(Locale.getDefault(), template, args);
    }

    @ProvidePresenter
    PreviewPresenter providePresenter() {
        return new PreviewPresenter();
    }

    public PreviewFragment setResult(ArrayList<CameraResult> cameraResults, boolean hasNextAction,
                                     int visiblePreviewIndex) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(EXTRA_KEY_RESULT_LIST, cameraResults);
        args.putBoolean(EXTRA_KEY_HAS_NEXT_ACTION, hasNextAction);
        args.putInt(EXTRA_KEY_VISIBLE_PREVIEW_INDEX, visiblePreviewIndex);
        setArguments(args);
        return this;
    }

    @OnClick({R.id.recaptureButton, R.id.acceptPhotoButton})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.recaptureButton:
                onRecaptureButtonClicked();
                break;
            case R.id.acceptPhotoButton:
                onAcceptButtonClicked();
                break;
            default:
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_preview_page, container, false);
        ButterKnife.bind(this, view);
        extractArguments(getArguments());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraActionListener = (CameraActionListener) getActivity();
        initView();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        cameraActionListener = (CameraActionListener) getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cameraActionListener = null;
    }

    @Override
    public void showPreview(List<PreviewModel> previewModels) {
        int visiblePreviewIndex = presenter.getVisiblePreviewIndex();
        setupVisiblePreviewInfo();
        setupPreviewPager(previewModels, visiblePreviewIndex);
        if (presenter.hasNextAction()) {
            acceptPhotoButton.setText("ДАЛЬШЕ");
        } else {
            acceptPhotoButton.setText("ГОТОВО");
        }
        controlPanel.setVisibility(View.VISIBLE);
    }

    private void extractArguments(Bundle args) {
        presenter.setCameraResultList(args.getParcelableArrayList(EXTRA_KEY_RESULT_LIST));
        presenter.setHasNextAction(args.getBoolean(EXTRA_KEY_HAS_NEXT_ACTION, false));
        presenter.setVisiblePreviewIndex(args.getInt(EXTRA_KEY_VISIBLE_PREVIEW_INDEX, 0));
    }

    private void initView() {
        presenter.setDisplayRotation(getActivity().getWindowManager()
                                             .getDefaultDisplay()
                                             .getRotation());
    }

    private void setupPreviewPager(List<PreviewModel> previewModels, int visiblePreviewIndex) {
        PreviewPagerAdapter previewPagerAdapter = new PreviewPagerAdapter(getActivity(), previewModels);
        previewPager.setAdapter(previewPagerAdapter);
        if (previewModels.size() > 1) {
            previewDots.setupWithViewPager(previewPager, true);
        } else {
            previewDots.setVisibility(View.INVISIBLE);
        }
        previewPager.setCurrentItem(visiblePreviewIndex);
        previewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
            }

            @Override
            public void onPageSelected(int position) {
                presenter.setVisiblePreviewIndex(position);
                setupVisiblePreviewInfo();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void setupVisiblePreviewInfo() {
        PreviewModel previewModel = presenter.getVisiblePreviewModel();
        CameraResult result = previewModel.getCameraResult();
        if (result.getAction()
                    .getKind() != CameraActionKind.BARCODE) {
            if (result.hasBarcode()) {
                barcodeText.setText(result.getBarcode());
                barcodeText.setVisibility(View.VISIBLE);
            } else {
                barcodeText.setVisibility(View.INVISIBLE);
            }
        } else {
            barcodeText.setVisibility(View.INVISIBLE);
        }

        if (result.getAction()
                .isMultiPageDocument()) {
            descriptionText.setText(String.format(Locale.getDefault(),
                                                  "%s, стр. %d",
                                                  result.getAction()
                                                          .getDescription(),
                                                  result.getAction()
                                                          .getIndex()));
        } else {
            descriptionText.setText(result.getAction()
                                            .getDescription());
        }

        setupPreviewInfo(previewModel);
    }

    private void setupPreviewInfo(PreviewModel previewModel) {
        CameraResult result = previewModel.getCameraResult();
        Camera2Metadata metadata = result.getMetadata();
        if (result.getAction()
                    .getKind() == CameraActionKind.BARCODE) {
            infoButton.setVisibility(View.INVISIBLE);
            photoInfoLayout.setVisibility(View.INVISIBLE);
            return;
        } else {
            infoButton.setVisibility(View.VISIBLE);
        }
        int base64TransferSizeKb = result.getPhotoBase64()
                                           .getBytes(StandardCharsets.UTF_8).length / 1024;
        infoButton.setOnClickListener(v -> {
            if (photoInfoLayout.getVisibility() != View.VISIBLE) {
                photoInfoLayout.setVisibility(View.VISIBLE);
            } else {
                photoInfoLayout.setVisibility(View.INVISIBLE);
            }
        });
        photoInfoCloseButton.setOnClickListener(v -> photoInfoLayout.setVisibility(View.INVISIBLE));
        if (metadata != null) {
            photoInfoTime.setText(fmt("Время создания: %s",
                                      new DateTime(metadata.getTimestamp()).toString(DateTimeFormat.forPattern(
                                              "dd.MM.yyyy HH:mm:ss"))));
            photoInfoQuality.setText(fmt("Качество: %d%%", metadata.getQuality()));
            photoInfoSize.setText(fmt("Размер: %dKb", base64TransferSizeKb));
            switch (metadata.getFocusMode()) {
                case FOCUS_MODE_OFF:
                    photoInfoFocus.setText("Фокусировка: нет");
                    break;
                case FOCUS_MODE_AUTO:
                    photoInfoFocus.setText("Фокусировка: авто");
                    break;
                case FOCUS_MODE_MANUAL:
                    photoInfoFocus.setText("Фокусировка: ручная");
                    break;
            }
        }
    }

    @Override
    public void onPhotoLoadError() {
        new AlertDialog.Builder(getActivity()).setCancelable(false)
                .setTitle("Камера")
                .setMessage("Ошибка загрузки фотографии")
                .setIcon(R.drawable.ic_camera_red_24dp)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    getActivity().finish();
                })
                .create()
                .show();
    }

    protected void onRecaptureButtonClicked() {
        cameraActionListener.onActionDiscarded(presenter.getVisiblePreviewModel()
                                                       .getCameraResult()
                                                       .getAction()
                                                       .getHashKey());
    }

    protected void onAcceptButtonClicked() {
        cameraActionListener.onNextAction();
    }
}
