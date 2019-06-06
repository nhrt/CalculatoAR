package com.example.swtp;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

//funktioniert nur mit einer Formel auf dem Bild. Vergleicht die linke Seite jeder Detection.

public class RecognitionParser {

    String parseRecognitions(List<Classifier.Recognition> mappedRecognitions){

        StringBuilder result = new StringBuilder();
        ListIterator<Classifier.Recognition> iterator = mappedRecognitions.listIterator();
        char c;

        sort(mappedRecognitions);
        while(iterator.hasNext()) {
            c = recognitionToChar(iterator.next());
            if(c != '~'){
                result.append(c);
            }
        }
        return result.toString();
    }

    private void sort(List<Classifier.Recognition> mappedRecognitions){
        Collections.sort(mappedRecognitions, new Comparator<Classifier.Recognition>() {
            @Override
            public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
                return (int)(o1.getLocation().left - o2.getLocation().left);
            }
        });
    }

    private char recognitionToChar(Classifier.Recognition recognition){
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
