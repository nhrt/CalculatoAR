package com.example.swtp.custombutton;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Random;

public class SaveScreenshotButton extends ScreenshotButton {
    public SaveScreenshotButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void storeScreenShot(Bitmap screenshot, Activity current) {

        WeakReference<Activity> activityReference = new WeakReference<>(current);
        String root = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/CalculatoAR");

        myDir.mkdirs();
        int n = 10000;
        Random generator = new Random();
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            final FileOutputStream out = new FileOutputStream(file);
            final Bitmap finalBitmap = screenshot;
            current.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                }
            });

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        LOGGER.i("ExternalStorage", "Scanned " + path + ":");
                        LOGGER.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }
}
