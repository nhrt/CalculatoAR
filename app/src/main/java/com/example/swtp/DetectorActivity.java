package com.example.swtp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.example.swtp.customview.ResultView;
import com.example.swtp.env.BorderedText;
import com.example.swtp.env.ImageUtils;
import com.example.swtp.env.Logger;
import com.example.swtp.recognition.FormulaExtractor;
import com.example.swtp.recognition.Parser;
import com.example.swtp.tracking.MultiBoxTracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Double.NaN;

public class DetectorActivity extends CameraActivity {


    private static class UITask extends AsyncTask{
        private WeakReference<DetectorActivity> activityReference;

        // only retain a weak reference to the activity
        UITask(DetectorActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            final DetectorActivity activity = activityReference.get();

            while(activity != null && !activity.isFinishing()) {
                activity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                if(activity.resultView != null){
                                    activity.resultView.setResult(activity.results);
                                }
                            }
                        }
                );

                try {
                    Thread.sleep(7);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
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


    private int counter;
    private List<Classifier.Recognition> recognition_buffer = new ArrayList<>();
    private List<Classifier.Recognition> recognitions;
    private static  AsyncTask resultThread;
    private FloatingActionButton btn_screenshot;

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

        //startUpdateThread();
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
        if(counter < Settings.AMOUNT_SSD){
            counter++;
            processImage();
            recognition_buffer.addAll(recognitions);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    readyForNextImage();
                }
            },50);
        }else {
            counter = 0;
            boolean gotSolution = false;

            //extract and combine all recognitions
            List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(recognition_buffer);
            recognition_buffer.clear();

            //evaluate formulas
            results.clear();
            for (List<Classifier.Recognition> formula : formulas) {
                double result = parser.parse(formula);
                RectF location = formula.get(formula.size() - 1).getLocation();

                if (result != NaN && location != null) {
                    gotSolution = true;
                    LOGGER.i("Result: %s Location: x %f  y %f", String.valueOf(result), location.right, location.bottom);
                    results.add(new Pair(String.valueOf(result), location));
                }
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    readyForNextImage();
                }
            }, gotSolution ? Settings.DETECTION_INTERVAL_SECONDS * 1000 : 0);

        }
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        resultThread.cancel(true);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        startUpdateThread();
        readyForNextImage();
    }


    private void startUpdateThread(){
        resultThread = new UITask(this);
        resultThread.execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        btn_screenshot = findViewById(R.id.btn_screenshot);
        btn_screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultThread.cancel(true);
                Bitmap screenshot = takeScreenShot();
                String path = saveScreenShot(screenshot);
                File file = new File(path);

                Uri imageUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "com.example.swtp.provider",
                        file);

                shareScreenShot(imageUri);
            }
        });
    }

    private void shareScreenShot(Uri uri){
        final Uri finalUri = uri;

        runInBackground(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, finalUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/png");
                try {
                    startActivity(intent);
                }catch (android.content.ActivityNotFoundException ex){
                    ex.printStackTrace();
                }
            }
        });
    }

    private String saveScreenShot(Bitmap screenshot){
        File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot" + timestamp + ".png");
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(imagePath);
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("GREC", e.getMessage(), e);
        } catch (IOException e) {
            Log.e("GREC", e.getMessage(), e);
        }

        LOGGER.i("Image saved at %s",imagePath.getPath());
        return imagePath.getPath();
    }

    private Bitmap takeScreenShot(){
        Bitmap bitmap = Bitmap.createBitmap(getRgbBytes(), previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

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

        synchronized (results){
            RectF pos;
            for (Pair<String, RectF> result : results){
                pos = result.second;
                pos.right = pos.right / displayWidth * width;
                pos.bottom = pos.bottom / displayHeight * height;
                canvas.drawText(result.first,result.second.right, result.second.bottom, paint);
            }
        }

        canvas.setBitmap(rotatedBitmap);
        LOGGER.i("took Screenshot");
        return rotatedBitmap;
    }
}


