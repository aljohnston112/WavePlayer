package com.example.waveplayer;

import java.util.HashMap;
import java.util.Random;

public class ProbMap<T> {

    HashMap<T, Double> probHashMap = new HashMap<>();

    void put(T t, Double aDouble) {
        probHashMap.put(t, aDouble);
    }

    boolean outcome(T t, Random random) {
        double prob = probHashMap.get(t);
        double randomChoice = random.nextDouble();
        if (randomChoice > prob) {
            return false;
        }
        return true;
    }

    public boolean bad(T t, double percent) {
        Double dub = probHashMap.get(t);
        if(dub == null){
            return false;
        }
        double prob = dub;
        boolean globalBad = true;
        for (Double d : probHashMap.values()) {
            if (prob < d) {
                globalBad = false;
            }
        }
        double sub = (prob * percent);
        if (prob - sub > 0) {
            probHashMap.put(t, (prob - sub));
        }
        return globalBad;
    }

    public boolean good(T t, double percent) {
        Double dub = probHashMap.get(t);
        if(dub == null){
            return false;
        }
        double prob = dub;
        boolean globalGood = true;
        for (Double d : probHashMap.values()) {
            if (prob > d) {
                globalGood = false;
            }
        }
        double add = (prob * percent);
        if (prob + add <= 1.0) {
            probHashMap.put(t, (prob + add));
        } else {
            probHashMap.put(t, 1.0);
        }
        return globalGood;
    }

    public void clearProbabilities(){
        for(T t : probHashMap.keySet()){
            probHashMap.put(t, 1.0);
        }
    }

}