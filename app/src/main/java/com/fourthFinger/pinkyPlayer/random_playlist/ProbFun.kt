package com.fourthFinger.pinkyPlayer.random_playlist

import java.io.Serializable
import java.util.*

const val MIN_VALUE = 0.0000000000000005

/**
 * A tree node where a set of elements are picked from a probability function to decide which child node
 * will produce the next element when fun() is called.
 *
 * @param <T> The type of the elements that make up the elements in the probability function.
 * @author Alexander Johnston
 * @since Copyright 2020
</T> */
// TODO fix max percent like the constructor
sealed class ProbFun<T>(
    choices: Set<T>,
    maxPercent: Double,
    comparable: Boolean
) : Serializable, Cloneable {

    class ProbFunLinkedMap<T>(
        choices: Set<T>, maxPercent: Double
    ) : ProbFun<T>(choices, maxPercent, false){
        fun swapTwoPositions(oldPosition: Int, newPosition: Int) {
            val oldMap = probabilityMap
            val keys = ArrayList(oldMap.keys)
            Collections.swap(keys, oldPosition, newPosition)
            val swappedMap = LinkedHashMap<T, Double>()
            for (key in keys) {
                swappedMap[key] = oldMap[key] ?: error("Problem swapping elements")
            }
            probabilityMap = swappedMap
        }

        fun switchOnesPosition(oldPosition: Int, newPosition: Int) {
            val oldMap = probabilityMap
            val keys = ArrayList(oldMap.keys)
            val d = keys[oldPosition]
            keys.removeAt(oldPosition)
            keys.add(newPosition, d)
            val switchedMap = LinkedHashMap<T, Double>()
            for (key in keys) {
                switchedMap[key] = oldMap[key]!!
            }
            probabilityMap = switchedMap
        }
    }

    class ProbFunTreeMap<T : Comparable<T>>(
        choices: Set<T>, maxPercent: Double
    ) : ProbFun<T>(choices, maxPercent, true)

    private val settingsRepo = SettingsRepo.getInstance()

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    protected var probabilityMap: MutableMap<T, Double>

    private var roundingError = 0.0

    private var _maxPercent = -1.0
    fun maxPercent() = _maxPercent
    fun setMaxPercent(maxPercent: Double) {
        require((maxPercent > (0.0) && maxPercent <= 1.0)) {
            "maxPercent passed into the ProbFunTree constructor must be above 0 and under 1.0" +
                    "value was $maxPercent"
        }
        _maxPercent = if (maxPercent == 1.0) {
            (1.0 - (size().toDouble() * MIN_VALUE))
        } else if (maxPercent < 1.0 / size().toDouble()) {
            1.0 / size().toDouble()
        } else {
            maxPercent
        }
    }

    private val id by lazy { hashCode() }

    init {
        Objects.requireNonNull(choices)
        require(choices.isNotEmpty()) {
            "Must have at least 1 element in the choices passed to the ProbFunTree constructor\n"
        }
        require(choices.size < 2000000000000000) {
            "ProbFun will not work with a size greater than 2,000,000,000,000,000"
        }
        probabilityMap = if (comparable) TreeMap() else LinkedHashMap()
        for (choice in choices) {
            probabilityMap[choice] = 1.0 / choices.size
        }
        fixProbSum()
        setMaxPercent(maxPercent)
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
        if (!probabilityMap.containsKey(element)) {
            val probability = 1.0 / probabilityMap.size
            probabilityMap[element] = probability
            scaleProbs()
        }
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
        require(percent in 0.0..1.0) {
            "percent passed to add() is not between 0.0 and 1.0 (exclusive)"
        }
        val realPercent: Double = if (percent < MIN_VALUE) {
            (1.0 / size().toDouble())
        } else if (percent > (1.0 - ((size().toDouble() + 1.0) * MIN_VALUE))) {
            1.0 - ((size().toDouble() + 1.0) * MIN_VALUE)
        } else {
            percent
        }
        val scale = 1.0 - realPercent
        val probabilities = probabilityMap.entries.toList()
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        probabilityMap[element] = realPercent
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
        Objects.requireNonNull(element)
        if (size() == 1) {
            return false
        }
        if (probabilityMap.remove(element) == null) {
            return false
        }
        scaleProbs()
        fixMaxPercent()
        fixProbSum()
        return true
    }

    /**
     * Removes elements with the lowest probability of occurring when [next] is called.
     * If elements have the same maximum probability of occurring, no elements will be removed.
     * If after a removal,
     * elements have the same maximum probability of occurring,
     * no more elements will be removed.
     * If [size] == 1, no elements will be removed.
     * If [size] == 1 after a removal, no more elements will be removed.
     */
    fun prune() {
        // TODO fix MaxPercent for prune
        if (size() == 1) {
            return
        }
        val min = probabilityMap.values.minOrNull()
        val max = probabilityMap.values.maxOrNull()
        if (max == min || max == null || min == null) {
            return
        }
        val probabilities = probabilityMap.entries
        val it = probabilities.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.value <= min && e.value < (max - roundingError)) {
                it.remove()
                if (size() == 1) {
                    scaleProbs()
                    return
                }
            }
        }
        scaleProbs()
    }

    /**
     * Removes elements with lower than [percent] probability of occurring when [next] is called.
     * If elements have the same maximum probability of occurring, no elements will be removed.
     * If after a removal,
     * elements have the same maximum probability of occurring,
     * no more elements will be removed.
     * If parentSize() == 1, no elements will be removed.
     * If parentSize() == 1 after a removal, no more elements will be removed.
     * @param  percent as the upper limit, inclusive, of the probability of elements being returned to be removed from this ProbFunTree.
     * @throws IllegalArgumentException if percent is not between 0.0 and 1.0 (exclusive)
     */
    fun prune(percent: Double) {
        // TODO fix MaxPercent for prune
        if (percent >= 1.0 || percent <= 0.0) {
            throw IllegalArgumentException("percent passed to prune() is not between 0.0 and 1.0 (exclusive)")
        }
        val max = probabilityMap.values.maxOrNull()
        val min = probabilityMap.values.minOrNull()
        if (size() == 1 || max == null || min == null || (max <= percent && min == max)) {
            return
        }
        val probabilities = probabilityMap.entries
        val it: MutableIterator<Map.Entry<T, Double>> = probabilities.iterator()
        var e: Map.Entry<T, Double>
        while (it.hasNext()) {
            e = it.next()
            if (e.value <= percent && e.value < max - roundingError) {
                it.remove()
                if (size() == 1) {
                    scaleProbs()
                    fixMaxPercent()
                    fixProbSum()
                    return
                }
            }
        }
        scaleProbs()
        fixMaxPercent()
        fixProbSum()
    }

    operator fun contains(t: T): Boolean {
        return probabilityMap.containsKey(t)
    }

    fun getKeys() = probabilityMap.keys

    /**
     * Returns the number of elements in this ProbFunTree.
     *
     * @return the number of elements in this ProbFunTree.
     */
    fun size(): Int {
        return probabilityMap.size
    }

    /**
     * Adjust the probability to make element more likely to be returned when fun() is called from this ProbFunTree.
     *
     * @param element as the element to make appear more often
     * @param percent as the percentage between 0 and 1 (exclusive),
     * of the probability of getting element to add to the probability.
     * @return the adjusted probability.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    fun good(element: T): Double {
        Objects.requireNonNull(element)
        val percent = settingsRepo.getPercentChangeUp()
        require((percent < 1.0 && percent > 0.0)) {
            "percent passed to good() is not between 0.0 and 1.0 (exclusive)"
        }
        if ((!contains(element)) || (probabilityMap[element]!! >= maxPercent())) return -1.0
        val oldProb = probabilityMap[element] ?: return -1.0
        var add = probToAddForGood(oldProb, percent)
        var newPercent = percent
        while (oldProb + add > maxPercent() - roundingError) {
            newPercent *= percent
            add = probToAddForGood(oldProb, newPercent)
        }
        val goodProbability = oldProb + add
        if (goodProbability >= (1.0 - (size() * MIN_VALUE))) return -1.0
        probabilityMap[element] = goodProbability
        val leftover = 1.0 - goodProbability
        val sumOfLeftovers = probSum() - goodProbability
        val leftoverScale = leftover / sumOfLeftovers
        for (e in probabilityMap.entries.toList()) {
            probabilityMap[e.key] = (e.value * leftoverScale)
        }
        probabilityMap[element] = goodProbability
        fixProbSum()
        return probabilityMap[element]!!
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
    fun bad(element: T): Double {
        // TODO Fix how maxPercent can be lower than the max prob here
        Objects.requireNonNull(element)
        val percent = settingsRepo.getPercentChangeDown()
        require((percent < 1.0 && percent > 0.0)) {
            "percent passed to good() is not between 0.0 and 1.0 (exclusive)"
        }
        val oldProb = probabilityMap[element] ?: return -1.0
        val sub = oldProb * percent
        if (oldProb - sub <= roundingError) return oldProb
        val badProbability = oldProb - sub
        if (badProbability <= (size() * MIN_VALUE)) return -1.0
        probabilityMap[element] = badProbability
        val leftover = 1.0 - badProbability
        val sumOfLeftovers = probSum() - badProbability
        val leftoverScale = leftover / sumOfLeftovers
        for (e in probabilityMap.entries.toList()) {
            probabilityMap[e.key] = (e.value * leftoverScale)
        }
        probabilityMap[element] = badProbability
        fixMaxPercent()
        fixProbSum()
        return probabilityMap[element]!!
    }

    private fun fixMaxPercent() {
        var maxCount = 0
        var notMaxCount = 0
        for (prob in probabilityMap.entries) {
            if (prob.value >= maxPercent()) {
                probabilityMap[prob.key] = maxPercent()
                maxCount++
            } else {
                notMaxCount++
            }
        }
        val maxes = maxCount.toDouble() * maxPercent()
        val leftover = 1.0 - maxes
        var leftoverSum = 0.0
        for (prob in probabilityMap.values) {
            if (prob < maxPercent()) {
                leftoverSum += prob
            }
        }
        val addToNonMax = (leftover - leftoverSum) / notMaxCount.toDouble()
        var tooMuch = false
        for (prob in probabilityMap.entries) {
            if (prob.value < maxPercent()) {
                val p = prob.value + addToNonMax
                probabilityMap[prob.key] = p
                if (p > maxPercent()) {
                    tooMuch = true
                }
            }
        }
        if (tooMuch) {
            var same = true
            val p = probabilityMap.values.iterator().next()
            for (prob in probabilityMap.values) {
                if (prob !in (p - roundingError)..(p + roundingError)) {
                    same = false
                }
            }
            if(!same) {
                fixMaxPercent()
            } else{
                setMaxPercent(p)
            }
        }
    }

    /**
     * Lowers the probabilities so there is about at most
     * [low] chance of getting any element from this ProbFunTree.
     * @param low as the low chance between 0.0 and 1.0.
     */
    fun lowerProbs(low: Double) {
        require((low >= (size() * MIN_VALUE)) && low <= (1.0 - (size() * MIN_VALUE)))
        val probs: Collection<T> = probabilityMap.keys.toList()
        for (t in probs) {
            if (probabilityMap[t]!! > low) {
                probabilityMap[t] = low
            }
        }
        var maxSum = 0.0
        var otherSum = 0.0
        for (t in probs) {
            if (probabilityMap[t] == low) {
                maxSum += probabilityMap[t]!!
            } else {
                otherSum += probabilityMap[t]!!
            }
        }
        val leftovers = 1.0 - maxSum
        val scale = leftovers / otherSum
        for (t in probs) {
            if (probabilityMap[t] != low) {
                probabilityMap[t] = probabilityMap[t]!! * scale
            }
        }
        scaleProbs()
        fixMaxPercent()
        fixProbSum()
    }

    /**
     * Sets the probabilities to there being an equal chance of getting any element from this ProbFunTree.
     */
    fun resetProbabilities() {
        for (e in probabilityMap.entries.toList()) {
            probabilityMap[e.key] = 1.0
        }
        scaleProbs()
    }

    /**
     * Fixes rounding error in the probabilities by adding up the probabilities
     * and changing the first probability so all probabilities add up to 1.0.
     * TODO This is a terrible solution to the rounding error
     */
    private fun fixProbSum() {
        roundingError = 1.0 - probSum()
        var firstProb = probabilityMap.entries.iterator().next()
        while (firstProb.value * 2.0 < roundingError) {
            var p: Double
            for (e in probabilityMap.entries) {
                p = e.value + (roundingError / probabilityMap.size.toDouble())
                e.setValue(p)
            }
            firstProb = probabilityMap.entries.iterator().next()
        }
        firstProb.setValue(firstProb.value + roundingError)
    }

    /**
     * @return the sum of all the probabilities in order to fix rounding error.
     */
    private fun probSum(): Double {
        val probabilities = probabilityMap.values.toList()
        var sum = 0.0
        for (p in probabilities) {
            sum += p
        }
        return sum
    }

    /**
     * Scales the probabilities so they add up to 1.0.
     */
    private fun scaleProbs() {
        val scale = 1.0 / probSum()
        val probabilities = probabilityMap.entries.toList()
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        fixProbSum()
    }

    fun getProbability(t: T): Double {
        return probabilityMap[t]!!
    }

    /**
     * Returns a randomly picked element from this ProbFunTree, based on the previously returned elements.
     *
     * @return a randomly picked element from this ProbFunTree.
     * Any changes in the element will be reflected in this ProbFunTree.
     */
    fun next(random: Random): T {
        val randomChoice = random.nextDouble()
        val entries = probabilityMap.entries.iterator()
        var element: T? = null
        var sumOfProbabilities = 0.0
        var e: Map.Entry<T, Double>
        while (randomChoice > sumOfProbabilities) {
            e = entries.next()
            element = e.key
            sumOfProbabilities += e.value
        }
        return element!!
    }

    override fun toString(): String {
        val id = hashCode()
        val sb = StringBuilder()
        sb.append("PF ")
        sb.append(id)
        sb.append(": [")
        for ((key, value) in probabilityMap.toList()) {
            sb.append("[")
            sb.append(key)
            sb.append(" = ")
            sb.append(value * 100.0)
            sb.append("%],")
        }
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    override fun hashCode() = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean {
        if (other is ProbFun<*>) {
            return hashCode() == other.hashCode()
        }
        return false
    }

    companion object {

        private const val serialVersionUID = -6556634307811294014L

    }

}