package com.example.waveplayer.random_playlist

class ProbFunTreeMap<T : Comparable<T>>(choices: MutableSet<T>, maxPercent: Double) :
        ProbFun<T>(choices, maxPercent, true)