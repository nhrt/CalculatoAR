package com.example.swtp.custombutton;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class ShareScreenshotButton extends ScreenshotButton {


    public ShareScreenshotButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void shareScreenShot(Uri uri) {
        final Uri finalUri = uri;

        Thread thread = new Thread(){
            public void run(){
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, finalUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/png");
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public String saveScreenShot(Bitmap screenshot, long timestamp) {
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot" + timestamp + ".png");
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(imagePath);
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            LOGGER.i("File not found");
            LOGGER.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.i("IOException");
            LOGGER.e("GREC", e.getMessage(), e);
        }
        LOGGER.i("Image saved at %s", imagePath.getPath());
        return imagePath.getPath();
    }
}
