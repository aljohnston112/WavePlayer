package com.example.waveplayer.random_playlist;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

public class ProbMap<T> implements Serializable {

    private final HashMap<T, Double> probHashMap = new HashMap<>();

    public void put(T t, Double aDouble) {
        probHashMap.put(t, aDouble);
    }

    public boolean outcome(T t, Random random) {
        double prob = probHashMap.get(t);
        double randomChoice = random.nextDouble();
        return !(randomChoice > prob);
    }

    public boolean bad(T t, double percent) {
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException(
                    "percent passed to bad() is not between 0.0 and 1.0 (exclusive)");
        }
        Double dub = probHashMap.get(t);
        if(dub == null){
            return false;
        }
        double prob = dub;
        boolean globalBad = true;
        for (Double d : probHashMap.values()) {
            if (prob <= d) {
                globalBad = false;
                break;
            }
        }
        double sub = (prob * percent);
            probHashMap.put(t, (prob - sub));
        return globalBad;
    }

    public boolean good(T t, double percent) {
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException(
                    "percent passed to good() is not between 0.0 and 1.0 (exclusive)");
        }
        Double dub = probHashMap.get(t);
        if(dub == null){
            return false;
        }
        double prob = dub;
        boolean globalGood = true;
        for (Double d : probHashMap.values()) {
            if (prob >= d) {
                globalGood = false;
                break;
            }
        }
        double add = (prob * percent);
        probHashMap.put(t, Math.min(prob + add, 1.0));
        return globalGood;
    }

    public void clearProbabilities(){
        for(T t : probHashMap.keySet()){
            probHashMap.put(t, 1.0);
        }
    }

}