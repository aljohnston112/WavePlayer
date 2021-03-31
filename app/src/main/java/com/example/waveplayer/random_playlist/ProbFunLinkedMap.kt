package com.example.waveplayer.random_playlist

class ProbFunLinkedMap<T : Comparable<T>>(choices: MutableSet<T>, maxPercent: Double) :
        ProbFun<T>(choices, maxPercent, false)