package ru.vasiliev.sandbox.camera2.framework.scanner;

import android.content.Context;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;

import ru.vasiliev.sandbox.camera2.utils.ImageUtils;

public class GMSBarcodeScanner implements BarcodeScanner {

    private BarcodeDetector barcodeDetector;

    public GMSBarcodeScanner(Context context) {
        barcodeDetector = new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.CODE_128)
                                                              .build();
    }

    @Override
    public String scan(Image image) {
        Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(ImageUtils.convertYUV420888ToNV21(image)),
                image.getWidth(), image.getHeight(), ImageFormat.NV21)
                                         .build();
        SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
        for (int i = 0; i < barcodes.size(); i++) {
            int key = barcodes.keyAt(i);
            Barcode barcode = barcodes.get(key);
            if (barcode != null) {
                return barcode.rawValue;
            }
        }
        return null;
    }
}
