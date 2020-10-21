package com.example.waveplayer;

import java.util.Set;

public class ProbFunLinkedMap<T extends Comparable<T>> extends ProbFun<T> {

    public ProbFunLinkedMap(Set<T> choices, double maxPercent) {
        super(choices, maxPercent, false);
    }

}
