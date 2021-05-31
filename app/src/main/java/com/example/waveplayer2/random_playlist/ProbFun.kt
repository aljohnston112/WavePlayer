package com.example.waveplayer2.random_playlist

import java.io.Serializable
import java.util.*

/**
 * A tree node where a set of elements are picked from a probability function to decide which child node
 * will produce the next element when fun() is called.
 *
 * @param <T> The type of the elements that make up the elements in the probability function.
 * @author Alexander Johnston
 * @since Copyright 2020
</T> */
open class ProbFun<T : Comparable<T>> : Serializable {

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    private lateinit var probabilityMap: MutableMap<T, Double>
    fun getKeys(): MutableList<T> {
        return ArrayList(probabilityMap.keys)
    }

    // The unique id of this ProbFunTree
    private var id = 0
    private var roundingError = 0.0
    private var maxPercent: Double
    fun setMaxPercent(maxPercent: Double) {
        this.maxPercent = maxPercent
    }

    protected constructor(choices: MutableSet<T>, maxPercent: Double, comparable: Boolean) {
        Objects.requireNonNull(choices)
        require(choices.size >= 1) { "Must have at least 1 element in the choices passed to the ProbFunTree constructor\n" }
        require(!(maxPercent <= 0 || maxPercent > 1.0)) { "maxPercent passed into the ProbFunTree constructor must be above 0 and 1.0 or under" }
        // Invariants secured
        if (comparable) {
            probabilityMap = TreeMap()
        } else {
            probabilityMap = LinkedHashMap()
        }
        this.maxPercent = maxPercent
        for (choice in choices) {
            probabilityMap[choice] = 1.0 / choices.size
        }
        fixProbSum()
    }

    /**
     * Scales the probabilities so they add up to 1.0.
     */
    private fun scaleProbs() {
        val scale = 1.0 / probSum()
        val probabilities: MutableSet<MutableMap.MutableEntry<T, Double>> = probabilityMap.entries
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        fixProbSum()
    }

    /**
     * @return the sum of all the probabilities in order to fix rounding error.
     */
    private fun probSum(): Double {
        val probabilities = probabilityMap.values
        var sum = 0.0
        for (d in probabilities) {
            sum += d
        }
        return sum
    }

    /**
     * Fixes rounding error in the probabilities by adding up the probabilities
     * and changing the first probability so all probabilities add up to 1.0.
     */
    private fun fixProbSum() {
        var firstProb = probabilityMap.entries.iterator().next()
        roundingError = 1.0 - probSum()
        while (firstProb.value * 2.0 < roundingError) {
            var p: Double
            for (e in probabilityMap.entries) {
                p = e.value
                p += roundingError / probabilityMap.size
                e.setValue(p)
            }
            firstProb = probabilityMap.entries.iterator().next()
            roundingError = 1.0 - probSum()
        }
        firstProb.setValue(firstProb.value + roundingError)
    }

    /**
     * Sets the probabilities to there being an equal chance of getting any element from this ProbFunTree.
     */
    fun clearProbabilities() {
        for (e in probabilityMap.entries) {
            e.setValue(1.0)
        }
        scaleProbs()
    }

    /**
     * Lowers the probabilities so there is about at most a low chance of getting any element from this ProbFunTree.
     *
     * @param low as the lowest chance of an object being returned when fun() is called.
     */
    fun lowerProbs(low: Double) {
        // TODO address higher probs and if prob is unreasonably low (1.0/n).
        val probs: MutableCollection<T> = probabilityMap.keys
        for (t in probs) {
            while (probabilityMap[t]!! > low) {
                probabilityMap[t] = low
            }
            scaleProbs()
        }
    }

    fun getProbability(t: T): Double {
        return probabilityMap[t] ?: 0.0
    }

