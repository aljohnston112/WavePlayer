package com.example.waveplayer;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class NestedProbMap {

    // The ProbFun used to determine if a song should play during a specific hour
    private final ProbMap<Integer> probabilityFunctionHours = new ProbMap<>();

    // The ProbFun used to determine if a song should play during a specific day
    private final ProbMap<Integer> probabilityFunctionDays = new ProbMap<>();

    // The ProbFun used to determine if a song should play during a specific month
    private final ProbMap<Integer> probabilityFunctionMonths = new ProbMap<>();

    public NestedProbMap() {
        for (int i = 0; i < 24; i++) {
            probabilityFunctionHours.put(i, 1.0);
        }
        for (int i = 0; i < 7; i++) {
            probabilityFunctionDays.put(i, 1.0);
        }
        for (int i = 0; i < 12; i++) {
            probabilityFunctionMonths.put(i, 1.0);
        }
    }

    public boolean outcome(Random random) {
        Calendar date = Calendar.getInstance();
        if (probabilityFunctionHours.outcome(date.get(Calendar.HOUR_OF_DAY), random) &&
                probabilityFunctionDays.outcome(date.get(Calendar.DAY_OF_WEEK) - 1, random) &&
                probabilityFunctionMonths.outcome(date.get(Calendar.MONTH), random)) {
            return true;
        }
        return false;
    }

    public boolean bad(double percent) {
        Calendar date = Calendar.getInstance();
        return probabilityFunctionHours.bad(date.get(Calendar.HOUR_OF_DAY), percent) &&
                probabilityFunctionDays.bad(date.get(Calendar.DAY_OF_WEEK) - 1, percent) &&
                probabilityFunctionMonths.bad(date.get(Calendar.MONTH), percent);
    }

    public boolean good(double percent) {
        Calendar date = Calendar.getInstance();
        return probabilityFunctionHours.good(date.get(Calendar.HOUR_OF_DAY), percent) &&
                probabilityFunctionDays.good(date.get(Calendar.DAY_OF_WEEK) - 1, percent) &&
                probabilityFunctionMonths.good(date.get(Calendar.MONTH), percent);
    }

    public void clearProbabilities() {
        probabilityFunctionHours.clearProbabilities();
        probabilityFunctionDays.clearProbabilities();
        probabilityFunctionMonths.clearProbabilities();
    }

}
