package ru.vasiliev.sandbox.camera.device.scanner;

import android.text.TextUtils;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import me.dm7.barcodescanner.zbar.BarcodeFormat;

public class ZBarBarcodeScanner implements BarcodeScanner {

    private ImageScanner scanner;

    public ZBarBarcodeScanner() {
        setupScanner();
    }

    @Override
    public String scan(android.media.Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data, 0, data.length);

        Image barcodeImage = new Image(image.getWidth(), image.getHeight(), "Y800");
        barcodeImage.setData(data);

        int result = scanner.scanImage(barcodeImage);
        if (result != 0) {
            SymbolSet symbolSet = scanner.getResults();
            for (Symbol symbol : symbolSet) {
                // In order to retrieve QR codes containing null bytes we need to
                // use getDataBytes() rather than getData() which uses C strings.
                // Weirdly ZBar transforms all data to UTF-8, even the data returned
                // by getDataBytes() so we have to decode it as UTF-8.
                String barcode = new String(symbol.getDataBytes(), StandardCharsets.UTF_8);
                if (!TextUtils.isEmpty(barcode)) {
                    return barcode;
                }
            }
        }

        return null;
    }

    private void setupScanner() {
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        scanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for (BarcodeFormat format : BarcodeFormat.ALL_FORMATS) {
            scanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }
}
