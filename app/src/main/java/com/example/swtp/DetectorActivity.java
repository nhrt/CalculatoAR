package com.example.swtp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.example.swtp.customview.ResultView;
import com.example.swtp.env.BorderedText;
import com.example.swtp.env.ImageUtils;
import com.example.swtp.env.Logger;
import com.example.swtp.recognition.FormulaExtractor;
import com.example.swtp.recognition.Parser;
import com.example.swtp.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Double.NaN;

public class DetectorActivity extends CameraActivity {
    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = true;
    private static final float TEXT_SIZE_DIP = 10;

    private BorderedText borderedText;
    private MultiBoxTracker tracker;
    private Integer sensorOrientation;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    OverlayView trackingOverlay;
    ResultView resultView;

    List<Pair<String, RectF>> results;

    Classifier detector;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private boolean computingDetection = false;
    private byte[] luminanceCopy;
    private long timestamp = 0;
    private long lastTimestamp = 0;
    private FormulaExtractor formulaExtractor = new FormulaExtractor();
    private Parser parser = new Parser();
    List<Classifier.Recognition> recognitions;

    RectF location;
    double result;

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return Settings.DESIRED_PREVIEW_SIZE;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }


    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = Settings.TF_OD_API_INPUT_SIZE;

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    getAssets(),
                    Settings.TF_OD_API_MODEL_FILE,
                    Settings.TF_OD_API_LABELS_FILE,
                    Settings.TF_OD_API_INPUT_SIZE,
                    Settings.TF_OD_API_IS_QUANTIZED);
            cropSize = Settings.TF_OD_API_INPUT_SIZE;
        } catch (IOException e) {
            LOGGER.e("Exception initializing classifier!", e);
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("orientation: " + sensorOrientation);
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform =
                ImageUtils.getTransformationMatrix(
                        cropSize, cropSize,
                        previewHeight, previewWidth,
                        0, MAINTAIN_ASPECT);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                    }
                });

        resultView = findViewById(R.id.resultView);

    }

    @Override
    protected void processImage() {

        ++timestamp;
        byte[] originalLuminance = getLuminance();
        final long currTimestamp = timestamp;

        lastTimestamp = currTimestamp;

        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        LOGGER.i("Running detection on image " + currTimestamp);
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);


        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= Settings.MINIMUM_CONFIDENCE_TF_OD_API) {
                //canvas.drawRect(location, paint);
                cropToFrameTransform.mapRect(location);
                location.offset(-75, 300);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }
        if (Settings.SHOW_RECTS) {
            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
            trackingOverlay.postInvalidate();
            requestRender();
        }

        recognitions = mappedRecognitions;


        computingDetection = false;
    }

    protected void processLoop() {
        processImage();
        List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(recognitions);
        results = new ArrayList<>();
        boolean gotSolution = false;
        for (List<Classifier.Recognition> formula : formulas) {
            result = parser.parse(formula);
            if (result != NaN) {
                gotSolution = true;
                location = formula.get(formula.size() - 1).getLocation();
                location.offset(0, 0);
                if(location != null){
                    results.add(new Pair(result + "", location));
                }
                trackingOverlay.postInvalidate();
            }
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        resultView.setResult(results);
                                    }
                                }
                        );
                    }
                }
        );

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                readyForNextImage();
            }
        }, gotSolution ? Settings.DETECTION_INTERVAL_SECONDS * 1000 : 0);
    }
}


