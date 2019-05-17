package com.example.swtp;

import android.util.Size;

public final class Settings {
    static final String TF_OD_API_MODEL_FILE = "optimized_graph.lite";
    static final String TF_OD_API_LABELS_FILE = "retrained_labels.txt";
    static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);
    static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    static final int TF_OD_API_INPUT_SIZE = 224;
    static final float IMAGE_MEAN = 128.0f;
    static final float IMAGE_STD = 128.0f;

    //@toDo: unser model kann nur 12 detections gleichzeitig durchführen
    static final int NUM_DETECTIONS = 12;
}
