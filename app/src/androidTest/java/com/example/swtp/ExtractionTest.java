package com.example.swtp;

import android.graphics.RectF;
import android.support.test.runner.AndroidJUnit4;

import com.example.swtp.recognition.FormulaExtractor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
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
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,50f,50f)));
        mappedRecognitions.add(new Classifier.Recognition("1","formula", 1f,new RectF(51f,51f,100f,100f)));
        mappedRecognitions.add(new Classifier.Recognition("2","formula", 1f,new RectF(101f,101f,150f,150f)));
        mappedRecognitions.add(new Classifier.Recognition("3","formula", 1f,new RectF(151f,151f,200f,200f)));
        assertEquals(4,formulaExtractor.extract(mappedRecognitions).size());
    }

    @Test
    public void categorisation_isCorrect(){
        mappedRecognitions.add(new Classifier.Recognition("1","formula", 1f,new RectF(0f,0f,500f,500f)));
        mappedRecognitions.add(new Classifier.Recognition("2","zero", 1f,new RectF(50f,50f,100f,100f)));
        assertEquals(2,formulaExtractor.extract(mappedRecognitions).get(0).size());
        mappedRecognitions.add(new Classifier.Recognition("3","zero", 1f,new RectF(501,501,600,600)));
        assertEquals(2,formulaExtractor.extract(mappedRecognitions).get(0).size());
        mappedRecognitions.add(new Classifier.Recognition("4","one", 1f,new RectF(151,151,200,200)));
        List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(3,formulas.get(0).size());
        assertEquals("2",formulas.get(0).get(1).getId());
        assertEquals("4",formulas.get(0).get(2).getId());
    }
    @Test
    public void formula_isSorted(){
        mappedRecognitions.add(new Classifier.Recognition("4","one", 1f,new RectF(151,151,200,200)));
        mappedRecognitions.add(new Classifier.Recognition("2","zero", 1f,new RectF(50f,50f,100f,100f)));
        mappedRecognitions.add(new Classifier.Recognition("3","one", 1f,new RectF(101,101,150,150)));
        mappedRecognitions.add(new Classifier.Recognition("1","formula", 1f,new RectF(0f,0f,500f,500f)));

        List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(4,formulas.get(0).size());
        assertEquals("1", formulas.get(0).get(0).getId());
        assertEquals("2", formulas.get(0).get(1).getId());
        assertEquals("3", formulas.get(0).get(2).getId());
        assertEquals("4", formulas.get(0).get(3).getId());
    }

    @Test
    public void formulaRemoving_isCorrect(){
        mappedRecognitions.add(new Classifier.Recognition("0","formula", 1f,new RectF(0f,0f,150f,150f)));
        mappedRecognitions.add(new Classifier.Recognition("1","formula", 1f,new RectF(51f,51f,100f,100f)));
        mappedRecognitions.add(new Classifier.Recognition("2","formula", 1f,new RectF(151f,151f,200f,200f)));
        List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(mappedRecognitions);

        assertEquals(2,formulas.size());
        assertNotEquals("1",formulas.get(0).get(0).getId());
        assertNotEquals("1",formulas.get(1).get(0).getId());
    }

    @Test
    public void detectionRemoving_isCorrect(){
        mappedRecognitions.add(new Classifier.Recognition("1","formula", 1f,new RectF(0f,0f,500f,500f)));
        mappedRecognitions.add(new Classifier.Recognition("2","zero", 1f,new RectF(50f,50f,100f,100f)));
        mappedRecognitions.add(new Classifier.Recognition("4","one", 1f,new RectF(151,151,200,200)));

        //80% overlapping
        mappedRecognitions.add(new Classifier.Recognition("5","one", 1f,new RectF(141,151,191,200)));

        List<List<Classifier.Recognition>> formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(3,formulas.get(0).size());
        assertEquals("2",formulas.get(0).get(1).getId());
        assertEquals("4",formulas.get(0).get(2).getId());

        //60% overlapping
        mappedRecognitions.add(new Classifier.Recognition("6","one", 1f,new RectF(131,151,171,200)));
        formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(3,formulas.get(0).size());
        assertEquals("2",formulas.get(0).get(1).getId());
        assertEquals("4",formulas.get(0).get(2).getId());

        //40% overlapping
        mappedRecognitions.add(new Classifier.Recognition("7","one", 1f,new RectF(121,151,161,200)));
        formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(3,formulas.get(0).size());
        assertEquals("2",formulas.get(0).get(1).getId());
        assertEquals("4",formulas.get(0).get(2).getId());

        //20% overlapping
        mappedRecognitions.add(new Classifier.Recognition("8","one", 1f,new RectF(111,151,151,200)));
        formulas = formulaExtractor.extract(mappedRecognitions);
        assertEquals(4,formulas.get(0).size());
        assertEquals("2",formulas.get(0).get(1).getId());
        assertEquals("8",formulas.get(0).get(2).getId());
        assertEquals("4",formulas.get(0).get(3).getId());


    }
}
