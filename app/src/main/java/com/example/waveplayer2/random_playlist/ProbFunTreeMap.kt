package com.example.waveplayer2.random_playlist

class ProbFunTreeMap<T : Comparable<T>>(
        choices: MutableSet<T>,
        maxPercent: Double
) : ProbFun<T>(choices, maxPercent, true)