    /**
     * Adds element to this ProbFunTree, making the probability equal to 1.0/n
     * where n is the number of elements contained in this ProbFunTree,
     * and appends elements as a child ProbFunTree, where elements are the choices.
     *
     * @param element as the element to add to this ProbFunTree.
     * @throws NullPointerException if element is null.
     */
    fun add(element: T) {
        Objects.requireNonNull(element)
        // Invariants secured
        val probability = 1.0 / probabilityMap.size
        if (!probabilityMap.containsKey(element)) {
            probabilityMap[element] = probability
        } else {
            return
        }
        scaleProbs()
    }

    /**
     * Adds an element to this ProbFunTree with the specified probability.
     * If the element exists in this ProbFunTree then it's probability will be overwritten with percent.
     *
     * @param element as the element to add to this ProbFunTree.
     * @param percent between 0 and 1 exclusive, as the chance of this ProbFunTree returning element.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if percent is not between 0.0 and 1.0 (exclusive).
     */
    fun add(element: T, percent: Double) {
        Objects.requireNonNull(element)
        require(!(percent >= 1.0 || percent <= 0.0)) { "percent passed to add() is not between 0.0 and 1.0 (exclusive)" }
        // Invariants secured
        val scale = 1.0 - percent
        val probabilities: MutableSet<MutableMap.MutableEntry<T, Double>> = probabilityMap.entries
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        probabilityMap[element] = percent
        scaleProbs()
    }

    /**
     * Removes an element from this ProbFunTree unless there is only one element.
     *
     * @param element as the element to remove from this ProbFunTree.
     * @return True if this ProbFunTree's parent contained the element and it was removed, else false.
     * @throws NullPointerException if element is null.
     */
    fun remove(element: T): Boolean {
        if (size() == 1) {
            return false
        }
        // Invariants secured
        if (probabilityMap.remove(element) == null) {
            return false
        }
        scaleProbs()
        return true
    }

    operator fun contains(t: T): Boolean {
        return probabilityMap.containsKey(t)
    }

    /**
     * Adjust the probability to make element more likely to be returned when fun() is called from this ProbFunTree.
     *
     * @param element as the element to make appear more often
     * @param percent as the percentage between 0 and 1 (exclusive),
     * of the probability of getting element to add to the probability.
     * @param scale   as whether or not to scale the percent down
     * to avoid hitting a ceiling for the probability.
     * @return the adjusted probability.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    fun good(element: T, percent: Double, scale: Boolean): Double {
        var percent = percent
        require(!(percent >= 1.0 || percent <= 0.0)) { "percent passed to good() is not between 0.0 and 1.0 (exclusive)" }
        // Invariants secured
        val oldProb = probabilityMap[element] ?: return -1.0
        var add = probToAddForGood(oldProb, percent)
        if (scale) {
            while (oldProb + add >= maxPercent - roundingError) {
                percent *= percent
                add = probToAddForGood(oldProb, percent)
            }
        } else if (oldProb + add >= maxPercent - roundingError) {
            return oldProb
        }
        val goodProbability = oldProb + add
        probabilityMap[element] = goodProbability
        val leftover = 1.0 - goodProbability
        val sumOfLeftovers = probSum() - goodProbability
        val leftoverScale = leftover / sumOfLeftovers
        for (e in probabilityMap.entries) {
            e.setValue(e.value * leftoverScale)
        }
        probabilityMap[element] = goodProbability
        fixProbSum()
        return probabilityMap.get(element)?: -1.0
    }

    private fun probToAddForGood(oldProb: Double, percent: Double): Double {
        return if (oldProb > 0.5) {
            (1.0 - oldProb) * percent
        } else {
            oldProb * percent
        }
    }

    /**
     * Adjust the probability to make element less likely to be returned when fun() is called from this ProbFunTree.
     *
     * @param element as the element to make appear less often
     * @param percent as the percentage between 0 and 1 (exclusive),
     * of the probability of getting element to subtract from the probability.
     * @return the adjusted probability or -1 if this ProbFunTree didn't contain the element.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    fun bad(element: T, percent: Double): Double {
        Objects.requireNonNull(element)
        require(!(percent >= 1.0 || percent <= 0.0)) { "percent passed to good() is not between 0.0 and 1.0 (exclusive)" }
        // Invariants secured
        val oldProb = probabilityMap[element] ?: return -1.0
        val sub = oldProb * percent
        if (oldProb - sub <= roundingError) return oldProb
        val badProbability = oldProb - sub
        probabilityMap[element] = badProbability
        val leftover = 1.0 - badProbability
        val sumOfLeftovers = probSum() - badProbability
        val leftoverScale = leftover / sumOfLeftovers
        for (e in probabilityMap.entries) {
            e.setValue(e.value * leftoverScale)
        }
        probabilityMap[element] = badProbability
        fixProbSum()
        return probabilityMap[element] ?: -1.0
    }

    /**
     * Returns a randomly picked element from this ProbFunTree, based on the previously returned elements.
     *
     * @return a randomly picked element from this ProbFunTree.
     * Any changes in the element will be reflected in this ProbFunTree.
     */
    fun `fun`(random: Random): T {
        return nextValue(random)
    }

