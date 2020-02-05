package ru.vasiliev.sandbox.camera2.framework.scanner;

import android.media.Image;

public interface BarcodeScanner {

    String scan(Image image);
}
