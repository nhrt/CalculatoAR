package com.example.swtp.recognition;

import android.graphics.RectF;

import com.example.swtp.Classifier;
import com.example.swtp.env.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;



public class FormulaRecognition {
    private static final Logger LOGGER = new Logger();
    public List<List<Classifier.Recognition>> parseRecognitions(List<Classifier.Recognition> mappedRecognitions){
        LOGGER.i("Start Parsing Recognitions");
        List<String> result = new ArrayList<>();
        List<List<Classifier.Recognition>> formulas = getFormulas(mappedRecognitions);
        for(List<Classifier.Recognition> formula : formulas){
            result.add(formulaToString(formula));
        }
        LOGGER.i("Return Parsed Recognitions");
        return result;
    }

    private List<List<Classifier.Recognition>> getFormulas(List<Classifier.Recognition> mappedRecognitions){
        List<List<Classifier.Recognition>> formulas = new ArrayList<>();

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

        iterator = mappedRecognitions.listIterator();
        RectF locationFormula;
        while (iterator.hasNext()){
            tmp = iterator.next();
            if(!tmp.getTitle().equals("formula")){
                for (int i = 0; i < formulas.size(); i++) {
                    locationFormula = formulas.get(i).get(0).getLocation();
                    if(tmp.getLocation().centerY() > locationFormula.top && tmp.getLocation().centerY() < locationFormula.bottom && tmp.getLocation().centerX() > locationFormula.left && tmp.getLocation().centerX() < locationFormula.right){
                        formulas.get(i).add(tmp);
                    }
                }
            }
        }

        for (int i = 0; i < formulas.size(); i++) {
            sortHoritzontal(formulas.get(i));
        }
        return formulas;
    }

    private void sortHoritzontal(List<Classifier.Recognition> mappedRecognitions){
        Collections.sort(mappedRecognitions, new Comparator<Classifier.Recognition>() {
            @Override
            public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
                return (int)(o1.getLocation().left - o2.getLocation().left);
            }
        });
    }





}
