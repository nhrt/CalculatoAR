package com.example.swtp;


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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
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
import java.util.concurrent.CopyOnWriteArrayList;


public class DetectorActivity extends CameraActivity {


    /**
     * UITask is a AsyncTask for updating the resultview.
     * Uses the class Homography to compute the current homography.
     * @see com.example.swtp.openCV.Homography
     **/
    private static class UITask extends AsyncTask {
        private WeakReference<DetectorActivity> activityReference;
        private Homography openCV = new Homography();

        // only retain a weak reference to the activity
        UITask(DetectorActivity context) {
            this.activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Object[] objects) {
            final DetectorActivity activity = activityReference.get();
            LOGGER.i("UI Task started");
            int[] imgArray;
            Bitmap dstImg = null;
            Bitmap srcImg;
            Mat homography = null;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (activity != null && !activity.isFinishing() && !isCancelled()) {
                if (!activity.gotSolution) {
                    dstImg = null;
                    continue;
                }

                if (dstImg != null) {
                    srcImg = dstImg;
                } else{
                    srcImg = Bitmap.createBitmap(activity.getRgbBytes(), 0, activity.previewWidth, activity.previewWidth, activity.previewHeight, Bitmap.Config.ARGB_8888);
                    srcImg = activity.rotateBitmap(srcImg, 90);
                }

                imgArray = activity.getRgbBytes();
                if (imgArray != null) {
                    dstImg = Bitmap.createBitmap(activity.getRgbBytes(), 0, activity.previewWidth, activity.previewWidth, activity.previewHeight, Bitmap.Config.ARGB_8888);
                    dstImg = activity.rotateBitmap(dstImg, 90);
                    try {
                        homography = openCV.findHomography(srcImg, dstImg);
                    } catch (CvException cv) {
                        LOGGER.i("CV EXCEPTION while searching homography");
                    }
                }

                if (homography != null && !homography.empty()) {
                    for (Pair<String, RectF> result : activity.results) {
                        Mat point = new Mat();
                        point.push_back(new MatOfPoint2f(new Point(result.second.right, result.second.bottom)));
                        Core.perspectiveTransform(point, point, homography);
                        LOGGER.i("%f %f", (float) point.get(0, 0)[0], (float) point.get(0, 0)[1]);
                        result.second.right = (float) point.get(0, 0)[0];
                        result.second.bottom = (float) point.get(0, 0)[1];
                    }
                }
                publishProgress();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] objects) {
            super.onProgressUpdate();
            final DetectorActivity activity = activityReference.get();

            if (activity != null && !activity.isFinishing() && activity.resultView != null) {
                if (!activity.resultView.hasResult()) {
                    activity.resultView.setResult(activity.results);
                }
                activity.resultView.invalidate();
            }
        }

    }

    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = true;
    private static final float TEXT_SIZE_DIP = 10;

    private MultiBoxTracker tracker;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private List<Pair<String, RectF>> results = new CopyOnWriteArrayList<>();
    //private List<Pair<String, RectF>> results = new ArrayList<>();

    private Classifier detector;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private byte[] luminanceCopy;
    private long timestamp = 0;

    private FormulaExtractor formulaExtractor = new FormulaExtractor();
    private Parser parser = new Parser();


    private boolean finishedCalc; //no new results while true
    private int counter; //counter how many recognition loops where executed
    private List<Classifier.Recognition> recognition_buffer = new ArrayList<>();
    private List<Classifier.Recognition> recognitions;
    private UITask resultThread;

    private ShareScreenshotButton btn_screenshot;
    private SaveScreenshotButton btn_save;
    private OverlayView trackingOverlay;
    private ResultView resultView;
    private ProgressBar spinner;
    private FrameLayout flash;
    private boolean isWaiting = false;
    boolean gotSolution = false;

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


    /**
     * Uses the image returned from {@code getRgbBytes()} and tries to find detections
     * with the class {@code Classifier}. Stores the results in the datastructure {@code results]
     * @see com.example.swtp.Classifier
     **/
    @Override
    protected void processImage() {

        ++timestamp;
        byte[] originalLuminance = getLuminance();
        final long currTimestamp = timestamp;


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
        final List<Classifier.Recognition> tmpResults = detector.recognizeImage(croppedBitmap);

        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();

        for (final Classifier.Recognition result : tmpResults) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= Settings.MINIMUM_CONFIDENCE_TF_OD_API) {
                //canvas.drawRect(location, paint);
                cropToFrameTransform.mapRect(location);
                location.offset(Settings.RESULT_OFFSET_X, Settings.RESULT_OFFSET_Y);
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
    }

    /**
     *Manages the processing of the image stream.
     *Spins the spinner on the main view.
     *Uses the recognition_buffer to cache the last recognitions. They are then used to extract the formulas.
     *To change the interval of processing change AMOUNT_SSD and DETECTIONS_INTERVAL_SECONDS in the class Settings
     * @see Settings
     * @see com.example.swtp.recognition.FormulaExtractor
     */
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
                    results.add(new Pair<>(String.valueOf(result), location));
                }
            }

            updateSpinner(false);
            isWaiting = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isWaiting = false;
                    updateSpinner(true);
                }
            }, gotSolution ? Settings.DETECTION_INTERVAL_SECONDS * 1000 : 0);

            readyForNextImage();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize the Clicklistener for the screenshot button
        btn_screenshot = findViewById(R.id.btn_screenshot);
        flash = findViewById(R.id.flash);
        btn_screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap screenshot = btn_screenshot.takeScreenShot(getRgbBytes(), previewWidth, previewHeight, results);
                String path = btn_screenshot.saveScreenShot(screenshot, timestamp);
                File file = new File(path);
                Uri imageUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "com.example.swtp.provider",
                        file);
                btn_screenshot.shareScreenShot(imageUri);
            }
        });

        //Initialize the Clicklistener for the StoreScreenshotbutton
        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Bitmap screenshot = btn_save.takeScreenShot(getRgbBytes(), previewWidth, previewHeight, results);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        flashScreen(500);
                        btn_save.storeScreenShot(screenshot);
                    }
                });

            }
        });
        spinner = findViewById(R.id.spinner);
        //Force the OpenCV libs to load
        OpenCVLoader.initDebug();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        startUpdateThread();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        resultThread.cancel(true);
        counter = 0;
        finishedCalc = false;
        isWaiting = false;
        results.clear();
    }

    /**
     * Changes the visibility of the spinner
     * @param spin
     */
    private void updateSpinner(boolean spin) {
        final int flag = spin ? View.VISIBLE : View.GONE;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(flag);
            }

        });
    }

    private void startUpdateThread() {
        resultThread = new UITask(this);
        resultThread.execute();
        LOGGER.i("New UI Task initialized");
    }

    /**
     * Shows the Framelayout flash for a period of time.
     * Looks like a flash.
     * @param duration  time of flash in milliseconds
     */
    private void flashScreen(int duration){
        if(flash == null){
            LOGGER.i("no flash found");
            return;
        }
        AlphaAnimation fade = new AlphaAnimation(1, 0);
        fade.setDuration(duration);
        fade.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                flash.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                flash.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        });
        flash.startAnimation(fade);
    }

    /**
     *
     * @param bitmap    source bitmap which will be rotated
     * @param degrees   degrees°
     * @return copy of source bitmap
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}


