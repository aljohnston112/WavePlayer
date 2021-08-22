package com.fourthFinger.pinkyPlayer.random_playlist

class ProbFunTreeMap<T : Comparable<T>>(
        choices: MutableSet<T>
) : ProbFun<T>(choices, true)