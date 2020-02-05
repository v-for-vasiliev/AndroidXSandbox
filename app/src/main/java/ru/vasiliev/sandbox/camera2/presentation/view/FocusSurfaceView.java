package ru.vasiliev.sandbox.camera2.presentation.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FocusSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final SurfaceHolder mSurfaceHolder;

    public FocusSurfaceView(Context context) {
        super(context);
        mSurfaceHolder = getHolder();
        setFocusable(true);
        setZOrderOnTop(true);
        mSurfaceHolder.addCallback(this);
    }

    public FocusSurfaceView(Context context, AttributeSet attrs) {
        super(context,
              attrs);
        mSurfaceHolder = getHolder();
        setFocusable(true);
        setZOrderOnTop(true);
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}