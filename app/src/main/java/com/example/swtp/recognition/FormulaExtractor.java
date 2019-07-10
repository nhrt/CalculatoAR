package com.example.swtp.recognition;

import android.graphics.RectF;

import com.example.swtp.Classifier;
import com.example.swtp.env.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;


public class FormulaExtractor {
    private static final Logger LOGGER = new Logger();
    

    /**
     * Extract takes a List of Recognitions and extracts every Recognition which is inside of a formula.
     * Every object which is not inside a formula will be removed. If two formulas are overlapping, the smaller one gets deleted.
     * @param mappedRecognitions All recognitions which will be used
     * @return List of formulas which are a list although. First object of ever< formula is the formula detection itself
     */
    public List<List<Classifier.Recognition>> extract(List<Classifier.Recognition> mappedRecognitions) {
        //LOGGER.i("Start Extracting Recognitions");
        List<List<Classifier.Recognition>> formulas = new ArrayList<>();


        //find all formula-objects in the mappedRecognitions
        List<Classifier.Recognition> formula;
        for (Classifier.Recognition recognition : mappedRecognitions){
            if(recognition.getTitle().equals("formula")){
                formula = new ArrayList<>();
                formula.add(recognition);
                formulas.add(formula);
            }
        }

        //Filter found formulas
        if(formulas.size() > 1){
            Set<List<Classifier.Recognition>> goodFormulas = new HashSet<>();
            Set<List<Classifier.Recognition>> removedFormulas = new HashSet<>();
            Classifier.Recognition formulaA;
            Classifier.Recognition formulaB;
            for (List<Classifier.Recognition> formulaA_list : formulas){
                formulaA = formulaA_list.get(0);
                if(removedFormulas.contains(formulaA_list) || goodFormulas.contains(formulaA_list)){
                    continue;
                }
                for (List<Classifier.Recognition> formulaB_list : formulas){
                    if(removedFormulas.contains(formulaB_list)){
                        continue;
                    }
                    formulaB = formulaB_list.get(0);
                    if(!formulaA.equals(formulaB)){
                        if(formulaA.getLocation().intersect(formulaB.getLocation())){
                            int areaA = (int) (formulaA.getLocation().height() * formulaA.getLocation().width());
                            int areaB = (int) (formulaB.getLocation().height() * formulaB.getLocation().width());
                            if (areaA <= areaB) {
                                goodFormulas.add(formulaB_list);
                                removedFormulas.add(formulaA_list);
                            } else {
                                goodFormulas.add(formulaA_list);
                                removedFormulas.add(formulaB_list);
                            }
                        }else{
                            goodFormulas.add(formulaA_list);
                        }
                    }
                }
            }
            formulas = new ArrayList<>(goodFormulas);
        }

        //order objects to fitting formulas
        double threshold = 0.3;
        RectF locationFormula;
        for(Classifier.Recognition tmp : mappedRecognitions){
            RectF tmpLoc = tmp.getLocation();
            if (!tmp.getTitle().equals("formula")) {
                for(List<Classifier.Recognition> currentFormula : formulas){
                    locationFormula = currentFormula.get(0).getLocation();
                    //Compare the middle of the object to to outer-bound of the formula
                    // -> If inside of this formula-bounds then arrange to the formula
                    if (locationFormula.contains(tmpLoc.centerX(), tmpLoc.centerY())) {
                        //Check if same detection is already in formula
                        if (currentFormula.size() < 2) {
                            currentFormula.add(tmp);
                        } else {
                            RectF objLoc;
                            boolean hadIntersection = false;
                            Classifier.Recognition obj;
                            int size = currentFormula.size();
                            for (int j = 1; j < size; j++) {
                                obj = currentFormula.get(j);
                                objLoc = obj.getLocation();
                                if (tmpLoc.intersect(objLoc)) {
                                    hadIntersection = true;
                                    float width = (objLoc.width() + tmpLoc.width()) / 2;
                                    int div;
                                    if (objLoc.right - tmpLoc.left > 0) {
                                        //obj left hand side of tmp
                                        div = (int) (objLoc.right - tmpLoc.left);
                                    } else {
                                        //obj right hand side of tmp
                                        div = (int) (tmpLoc.right - objLoc.left);
                                    }
                                    double x = div / width;
                                    if (x < threshold) {
                                        currentFormula.add(tmp);
                                    }
                                }
                            }
                            if (!hadIntersection) {
                                currentFormula.add(tmp);
                            }
                        }

                    }
                }
            }
        }


        //Sort the formula horizontal
        for (int i = 0; i < formulas.size(); i++) {
            Collections.sort(formulas.get(i), new Comparator<Classifier.Recognition>() {
                @Override
                public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
                    return (int) (o1.getLocation().left - o2.getLocation().left);
                }
            });
        }


        // LOGGER.i("Return Extracted Recognitions");
        return formulas;
    }
}


        /*
        if (formulas.size() > 1) {
            Set<List<Classifier.Recognition>> goodFormulas = new HashSet<>();
            Set<List<Classifier.Recognition>> removedFormulas = new HashSet<>();
            ListIterator<List<Classifier.Recognition>> iteratorFormulaA = formulas.listIterator();
            ListIterator<List<Classifier.Recognition>> iteratorFormulaB;
            Classifier.Recognition formulaA;
            Classifier.Recognition formulaB;
            List<Classifier.Recognition> listA;
            List<Classifier.Recognition> listB;
            int areaA, areaB;
            while (iteratorFormulaA.hasNext()) {
                listA = iteratorFormulaA.next();
                formulaA = listA.get(0);
                if (removedFormulas.contains(listA)) continue;
                iteratorFormulaB = formulas.listIterator();
                while (iteratorFormulaB.hasNext()) {
                    listB = iteratorFormulaB.next();
                    if (removedFormulas.contains(listB)) continue;
                    formulaB = listB.get(0);
                    if (!formulaA.equals(formulaB)) {
                        if (formulaA.getLocation().intersect(formulaB.getLocation())) {
                            //are overlapping -> remove smaller formula
                            areaA = (int) (formulaA.getLocation().height() * formulaA.getLocation().width());
                            areaB = (int) (formulaB.getLocation().height() * formulaB.getLocation().width());
                            if (areaA <= areaB) {
                                if(goodFormulas.contains(listA))goodFormulas.add(listB);
                                removedFormulas.add(listA);
                            } else {
                                if(!goodFormulas.contains(listA)) goodFormulas.add(listA);
                                removedFormulas.add(listB);
                            }
                        } else {
                            if(!removedFormulas.contains(listA) && !goodFormulas.contains(listA))
                                goodFormulas.add(listA);
                        }
                    }
                }
            }
            formulas = new ArrayList<>(goodFormulas);
        }
        */