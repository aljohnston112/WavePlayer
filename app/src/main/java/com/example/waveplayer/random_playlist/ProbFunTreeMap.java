package com.example.waveplayer.random_playlist;

import java.util.Set;

public class ProbFunTreeMap<T extends Comparable<T>> extends ProbFun<T> {

    public ProbFunTreeMap(Set<T> choices, double maxPercent) {
        super(choices, maxPercent, true);
    }

}
