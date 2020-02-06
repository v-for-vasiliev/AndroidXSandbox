package ru.vasiliev.sandbox.camera2.presentation.preview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

import ru.vasiliev.sandbox.R;
import ru.vasiliev.sandbox.camera2.data.action.CameraActionKind;
import ru.vasiliev.sandbox.camera2.data.result.CameraResult;
import ru.vasiliev.sandbox.camera2.presentation.preview.viewmodel.PreviewModel;

public class PreviewPagerAdapter extends PagerAdapter {

    private Context context;
    private List<PreviewModel> previewModels = new ArrayList<>();

    PreviewPagerAdapter(Context context, List<PreviewModel> previewModels) {
        this.context = context;
        this.previewModels.addAll(previewModels);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        PreviewModel pm = previewModels.get(position);
        CameraResult result = pm.getCameraResult();
        if (result.getAction()
                    .getKind() == CameraActionKind.BARCODE) {
            View previewContainer = LayoutInflater.from(context)
                    .inflate(R.layout.layout_camera_scan_preview_page, null);
            TextView barcode = previewContainer.findViewById(R.id.barcode_value);
            barcode.setText(result.getBarcode());
            ImageView preview = previewContainer.findViewById(R.id.barcode_preview);
            Glide.with(context)
                    .load(pm.getPreviewBitmap())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                                   .signature(new ObjectKey(pm.getPreviewBitmapCacheId())))
                    .into(preview);
            container.addView(previewContainer);
            return previewContainer;
        } else {
            PhotoView preview = (PhotoView) LayoutInflater.from(context)
                    .inflate(R.layout.layout_camera_capture_preview_page, null);
            Glide.with(context)
                    .load(pm.getPreviewBitmap())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                                   .signature(new ObjectKey(pm.getPreviewBitmapCacheId())))
                    .into(preview);
            container.addView(preview);
            return preview;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object view) {
        container.removeView((View) view);
    }

    @Override
    public int getCount() {
        return previewModels.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return object == view;
    }
}