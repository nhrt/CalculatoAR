package com.example.swtp;


import android.app.Activity;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.example.swtp.custombutton.SaveScreenshotButton;
import com.example.swtp.custombutton.ShareScreenshotButton;
import com.example.swtp.customview.ResultView;
import com.example.swtp.env.BorderedText;
import com.example.swtp.env.ImageUtils;
import com.example.swtp.env.Logger;
import com.example.swtp.openCV.Homography;
import com.example.swtp.recognition.FormulaExtractor;
import com.example.swtp.recognition.Parser;
import com.example.swtp.tracking.MultiBoxTracker;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class DetectorActivity extends CameraActivity {


    private static class UITask extends AsyncTask {
        private WeakReference<DetectorActivity> activityReference;
        public boolean stop;

        // only retain a weak reference to the activity
        UITask(DetectorActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            final DetectorActivity activity = activityReference.get();

            int[] imgArray;
            Bitmap dstImg = null;
            Mat homography = null;
            stop = false;


            while (activity != null && !activity.isFinishing() && !stop) {
                if(!activity.gotSolution){
                    dstImg = null;
                    continue;
                }
                if (dstImg != null){
                    srcImg = dstImg;
                }else{
                    srcImg =  Bitmap.createBitmap(activity.getRgbBytes(), 0, activity.previewWidth, activity.previewWidth, activity.previewHeight, Bitmap.Config.ARGB_8888);
                    srcImg = activity.rotateBitmap(srcImg,90);
                }

                imgArray = activity.getRgbBytes();
                if (imgArray != null) {
                    dstImg = Bitmap.createBitmap(activity.getRgbBytes(), 0, activity.previewWidth, activity.previewWidth, activity.previewHeight, Bitmap.Config.ARGB_8888);
                    dstImg = activity.rotateBitmap(dstImg,90);
                    try {
                        homography = openCV.findHomography(srcImg,dstImg);
                    } catch (CvException cv) {
                        LOGGER.i("CV EXCEPTION while searching homography");
                    }
                }

                if (homography != null && !homography.empty()) {
                    for (Pair<String, RectF> result : activity.results) {
                        Mat point = new Mat();
                        point.push_back(new MatOfPoint2f(new Point(result.second.right,result.second.bottom)));
                        Core.perspectiveTransform(point,point,homography);
                        LOGGER.i("%f %f",(float)point.get(0,0)[0],(float)point.get(0,0)[1]);
                        result.second.right = (float)point.get(0,0)[0];
                        result.second.bottom = (float)point.get(0,0)[1];
                    }
                }

                activity.runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        activity.resultView.invalidate();
                    }
                });
            }
            return null;

        }
    }

    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = true;
    private static final float TEXT_SIZE_DIP = 10;

    private MultiBoxTracker tracker;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private OverlayView trackingOverlay;
    private ResultView resultView;

    private List<Pair<String, RectF>> results = new ArrayList<>();

    private Classifier detector;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private boolean computingDetection = false;
    private byte[] luminanceCopy;
    private long timestamp = 0;


    private FormulaExtractor formulaExtractor = new FormulaExtractor();
    private Parser parser = new Parser();

    private static Bitmap srcImg;
    private static Homography openCV = new Homography();
    private boolean isWaiting;
    private boolean gotSolution;
    private int counter;
    private List<Classifier.Recognition> recognition_buffer = new ArrayList<>();
    private List<Classifier.Recognition> recognitions;
    private UITask resultThread;
    private ShareScreenshotButton btn_screenshot;
    private SaveScreenshotButton btn_save;

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
        BorderedText borderedText = new BorderedText(textSizePx);
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

        int sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("orientation: " + sensorOrientation);
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap = rgbFrameBitmap.copy(Bitmap.Config.ARGB_8888, true);
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
            //requestRender();
        }

        recognitions = mappedRecognitions;


        computingDetection = false;
    }

    protected void processLoop() {
        if (isWaiting) {
            readyForNextImage();
            return;
        }
        gotSolution = false;

        if (counter < Settings.AMOUNT_SSD) {
            counter++;
            processImage();
            recognition_buffer.addAll(recognitions);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    readyForNextImage();
                }
            }, 50);
        } else {
            counter = 0;

            //extract and combine all recognitions
            List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(recognition_buffer);
            recognition_buffer.clear();

            //evaluate formulas
            results.clear();
            for (List<Classifier.Recognition> formula : formulas) {
                double result = parser.parse(formula);
                RectF location = formula.get(formula.size() - 1).getLocation();

                if (!Double.isNaN(result) && location != null) {
                    gotSolution = true;
                    LOGGER.i("Result: %s Location: x %f  y %f", String.valueOf(result), location.right, location.bottom);
                    results.add(new Pair(String.valueOf(result), location));
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultView.setResult(results);
                }
            });

            isWaiting = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isWaiting = false;
                }
            }, gotSolution ? Settings.DETECTION_INTERVAL_SECONDS * 1000 : 0);

            readyForNextImage();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btn_screenshot = findViewById(R.id.btn_screenshot);
        btn_screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultThread.stop = true;
                Bitmap screenshot = btn_screenshot.takeScreenShot(getRgbBytes(),previewWidth,previewHeight,results);
                String path = btn_screenshot.saveScreenShot(screenshot, timestamp);
                File file = new File(path);

                Uri imageUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "com.example.swtp.provider",
                        file);
                btn_screenshot.shareScreenShot(imageUri);
            }
        });

        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultThread.stop = true;
                final Bitmap screenshot = btn_save.takeScreenShot(getRgbBytes(), previewWidth,previewHeight, results);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn_save.storeScreenShot(screenshot);
                    }
                });

            }
        });
        OpenCVLoader.initDebug();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        runInBackground(new Runnable() {
            @Override
            public void run() {
                readyForNextImage();
            }
        });
        startUpdateThread();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        counter = 0;
        isWaiting = false;
        results.clear();
    }

    private void startUpdateThread() {
        resultThread = new UITask(this);
        resultThread.execute();
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}


