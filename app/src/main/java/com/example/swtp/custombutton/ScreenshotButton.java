package com.example.swtp.custombutton;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import com.example.swtp.env.Logger;

import java.util.List;

public abstract class ScreenshotButton extends FloatingActionButton {
    Context context;
    Logger LOGGER = new Logger();

    public ScreenshotButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }


    /**
     * takes bytearray and a result-list to draw a new image which will be returned
     * @param bytes     rgbByteArray of the image
     * @param previewWidth  width of the image
     * @param previewHeight height of the image
     * @param results   results that will be added
     * @return Bitmap of the image with printed results on it
     */
    public Bitmap takeScreenShot(int[] bytes, int previewWidth, int previewHeight, List<Pair<String, RectF>> results) {

        Bitmap bitmap = Bitmap.createBitmap(bytes, previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        Bitmap rotatedBitmap = rotateBitmap(bitmap,90);

        Canvas canvas = new Canvas(rotatedBitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.TRANSPARENT);
        canvas.drawPaint(paint);

        paint.setTextSize(60f);
        paint.setColor(Color.BLACK);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        DisplayMetrics dm = getResources().getDisplayMetrics();


        int displayWidth = dm.widthPixels;
        int displayHeight = dm.heightPixels;

        float left, top;
        synchronized (results) {
            for (Pair<String, RectF> result : results) {
                left = result.second.right;
                top = result.second.bottom;
                left = left / displayWidth * width;
                top = top / displayHeight * height;
                canvas.drawText(result.first, left, top, paint);
            }
        }

        canvas.setBitmap(rotatedBitmap);
        LOGGER.i("took Screenshot");
        return rotatedBitmap;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


}
