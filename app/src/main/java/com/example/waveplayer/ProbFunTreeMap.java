package com.example.waveplayer;

import java.util.Set;
import java.util.TreeMap;

public class ProbFunTreeMap<T extends Comparable<T>> extends ProbFun<T> {

    public ProbFunTreeMap(Set<T> choices, double maxPercent) {
        super(choices, maxPercent, true);
    }

}
