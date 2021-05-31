package com.example.waveplayer2.random_playlist

class ProbFunLinkedMap<T : Comparable<T>>(
        choices: MutableSet<T>,
        maxPercent: Double
) : ProbFun<T>(choices, maxPercent, false)