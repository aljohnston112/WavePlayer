package com.fourthFinger.pinkyPlayer.random_playlist

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Serializable
import java.util.*
import kotlin.random.Random

const val MIN_VALUE = 0.0000000000000005

sealed class ProbFun<T>(
    choices: Set<T>,
    var maxPercent: Double,
    comparable: Boolean
) : Serializable, Cloneable {

    class ProbFunLinkedMap<T>(
        choices: Set<T>, maxPercent: Double
    ) : ProbFun<T>(choices, maxPercent, false) {

        constructor(probFun: ProbFunLinkedMap<T>) : this(
            probFun.probabilityMap.keys, probFun.maxPercent
        ) {
            for ((key, value) in probFun.probabilityMap) {
                probabilityMap[key] = value
            }
            roundingError = probFun.roundingError
        }

        public override fun clone(): ProbFunLinkedMap<T> {
            return ProbFunLinkedMap(this)
        }

    }

    class ProbFunTreeMap<T : Comparable<T>>(
        choices: Set<T>, maxPercent: Double
    ) : ProbFun<T>(choices, maxPercent, true) {

        constructor(probFun: ProbFunTreeMap<T>) : this(
            probFun.probabilityMap.keys, probFun.maxPercent
        ) {
            for ((key, value) in probFun.probabilityMap) {
                probabilityMap[key] = value
            }
            roundingError = probFun.roundingError
        }

        public override fun clone(): ProbFunTreeMap<T> {
            return ProbFunTreeMap(this)
        }
    }

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    protected var probabilityMap: MutableMap<T, Double>

    protected var roundingError = 0.0

    protected val id by lazy { hashCode() }

    init {
        Objects.requireNonNull(choices)
        require(choices.size < 2000000000000000) {
            "ProbFun will not work with a size greater than 2,000,000,000,000,000"
        }
        require((maxPercent > (0) && maxPercent <= 1.0)) {
            "maxPercent passed into the ProbFunTree constructor must be above 0 and under 1.0" +
                    "value was $maxPercent"
        }
        if (maxPercent == 1.0) {
            this.maxPercent = (1.0 - (choices.size * MIN_VALUE))
        }
        probabilityMap = if (comparable) TreeMap() else LinkedHashMap()
        for (choice in choices) {
            probabilityMap[choice] = 1.0 / choices.size
        }
        if(choices.isNotEmpty()) {
            fixProbSum()
        }
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
            var probability = 1.0 / probabilityMap.size
            if(probabilityMap.isEmpty()){
                probability = 1.0
            }
            probabilityMap[element] = probability
            scaleProbabilities()
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
        require((percent >= ((size() + 1) * MIN_VALUE)) && percent <= (1.0 - ((size() + 1) * MIN_VALUE))) {
            "percent passed to add() is not between 0.0 and 1.0 (exclusive)"
        }
        val scale = 1.0 - percent
        val probabilities = probabilityMap.entries.toList()
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        probabilityMap[element] = percent
        scaleProbabilities()
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
        scaleProbabilities()
        return true
    }

    /**
     * Removes elements with the lowest probability of occurring when [next] is called.
     * If elements have the same maximum probability of occurring, no elements will be removed.
     * If after a removal,
     * elements have the same maximum probability of occurring,
     * no more elements will be removed.
     * If parentSize() == 1, no elements will be removed.
     * If parentSize() == 1 after a removal, no more elements will be removed.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun prune() {
        if (size() == 1) {
            return
        }
        val min = probabilityMap.values.stream().parallel().min { d1: Double, d2: Double ->
            (d1).compareTo((d2))
        }
        val max = probabilityMap.values.stream().parallel().max { d1: Double, d2: Double ->
            (d1).compareTo((d2))

        }
        if (max == min) {
            return
        }
        val probabilities = probabilityMap.entries
        val it = probabilities.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.value <= min.get() && e.value < (max.get() - roundingError)) {
                it.remove()
                if (size() == 1) {
                    scaleProbabilities()
                    return
                }
            }
        }
        scaleProbabilities()
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
    @RequiresApi(Build.VERSION_CODES.N)
    fun prune(percent: Double) {
        if (percent >= 1.0 || percent <= 0.0) {
            throw IllegalArgumentException("percent passed to prune() is not between 0.0 and 1.0 (exclusive)")
        }
        val max = probabilityMap.values.stream().parallel().max { d1: Double, d2: Double ->
            (d1).compareTo((d2))

        }
        val min = probabilityMap.values.stream().parallel().min { d1: Double, d2: Double ->
            (d1).compareTo((d2))

        }
        if (size() == 1 || (max.get() <= percent && min.get() == max.get())) {
            return
        }
        val probabilities = probabilityMap.entries
        val it: MutableIterator<Map.Entry<T, Double>> = probabilities.iterator()
        var e: Map.Entry<T, Double>
        while (it.hasNext()) {
            e = it.next()
            if (e.value <= min.get() && e.value < max.get() - roundingError) {
                it.remove()
                if (size() == 1) {
                    scaleProbabilities()
                    return
                }
            }
        }
        scaleProbabilities()
    }

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
    fun good(element: T, percent: Double): Double {
        Objects.requireNonNull(element)
        require((percent < 1.0 && percent > 0.0)) {
            "percent passed to good() is not between 0.0 and 1.0 (exclusive)"
        }
        if ((!contains(element)) || (probabilityMap[element]!! >= maxPercent)) return -1.0
        val oldProb = probabilityMap[element] ?: return -1.0
        var add = probToAddForGood(oldProb, percent)
        var newPercent = percent
        while (oldProb + add >= maxPercent - roundingError) {
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
    fun bad(element: T, percent: Double): Double {
        // TODO Fix how maxPercent can be lower than the max prob here
        Objects.requireNonNull(element)
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
        fixProbSum()
        return probabilityMap[element]!!
    }

    /**
     * Lowers the probabilities so there is about at most
     * [low] chance of getting any element from this ProbFunTree.
     * @param low as the low chance between 0.0 and 1.0.
     */
    fun lowerProbabilities(low: Double) {
        require((low >= (size() * MIN_VALUE)) && low <= (1.0 - (size() * MIN_VALUE)))
        val probabilities: Collection<T> = probabilityMap.keys.toList()
        for (t in probabilities) {
            if (probabilityMap[t]!! > low) {
                probabilityMap[t] = low
            }
        }
        var maxSum = 0.0
        var otherSum = 0.0
        for (t in probabilities) {
            if (probabilityMap[t] == low) {
                maxSum += probabilityMap[t]!!
            } else {
                otherSum += probabilityMap[t]!!
            }
        }
        val leftovers = 1.0 - maxSum
        val scale = leftovers / otherSum
        for (t in probabilities) {
            if (probabilityMap[t] != low) {
                probabilityMap[t] = probabilityMap[t]!! * scale
            }
        }
        scaleProbabilities()
    }

    /**
     * Sets the probabilities to there being an equal chance of getting any element from this ProbFunTree.
     */
    fun resetProbabilities() {
        for (e in probabilityMap.entries.toList()) {
            probabilityMap[e.key] = 1.0
        }
        scaleProbabilities()
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
    private fun scaleProbabilities() {
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
    fun next(): T {
        val randomChoice = Random.nextDouble()
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

    abstract override fun clone(): ProbFun<T>

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
        sb.deleteCharAt(sb.length-1)
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

        private const val SERIAL_VERSION_UID = -6556634307811294014L

    }
}
