package ru.vasiliev.sandbox.camera.device.scanner;

import android.media.Image;

public interface BarcodeScanner {

    String scan(Image image);
}
