package com.example.swtp;

import com.example.swtp.recognition.Parser;


import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.*;

public class ParserTest {


    List<Classifier.Recognition> formula = new ArrayList<>();
    Parser parser = new Parser();
    String[] labels = {"formula","zero","one","two","three","four","five","six","seven","eight","nine","plus","minus","mul","equals","div"};

    @Before
    public void initialize(){
        for(String label : labels){
            formula.add(new Classifier.Recognition("0",label,1f,null));
        }
    }
    @Test
    public void recognitionToChar_isCorrect(){
        assertEquals( '~',parser.recognitionToChar(formula.get(0)));
        assertEquals( '0',parser.recognitionToChar(formula.get(1)));
        assertEquals( '1',parser.recognitionToChar(formula.get(2)));
        assertEquals( '2',parser.recognitionToChar(formula.get(3)));
        assertEquals( '3',parser.recognitionToChar(formula.get(4)));
        assertEquals( '4',parser.recognitionToChar(formula.get(5)));
        assertEquals( '5',parser.recognitionToChar(formula.get(6)));
        assertEquals( '6',parser.recognitionToChar(formula.get(7)));
        assertEquals( '7',parser.recognitionToChar(formula.get(8)));
        assertEquals( '8',parser.recognitionToChar(formula.get(9)));
        assertEquals( '9',parser.recognitionToChar(formula.get(10)));
        assertEquals( '+',parser.recognitionToChar(formula.get(11)));
        assertEquals( '-',parser.recognitionToChar(formula.get(12)));
        assertEquals( '*',parser.recognitionToChar(formula.get(13)));
        assertEquals( '=',parser.recognitionToChar(formula.get(14)));
        assertEquals( '/',parser.recognitionToChar(formula.get(15)));
    }

    @Test
    public void recognitionsToString_isCorrect(){
        assertEquals("0123456789+-*=/",parser.formulaToString(formula));
    }

    @Test
    public void evaluation_isCorrect(){
        List<Classifier.Recognition> smallFormula = new ArrayList<>();
        smallFormula.add(formula.get(0));
        smallFormula.add(formula.get(2));
        smallFormula.add(formula.get(11));
        smallFormula.add(formula.get(3));
        smallFormula.add(formula.get(14));
        assertEquals("1+2=",parser.formulaToString(smallFormula));
        assertEquals(3.0,parser.parse(smallFormula),0.99);
        smallFormula = new ArrayList<>();
        smallFormula.add(formula.get(0));
        smallFormula.add(formula.get(2));
        smallFormula.add(formula.get(15));
        smallFormula.add(formula.get(3));
        smallFormula.add(formula.get(14));
        assertEquals("1/2=",parser.formulaToString(smallFormula));
        assertEquals(0.5,parser.parse(smallFormula),0.99);
        smallFormula = new ArrayList<>();
        smallFormula.add(formula.get(0));
        smallFormula.add(formula.get(2));
        smallFormula.add(formula.get(13));
        smallFormula.add(formula.get(3));
        smallFormula.add(formula.get(14));
        assertEquals("1*2=",parser.formulaToString(smallFormula));
        assertEquals(2.0,parser.parse(smallFormula),0.99);
        smallFormula = new ArrayList<>();
        smallFormula.add(formula.get(0));
        smallFormula.add(formula.get(2));
        smallFormula.add(formula.get(12));
        smallFormula.add(formula.get(3));
        smallFormula.add(formula.get(14));
        assertEquals("1-2=",parser.formulaToString(smallFormula));
        assertEquals(-1.0,parser.parse(smallFormula),0.99);
    }
}
