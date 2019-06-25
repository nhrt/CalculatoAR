package com.example.swtp.openCV;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FastFeatureDetector;

public class TestCV {

    public void test(Bitmap img1, Bitmap img2){

        Mat imgMat1 = new Mat(), imgMat2 = new Mat();
        MatOfKeyPoint point = new MatOfKeyPoint();

        FastFeatureDetector featureDetector = FastFeatureDetector.create(4);
        featureDetector.detect(imgMat1, point);
    }
}
