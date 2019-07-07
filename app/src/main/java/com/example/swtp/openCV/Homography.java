package com.example.swtp.openCV;

import android.graphics.Bitmap;

import com.example.swtp.env.Logger;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.BRISK;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Homography {
    Logger LOGGER = new Logger();

    public Mat findHomography(Bitmap img1, Bitmap img2){
        LOGGER.i("Searching homography");

        Mat homoMat = null;

        //Save Images as grayscale bitmap
        Mat imgMat1 = new Mat();
        Mat imgMat2 = new Mat();
        Utils.bitmapToMat(img1, imgMat1);
        Utils.bitmapToMat(img2, imgMat2);
        Imgproc.cvtColor(imgMat1,imgMat1,Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(imgMat2,imgMat2,Imgproc.COLOR_RGB2GRAY);

        //find keypoints
        MatOfKeyPoint keyMat1 = new MatOfKeyPoint();
        MatOfKeyPoint keyMat2 = new MatOfKeyPoint();

        BRISK detector = BRISK.create();
        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();
        detector.detectAndCompute(imgMat1,new Mat(), keyMat1, descriptor1);
        detector.detectAndCompute(imgMat2,new Mat(), keyMat2, descriptor2);

        //Matching
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptor1, descriptor2, knnMatches, 2);

        //Filter matches
        float ratioThresh = 0.75f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);

        List<KeyPoint> listOfKeyPoints1 = keyMat1.toList();
        List<KeyPoint> listOfKeyPoints2 = keyMat2.toList();
        List<Point> points1 = new ArrayList<>();
        List<Point> points2 = new ArrayList<>();

        for (int i = 0; i < listOfGoodMatches.size(); i++) {
            //-- Get the keypoints from the good matches
            points1.add(listOfKeyPoints1.get(listOfGoodMatches.get(i).queryIdx).pt);
            points2.add(listOfKeyPoints2.get(listOfGoodMatches.get(i).trainIdx).pt);
        }

        MatOfPoint2f pointsMat1 = new MatOfPoint2f();
        MatOfPoint2f pointsMat2 = new MatOfPoint2f();
        pointsMat1.fromList(points1);
        pointsMat2.fromList(points2);
        double ransacReprojThreshold = 3.0;
        Mat H = Calib3d.findHomography( pointsMat1, pointsMat2, Calib3d.RANSAC, ransacReprojThreshold );

        LOGGER.i("Found homography");
        return H;
    }
}