    /**
     * For generating the next value.
     *
     * @return the next generated value.
     */
    private fun nextValue(random: Random): T {
        val randomChoice = random.nextDouble()
        var sumOfProbabilities = 0.0
        val entries: MutableIterator<MutableMap.MutableEntry<T, Double>> = probabilityMap.entries.iterator()
        lateinit var element: T
        var e: MutableMap.MutableEntry<T, Double>
        while (randomChoice > sumOfProbabilities) {
            e = entries.next()
            element = e.key
            sumOfProbabilities += e.value
        }
        return element
    }

    /**
     * Returns the number of elements in this ProbFunTree.
     *
     * @return the number of elements in this ProbFunTree.
     */
    fun size(): Int {
        return probabilityMap.size
    }

    /**
     * Private copy constructor for clone
     *
     * @param probFun as the ProbFunTree to copy
     */
    private constructor(probFun: ProbFun<T>) {
        for ((key, value) in probFun.probabilityMap) {
            probabilityMap[key] = value
        }
        maxPercent = probFun.maxPercent
        roundingError = probFun.roundingError
        id = 0
    }

    fun clone(): ProbFun<T> {
        return ProbFun(this)
    }

    override fun toString(): String {
        if (id == 0) {
            id = System.identityHashCode(this)
        }
        val sb = StringBuilder()
        sb.append("PF ")
        sb.append(id)
        sb.append(": [")
        for ((key, value) in probabilityMap) {
            sb.append("[")
            sb.append(key)
            sb.append(" = ")
            sb.append(value * 100.0)
            sb.append("%],")
        }
        sb.append("\n")
        return sb.toString()
    }

    override fun hashCode(): Int {
        if (id == 0) {
            id = System.identityHashCode(this)
        }
        return id
    }

    fun swapPositions(oldPosition: Int, newPosition: Int) {
        val oldMap = probabilityMap
        val keySetList = ArrayList(oldMap.keys)
        Collections.swap(keySetList, oldPosition, newPosition)
        val swappedMap = LinkedHashMap<T, Double>()
        for (oldSwappedKey in keySetList) {
            swappedMap[oldSwappedKey] = oldMap[oldSwappedKey]!!
        }
        probabilityMap = swappedMap
    }

    fun switchPositions(oldPosition: Int, newPosition: Int) {
        val oldMap = probabilityMap
        val keySetList = ArrayList(oldMap.keys)
        keySetList.add(newPosition, keySetList[oldPosition])
        keySetList.removeAt(oldPosition + 1)
        val swappedMap = LinkedHashMap<T, Double>()
        for (oldSwappedKey in keySetList) {
            swappedMap[oldSwappedKey] = oldMap[oldSwappedKey]!!
        }
        probabilityMap = swappedMap
    }

    companion object {
        private const val serialVersionUID = -6556634307811294014L
    }
}