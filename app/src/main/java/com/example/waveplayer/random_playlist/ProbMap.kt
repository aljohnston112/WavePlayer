package com.example.waveplayer.random_playlist

import java.io.Serializable
import java.util.*

class ProbMap<T> : Serializable {
    private val probHashMap: HashMap<T?, Double?>? = HashMap()
    fun put(t: T?, aDouble: Double?) {
        probHashMap[t] = aDouble
    }

    fun outcome(t: T?, random: Random?): Boolean {
        val prob: Double = probHashMap.get(t)
        val randomChoice = random.nextDouble()
        return randomChoice <= prob
    }

    fun bad(t: T?, percent: Double): Boolean {
        require(!(percent >= 1.0 || percent <= 0.0)) { "percent passed to bad() is not between 0.0 and 1.0 (exclusive)" }
        val dub = probHashMap.get(t) ?: return false
        var globalBad = true
        for (d in probHashMap.values) {
            if (dub <= d) {
                globalBad = false
                break
            }
        }
        val sub = dub * percent
        probHashMap[t] = dub - sub
        return globalBad
    }

    fun good(t: T?, percent: Double): Boolean {
        require(!(percent >= 1.0 || percent <= 0.0)) { "percent passed to good() is not between 0.0 and 1.0 (exclusive)" }
        val dub = probHashMap.get(t) ?: return false
        var globalGood = true
        for (d in probHashMap.values) {
            if (dub >= d) {
                globalGood = false
                break
            }
        }
        val add = dub * percent
        probHashMap[t] = Math.min(dub + add, 1.0)
        return globalGood
    }

    fun clearProbabilities() {
        for (t in probHashMap.keys) {
            probHashMap[t] = 1.0
        }
    }
}