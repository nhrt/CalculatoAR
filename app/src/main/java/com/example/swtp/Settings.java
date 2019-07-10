package com.example.swtp;

import android.util.Size;

/**
 * This class contains values to easily change app behavior.
 */
public final class Settings {
    static final String TF_OD_API_MODEL_FILE = "retrained_graph_v2.tflite";     //model specific only change when changing model
    static final String TF_OD_API_LABELS_FILE = "retrained_labels.txt";         //model specific only change when changing model
    static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);  //model specific only change when changing model
    static public  final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;             //between 0 and 1. 1 max 0 min.
    static final int TF_OD_API_INPUT_SIZE = 300;                                //model specific only change when changing model
    static final float IMAGE_MEAN = 128.0f;                                     //model specific only change when changing model
    static final float IMAGE_STD = 128.0f;                                      //model specific only change when changing model
    static final boolean TF_OD_API_IS_QUANTIZED = false;                        //model specific only change when changing model
    static final int NUM_DETECTIONS = 50;                                       //model specific only change when changing model
    static boolean SHOW_RECTS = false;                                          //for debugging
    static final int DETECTION_INTERVAL_SECONDS = 5;                            //time period for process interval
    static final int AMOUNT_SSD = 3;                                            //how many frames are used for the formula extractions
    static final int RESULT_OFFSET_X = -75;                                     //offsets x and y to compensate a small detection error
    static final int RESULT_OFFSET_Y = 300;
}
