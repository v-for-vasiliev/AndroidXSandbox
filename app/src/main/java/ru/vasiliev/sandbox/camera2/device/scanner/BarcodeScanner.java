package ru.vasiliev.sandbox.camera2.device.scanner;

import android.media.Image;

public interface BarcodeScanner {

    String scan(Image image);
}
