package ru.vasiliev.sandbox.camera.device.camera.common

import io.reactivex.Observable
import ru.vasiliev.sandbox.camera.device.camera.data.CaptureMetadata
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFacing
import ru.vasiliev.sandbox.camera.device.camera.util.CameraFlash

interface CameraController {
    fun start(): Boolean
    fun stop()
    fun takePicture()

    val isCameraOpened: Boolean

    /*
    Set<AspectRatio> getSupportedAspectRatios();
    boolean setAspectRatio(AspectRatio ratio);
    AspectRatio getAspectRatio();
    */
    var facing: CameraFacing
    var autoFocus: Boolean
    var flash: CameraFlash
    var autoWhiteBalance: Boolean

    val imageCaptureStream: Observable<ByteArray>
    val imageMetadataStream: Observable<CaptureMetadata>
    val imageProcessorStream: Observable<ByteArray>
}