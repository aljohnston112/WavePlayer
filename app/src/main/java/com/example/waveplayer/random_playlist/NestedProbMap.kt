package com.example.waveplayer.random_playlist

import java.io.Serializable
import java.util.*

class NestedProbMap : Serializable {
    // The ProbFun used to determine if a song should play during a specific hour
    private val probabilityFunctionHours: ProbMap<Int?>? = ProbMap()

    // The ProbFun used to determine if a song should play during a specific day
    private val probabilityFunctionDays: ProbMap<Int?>? = ProbMap()

    // The ProbFun used to determine if a song should play during a specific month
    private val probabilityFunctionMonths: ProbMap<Int?>? = ProbMap()
    fun outcome(random: Random?): Boolean {
        val date = Calendar.getInstance()
        return probabilityFunctionHours.outcome(date[Calendar.HOUR_OF_DAY], random) &&
                probabilityFunctionDays.outcome(date[Calendar.DAY_OF_WEEK] - 1, random) &&
                probabilityFunctionMonths.outcome(date[Calendar.MONTH], random)
    }

    fun bad(percent: Double): Boolean {
        val date = Calendar.getInstance()
        return probabilityFunctionHours.bad(date[Calendar.HOUR_OF_DAY], percent) &&
                probabilityFunctionDays.bad(date[Calendar.DAY_OF_WEEK] - 1, percent) &&
                probabilityFunctionMonths.bad(date[Calendar.MONTH], percent)
    }

    fun good(percent: Double): Boolean {
        val date = Calendar.getInstance()
        return probabilityFunctionHours.good(date[Calendar.HOUR_OF_DAY], percent) &&
                probabilityFunctionDays.good(date[Calendar.DAY_OF_WEEK] - 1, percent) &&
                probabilityFunctionMonths.good(date[Calendar.MONTH], percent)
    }

    fun clearProbabilities() {
        probabilityFunctionHours.clearProbabilities()
        probabilityFunctionDays.clearProbabilities()
        probabilityFunctionMonths.clearProbabilities()
    }

    init {
        for (i in 0..23) {
            probabilityFunctionHours.put(i, 1.0)
        }
        for (i in 0..6) {
            probabilityFunctionDays.put(i, 1.0)
        }
        for (i in 0..11) {
            probabilityFunctionMonths.put(i, 1.0)
        }
    }
}