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
     -> if two formulas are overlapping, the smaller one gets deleted
     */
    public List<List<Classifier.Recognition>> extract(List<Classifier.Recognition> mappedRecognitions) {
        //LOGGER.i("Start Extracting Recognitions");
        List<List<Classifier.Recognition>> formulas = new ArrayList<>();


        //Find all "Formula" objects in the detections
        ListIterator<Classifier.Recognition> iterator = mappedRecognitions.listIterator();
        List<Classifier.Recognition> formula;
        Classifier.Recognition tmp;
        while (iterator.hasNext()) {
            tmp = iterator.next();
            if (tmp.getTitle().equals("formula")) {
                formula = new ArrayList<Classifier.Recognition>();
                formula.add(tmp);
                formulas.add(formula);
            }
        }

        //remove every formula which is a false detection
        if (formulas.size() > 1) {
            List<List<Classifier.Recognition>> tmpList = new ArrayList<>();
            List<List<Classifier.Recognition>> removedLists = new ArrayList<>();
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
                if (removedLists.contains(listA)) continue;
                iteratorFormulaB = formulas.listIterator();
                while (iteratorFormulaB.hasNext()) {
                    listB = iteratorFormulaB.next();
                    if (removedLists.contains(listB)) continue;
                    formulaB = listB.get(0);
                    if (!formulaA.equals(formulaB)) {
                        if (formulaA.getLocation().intersect(formulaB.getLocation())) {
                            //are overlapping -> remove smaller formula
                            areaA = (int) (formulaA.getLocation().height() * formulaA.getLocation().width());
                            areaB = (int) (formulaB.getLocation().height() * formulaB.getLocation().width());
                            if (areaA <= areaB) {
                                if (!tmpList.contains(listA)) tmpList.add(listA);
                                removedLists.add(listB);
                            } else {
                                if (!tmpList.contains(listB)) tmpList.add(listB);
                                removedLists.add(listA);
                            }
                        } else {
                            if (!tmpList.contains(listA) && !removedLists.contains(listA))
                                tmpList.add(listA);
                        }
                    }
                }
            }
            formulas = tmpList;
        }

        //order objects to fitting formulas
        iterator = mappedRecognitions.listIterator();
        RectF locationFormula;
        List<Classifier.Recognition> currentFormula;
        while (iterator.hasNext()) {
            tmp = iterator.next();
            RectF tmpLoc = tmp.getLocation();
            if (!tmp.getTitle().equals("formula")) {
                for (int i = 0; i < formulas.size(); i++) {
                    currentFormula = formulas.get(i);
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
                                    if (x < 0.3) {
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
