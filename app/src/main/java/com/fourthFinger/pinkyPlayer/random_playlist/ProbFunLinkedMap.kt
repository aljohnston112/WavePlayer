package com.fourthFinger.pinkyPlayer.random_playlist

class ProbFunLinkedMap<T : Comparable<T>>(
        choices: MutableSet<T>,
) : ProbFun<T>(choices, false)