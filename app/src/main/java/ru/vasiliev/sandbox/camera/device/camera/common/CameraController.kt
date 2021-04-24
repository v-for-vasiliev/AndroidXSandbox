package ru.vasiliev.sandbox.camera.device.camera.common

import io.reactivex.Observable
import ru.vasiliev.sandbox.camera.device.camera.data.CaptureMetadata
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash

interface CameraController {
    fun start(): Boolean
    fun stop()
    val isCameraOpened: Boolean
    var facing: CameraFacing?

    /*
    Set<AspectRatio> getSupportedAspectRatios();

    boolean setAspectRatio(AspectRatio ratio);

    AspectRatio getAspectRatio();
    */
    var autoFocus: Boolean
    var flash: CameraFlash?
    var autoWhiteBalance: Boolean
    fun takePicture()
    val imageCaptureStream: Observable<ByteArray?>?
    val imageMetadataStream: Observable<CaptureMetadata?>?
    val imageProcessorStream: Observable<ByteArray?>?
}