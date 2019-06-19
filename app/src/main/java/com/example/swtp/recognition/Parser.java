package com.example.swtp.recognition;

import com.example.swtp.Classifier;

import org.mariuszgromada.math.mxparser.Expression;

import java.util.List;
import java.util.ListIterator;

public class Parser {

    /*
        method parse takes a ordered list of Recognitions.
        1. correction of recoginitions with correctForumla
        2. translation from corrected formula to a string
        3. remove the tailing "equals" (necessary for mxparser
        4. evaluate the formula as an expression and return the double value
     */
    public double parse(List<Classifier.Recognition> mappedRecognitions){
        String string = formulaToString(correctFormula(mappedRecognitions));
        StringBuilder formula = new StringBuilder();
        if(string.endsWith("=") || string.endsWith("-")){
            formula.append(string.substring(0,string.length() - 1));
        }else{
            formula.append(string);
        }

        Expression expression = new Expression(formula.toString());
        return expression.calculate();
    }


    /*
        Method correctFormula changes tailing "minus" to a tailing "equals"
     */
    public List<Classifier.Recognition> correctFormula(List<Classifier.Recognition> mappedRecognitions){
        Classifier.Recognition tmp, last;

        last = mappedRecognitions.get(mappedRecognitions.size() - 1);
        if(last.getTitle() == "minus"){
            mappedRecognitions.remove(last);
            tmp = new Classifier.Recognition(last.getId(),"equals",last.getConfidence(), last.getLocation());
            mappedRecognitions.add(tmp);
        }

        return mappedRecognitions;
    }

    /*
        method formulaToString takes a List of Recognitions and translates it into a String
     */
    public String formulaToString(List<Classifier.Recognition> mappedRecognitions){
        ListIterator<Classifier.Recognition> iterator = mappedRecognitions.listIterator();
        char c;
        StringBuilder result = new StringBuilder();
        while(iterator.hasNext()) {

            c = recognitionToChar(iterator.next());
            if(c != '~'){
                result.append(c);
            }
        }
        return result.toString();
    }

    /*
        method recognitionToChar takes a recognition and finds a fitting character
        "formula" gets translated to '~'
        default return value is a '~'

     */
    public char recognitionToChar(Classifier.Recognition recognition){
        switch (recognition.getTitle()){
            case "zero":
                return '0';
            case "one":
                return '1';
            case "two":
                return '2';
            case "three":
                return '3';
            case "four":
                return '4';
            case "five":
                return '5';
            case "six":
                return '6';
            case "seven":
                return '7';
            case "eight":
                return '8';
            case "nine":
                return '9';
            case "plus":
                return '+';
            case "minus":
                return '-';
            case "mul":
                return '*';
            case "equals":
                return '=';
            case "div":
                return '/';
            case "formula":
                return '~';
            default:
                return '~';
        }
    }
}
