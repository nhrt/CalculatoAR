package com.example.swtp.recognition;

import com.example.swtp.Classifier;

import java.util.List;
import java.util.ListIterator;

public class Parser {

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
                break;
            default:
                break;
        }
        return '~';
    }
}
