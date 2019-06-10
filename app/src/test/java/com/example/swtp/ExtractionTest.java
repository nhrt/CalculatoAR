package com.example.swtp;

import android.graphics.RectF;

import com.example.swtp.recognition.FormulaExtractor;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;


public class ExtractionTest {
    FormulaExtractor formulaExtractor;
    List<Classifier.Recognition> mappedRecognitions;


    @Before
    public void initialize(){
        formulaExtractor = new FormulaExtractor();
        mappedRecognitions = new LinkedList<>();
    }

    @Test
    public void numberFormula_isCorrect(){
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,500f,500f)));
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,500f,500f)));
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,500f,500f)));
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,500f,500f)));
        assertEquals(4,formulaExtractor.extract(mappedRecognitions).size());
    }
}
