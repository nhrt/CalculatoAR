package com.example.swtp.recognition;

import android.graphics.RectF;

import com.example.swtp.Classifier;
import com.example.swtp.env.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;



public class FormulaExtractor {
    private static final Logger LOGGER = new Logger();

    /*
     Extract takes a List of Recognitions and extract every Recognition which is inside of a formula
     Input-Type: List<Classifier.Recognition> mappedRecognitions
     Return-Type:  List<List<Classifier.Recognition>> "A list of a list of objects belonging to a formula
     -> every List-Item is a formula
     -> the first element of every formula is the formula object itself
     -> every object which is not in a formula gets deleted
     */
    public List<List<Classifier.Recognition>> extract(List<Classifier.Recognition> mappedRecognitions){
        //LOGGER.i("Start Extracting Recognitions");
        List<List<Classifier.Recognition>> formulas = new ArrayList<>();


        //Find all "Formula" objects in the detections
        ListIterator<Classifier.Recognition> iterator = mappedRecognitions.listIterator();
        List<Classifier.Recognition> formula;
        Classifier.Recognition tmp;
        while(iterator.hasNext()){
            tmp = iterator.next();
            if(tmp.getTitle().equals("formula")){
                formula = new ArrayList<Classifier.Recognition>();
                formula.add(tmp);
                formulas.add(formula);
            }
        }

        //order objects to fitting formulas
        iterator = mappedRecognitions.listIterator();
        RectF locationFormula;
        while (iterator.hasNext()){
            tmp = iterator.next();
            if(!tmp.getTitle().equals("formula")){
                for (int i = 0; i < formulas.size(); i++) {
                    locationFormula = formulas.get(i).get(0).getLocation();
                    //Compare the middle of the object to to outer-bound of the formula
                    // -> If inside of this formula-bounds then arrange to the formula
                    int centerY = (int)tmp.getLocation().centerY();
                    int centerX = (int)tmp.getLocation().centerX();
                    if(locationFormula.contains(centerX,centerY)){
                        formulas.get(i).add(tmp);
                    }
                }
            }
        }

        //Sort the formula horizontal
        for (int i = 0; i < formulas.size(); i++) {
            Collections.sort(formulas.get(i), new Comparator<Classifier.Recognition>() {
                @Override
                public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
                    return (int)(o1.getLocation().left - o2.getLocation().left);
                }
            });
        }
        // LOGGER.i("Return Extracted Recognitions");
        return formulas;
    }
}
