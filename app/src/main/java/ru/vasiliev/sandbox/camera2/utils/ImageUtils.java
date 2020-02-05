package ru.vasiliev.sandbox.camera2.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.net.Uri;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import net.glxn.qrgen.android.QRCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

import timber.log.Timber;

public class ImageUtils {

    public static String imageToBase64(Image image) {
        if (image != null) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String base64Image = Base64.encodeToString(bytes,
                                                       Base64.NO_WRAP);
            image.close();
            return base64Image;
        }
        return null;
    }

    public static Bitmap imageToBitmap(Image image) {
        if (image != null) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes,
                                                 0,
                                                 bytes.length,
                                                 null);
        }
        return null;
    }

    public static byte[] convertYUV420888ToNV21(Image yuv420888Image) {
        // Converting YUV_420_888 data to YUV_420_SP (NV21).
        byte[] data;
        ByteBuffer buffer0 = yuv420888Image.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = yuv420888Image.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        data = new byte[buffer0_size + buffer2_size];
        buffer0.get(data,
                    0,
                    buffer0_size);
        buffer2.get(data,
                    buffer0_size,
                    buffer2_size);
        return data;
    }

    public static Bitmap base64ToBitmap(String base64photo) {
        byte[] imageAsBytes = Base64.decode(base64photo.getBytes(),
                                            Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(imageAsBytes,
                                             0,
                                             imageAsBytes.length);
    }

    public static Bitmap rotateBitmap(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source,
                                   0,
                                   0,
                                   source.getWidth(),
                                   source.getHeight(),
                                   matrix,
                                   true);
    }

    public static Bitmap textToBitmap(String text, float textSize) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);

        float baseline = -textPaint.ascent(); // ascent() is negative
        int width = (int) (textPaint.measureText(text) + 0.5f);
        int height = (int) (baseline + textPaint.descent() + 0.5f);

        Bitmap bitmap = Bitmap.createBitmap(width,
                                            height,
                                            Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text,
                        0,
                        baseline,
                        textPaint);

        return bitmap;
    }

    public static Bitmap createQRCodeBitmap(String barcode, int widthPx, int heightPx) {
        return QRCode.from(barcode)
                     .withSize(widthPx,
                               heightPx)
                     .bitmap();
    }

    public static Bitmap createBarcodeBitmap(String data, int width) throws WriterException {
        int height = width / 3;
        MultiFormatWriter writer = new MultiFormatWriter();
        String finalData = Uri.encode(data);

        // Use 1 as the height of the matrix as this is a 1D Barcode.
        BitMatrix bm = writer.encode(finalData,
                                     BarcodeFormat.CODE_128,
                                     width,
                                     1);
        int bmWidth = bm.getWidth();

        Bitmap imageBitmap = Bitmap.createBitmap(bmWidth,
                                                 height,
                                                 Bitmap.Config.ARGB_8888);
        for (int i = 0; i < bmWidth; i++) {
            // Paint columns of width 1
            int[] column = new int[height];
            Arrays.fill(column,
                        bm.get(i,
                               0) ? Color.BLACK : Color.WHITE);
            imageBitmap.setPixels(column,
                                  0,
                                  1,
                                  i,
                                  0,
                                  1,
                                  height);
        }

        return imageBitmap;
    }

    public static void dumpToFile(Context context, String base64photo) {
        dumpToFile(context,
                   base64photo,
                   null);
    }

    public static void dumpToFile(Context context, String base64photo, String fileName) {
        File dumpFile;
        if (fileName == null) {
            dumpFile = new File(context.getFilesDir() + String.format(Locale.getDefault(),
                                                                      "/camera/photo_%d.jpg",
                                                                      System.currentTimeMillis()));
        } else {
            dumpFile = new File(context.getFilesDir() + "/camera/" + fileName);
        }
        byte[] rawImageBytes = Base64.decode(base64photo,
                                             Base64.NO_WRAP);
        try (FileOutputStream out = new FileOutputStream(dumpFile)) {
            out.write(rawImageBytes);
        } catch (IOException e) {
            Timber.e("",
                     e);
        }
    }
}
