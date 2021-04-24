package ru.vasiliev.sandbox.camera.device.camera.common

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.widget.FrameLayout

abstract class CameraPreview : FrameLayout {
    var previewWidth = 0
        private set
    var previewHeight = 0
        private set
    private var previewSurfaceChangedListener: PreviewSurfaceChangedListener? = null

    constructor(context: Context) : super(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    interface PreviewSurfaceChangedListener {
        fun onPreviewSurfaceChanged()
    }

    fun setPreviewSurfaceChangedListener(previewSurfaceChangedListener: PreviewSurfaceChangedListener?) {
        this.previewSurfaceChangedListener = previewSurfaceChangedListener
    }

    protected fun dispatchSurfaceChanged() {
        if (previewSurfaceChangedListener != null) {
            previewSurfaceChangedListener!!.onPreviewSurfaceChanged()
        }
    }

    abstract val previewOutputClass: Class<*>?
    abstract val isReady: Boolean
    abstract val surface: Surface?
    abstract val surfaceTexture: SurfaceTexture?
    abstract var displayOrientation: Int
    open fun setPreviewBufferSize(width: Int, height: Int) {}
    protected fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }
}