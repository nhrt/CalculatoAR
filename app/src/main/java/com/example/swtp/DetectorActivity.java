package com.example.swtp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.util.Size;
import android.util.TypedValue;


import com.example.swtp.env.BorderedText;
import com.example.swtp.env.ImageUtils;
import com.example.swtp.env.Logger;
import com.example.swtp.tracking.MultiBoxTracker;




public class DetectorActivity extends CameraActivity{
    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = true;
    private static final float TEXT_SIZE_DIP = 10;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private BorderedText borderedText;
    private MultiBoxTracker tracker;
    private Integer sensorOrientation;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    OverlayView trackingOverlay;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }


    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;


        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        if(trackingOverlay != null){
            trackingOverlay.addCallback(
                    new OverlayView.DrawCallback() {
                        @Override
                        public void drawCallback(final Canvas canvas) {
                            tracker.draw(canvas);
                        }
                    });
        }
    }

    @Override
    protected void processImage() {
    }


}
