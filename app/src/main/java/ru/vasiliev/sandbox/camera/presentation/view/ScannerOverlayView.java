package ru.vasiliev.sandbox.camera.presentation.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import ru.vasiliev.sandbox.R;

@SuppressWarnings("ALL")
public class ScannerOverlayView extends LinearLayout {

    private static final int SCAN_RECT_HEIGHT = 400;
    private static final int SCAN_RECT_WIDTH = 600;
    private static final int SCAN_RECT_CORNER_WIDTH = 70;
    private static final int BACKGROUND_COLOR = R.color.black_40;
    private static final int SCAN_CORNERS_COLOR = R.color.white;

    private Bitmap scannerViewBackgroundBitmap;

    public ScannerOverlayView(Context context) {
        super(context);
    }

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScannerOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScannerOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (scannerViewBackgroundBitmap == null) {
            scannerViewBackgroundBitmap = createScannerViewBackground();
        }
        canvas.drawBitmap(scannerViewBackgroundBitmap, 0, 0, null);
    }

    protected Bitmap createScannerViewBackground() {
        Bitmap backgroundBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(backgroundBitmap);

        drawBackground(canvas, BACKGROUND_COLOR);
        drawScannerRectangle(canvas, SCAN_RECT_WIDTH, SCAN_RECT_HEIGHT, SCAN_RECT_CORNER_WIDTH, SCAN_CORNERS_COLOR);

        return backgroundBitmap;
    }

    private void drawBackground(Canvas canvas, @ColorRes int color) {
        RectF outerRectangle = new RectF(0, 0, getWidth(), getHeight());
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(getContext(), color));
        //paint.setAlpha(99);
        canvas.drawRect(outerRectangle, paint);
    }

    private void drawScannerRectangle(Canvas canvas, int scannerRectWidth, int scannerRectHeight,
                                      int scannerCornersWidth, @ColorRes int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;
        RectF scanRect = new RectF(centerX - (scannerRectWidth / 2.0f),
                                   centerY - (scannerRectHeight / 2.0f),
                                   centerX + (scannerRectWidth / 2.0f),
                                   centerY + (scannerRectHeight / 2.0f));
        canvas.drawRect(scanRect, paint);

        drawScannerCornersPath(canvas, scanRect, scannerCornersWidth, color);
    }

    private void drawScannerCornersPath(Canvas canvas, RectF scanRect, int cornerWidth, @ColorRes int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(ContextCompat.getColor(getContext(), color));
        paint.setStrokeJoin(Paint.Join.MITER);

        Path path = new Path();

        path.moveTo(scanRect.left, scanRect.top + cornerWidth);
        path.lineTo(scanRect.left, scanRect.top);
        path.lineTo(scanRect.left + cornerWidth, scanRect.top);

        path.moveTo(scanRect.right - cornerWidth, scanRect.top);
        path.lineTo(scanRect.right, scanRect.top);
        path.lineTo(scanRect.right, scanRect.top + cornerWidth);

        path.moveTo(scanRect.left, scanRect.bottom - cornerWidth);
        path.lineTo(scanRect.left, scanRect.bottom);
        path.lineTo(scanRect.left + cornerWidth, scanRect.bottom);

        path.moveTo(scanRect.right - cornerWidth, scanRect.bottom);
        path.lineTo(scanRect.right, scanRect.bottom);
        path.lineTo(scanRect.right, scanRect.bottom - cornerWidth);

        canvas.drawPath(path, paint);
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        scannerViewBackgroundBitmap = null;
    }
}