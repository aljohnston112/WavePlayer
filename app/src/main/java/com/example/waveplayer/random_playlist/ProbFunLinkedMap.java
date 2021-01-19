package com.example.waveplayer.random_playlist;

import java.util.Set;

public class ProbFunLinkedMap<T extends Comparable<T>> extends ProbFun<T> {

    public ProbFunLinkedMap(Set<T> choices, double maxPercent) {
        super(choices, maxPercent, false);
    }

}
