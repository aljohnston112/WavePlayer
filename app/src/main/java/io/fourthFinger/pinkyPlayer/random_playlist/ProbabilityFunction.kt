package io.fourthFinger.pinkyPlayer.random_playlist

import java.io.Serializable
import java.util.*
import kotlin.random.Random

const val MIN_VALUE = 0.0000000000000005


// TODO replace this class with integer based probabilities

/**
 * A adjustable probability distribution over the group of items.
 * Supports up to 2*10^15 items.
 *
 * @param items The set of items to be assigned probabilities.
 * @param maxPercent The max percent that any item can have.
 * @param comparable Whether or not the items are comparable.
 *
 */
sealed class ProbabilityFunction<T>(
    items: Set<T>,
    comparable: Boolean,
    var maxPercent: Double,
) : Serializable, Cloneable {

    // The set of elements to be picked from,
    // mapped to the probabilities of getting picked
    protected var probabilityMap: MutableMap<T, Double>

    protected var roundingError = 0.0

    class ProbabilityFunctionLinkedMap<T>(
        choices: Set<T>,
        maxPercent: Double
    ) : ProbabilityFunction<T>(
        choices,
        false,
        maxPercent

    ) {

        constructor(probFun: ProbabilityFunctionLinkedMap<T>) : this(
            probFun.probabilityMap.keys,
            probFun.maxPercent
        ) {
            for ((key, value) in probFun.probabilityMap) {
                probabilityMap[key] = value
            }
            roundingError = probFun.roundingError
        }

        public override fun clone(): ProbabilityFunctionLinkedMap<T> {
            return ProbabilityFunctionLinkedMap(this)
        }

    }

    class ProbabilityFunctionTreeMap<T : Comparable<T>>(
        choices: Set<T>,
        maxPercent: Double
    ) : ProbabilityFunction<T>(
        choices,
        true,
        maxPercent
    ) {

        constructor(probFun: ProbabilityFunctionTreeMap<T>) : this(
            probFun.probabilityMap.keys, probFun.maxPercent
        ) {
            for ((key, value) in probFun.probabilityMap) {
                probabilityMap[key] = value
            }
            roundingError = probFun.roundingError
        }

        public override fun clone(): ProbabilityFunctionTreeMap<T> {
            return ProbabilityFunctionTreeMap(this)
        }
    }

    init {
        Objects.requireNonNull(items)
        require(items.size < 2000000000000000) {
            "ProbabilityFunction will not work with a size greater than 2,000,000,000,000,000"
        }
        require((maxPercent > (0) && maxPercent <= 1.0)) {
            "maxPercent passed into the ProbabilityFunction constructor must be above 0 and under 1.0; " +
                    "value was $maxPercent"
        }
        // Prevents the probability of items going to 0
        if (maxPercent == 1.0) {
            this.maxPercent = (1.0 - (items.size * MIN_VALUE))
        }

        probabilityMap = if (comparable) TreeMap() else LinkedHashMap()

        // Equal probability for all items
        for (choice in items) {
            probabilityMap[choice] = 1.0 / items.size
        }
        fixProbabilitySum()
    }

    /**
     * @return All items in this ProbabilityFunction.
     */
    fun getItems() = probabilityMap.keys

    /**
     * Fixes rounding error in the probabilities so they add up to 1.0
     */
    private fun fixProbabilitySum() {
        if (probabilityMap.isNotEmpty()) {
            roundingError = 1.0 - probabilitySum()
            val firstProb = probabilityMap.entries.iterator().next()
            for (entry in probabilityMap.entries) {
                val newProbability = entry.value + (roundingError / probabilityMap.size.toDouble())
                entry.setValue(newProbability)
            }
            roundingError = 1.0 - probabilitySum()
            firstProb.setValue(firstProb.value + roundingError)
            roundingError = 1.0 - probabilitySum()
        }
    }

    /**
     * @return The sum of all the probabilities.
     */
    private fun probabilitySum(): Double {
        val probabilities = probabilityMap.values
        var sum = 0.0
        for (probability in probabilities) {
            sum += probability
        }
        return sum
    }

    /**
     * Adds element to this ProbabilityFunction, 
     * making the probability equal to 1.0 / n
     * where n is the number of elements contained in this ProbabilityFunction.
     * 
     * If this ProbabilityFunction already contains the element, 
     * then nothing will be added.
     * 
     * @param element The element to add to this ProbabilityFunction.
     * @throws NullPointerException if element is null.
     */
    fun add(element: T) {
        Objects.requireNonNull(element)
        if (!probabilityMap.containsKey(element)) {
            var probability = 1.0 / probabilityMap.size
            if (probabilityMap.isEmpty()) {
                probability = 1.0
            }
            probabilityMap[element] = probability
            scaleProbabilities()
        }
    }

    /**
     * Scales the probabilities so they add up to 1.0.
     */
    private fun scaleProbabilities() {
        val scale = 1.0 / probabilitySum()
        val entries = probabilityMap.entries
        for (entry in entries) {
            entry.setValue(entry.value * scale)
        }
        fixProbabilitySum()
    }

    /**
     * Adds element to this ProbabilityFunction with the specified probability.
     * If the element exists in this ProbabilityFunction,
     * then its probability will be overwritten with percent.
     *
     * @param element The element to add to this ProbabilityFunction.
     * @param percent The chance of this ProbabilityFunction returning element.
     * Must be above 0 and under 1 by MIN_VALUE * getSize()
     * @throws NullPointerException if element is null.
     * @throws IllegalArgumentException if percent is not between 0.0 and 1.0 (exclusive).
     */
    fun add(
        element: T,
        percent: Double
    ) {
        Objects.requireNonNull(element)
        require((percent >= ((getSize() + 1) * MIN_VALUE)) &&
                percent <= (1.0 - ((getSize() + 1) * MIN_VALUE))
        ) {
            "percent passed to add() was not above 0 or under 1 by MIN_VALUE * getSize()"
        }
        val scale = 1.0 - percent
        val probabilities = probabilityMap.entries
        for (e in probabilities) {
            e.setValue(e.value * scale)
        }
        probabilityMap[element] = percent
        scaleProbabilities()
    }

    /**
     * Returns the number of elements in this ProbabilityFunction.
     *
     * @return the number of elements in this ProbabilityFunction.
     */
    fun getSize(): Int {
        return probabilityMap.size
    }

    /**
     * Removes an element from this ProbabilityFunction unless there is only one element.
     *
     * @param element The element to remove from this ProbabilityFunction.
     *
     * @return True if this ProbabilityFunction contained the element and it was removed, else false.
     * @throws NullPointerException if element is null.
     */
    fun remove(element: T): Boolean {
        Objects.requireNonNull(element)
        if (getSize() == 1) {
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
     */
    fun prune() {
        if (getSize() == 1) {
            return
        }
        val min = probabilityMap.values.min()
        val max = probabilityMap.values.max()
        if (max == min) {
            return
        }
        val probabilities = probabilityMap.entries
        val it = probabilities.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value <= min && entry.value < (max - roundingError)) {
                it.remove()
                if (getSize() == 1) {
                    scaleProbabilities()
                    return
                }
            }
        }
        scaleProbabilities()
    }

    /**
     * Removes elements that have lower than [percent] probability of occurring when [next] is called.
     * If elements have the same maximum probability of occurring, no elements will be removed.
     *
     * @param  percent as the upper limit, inclusive, of the probability of elements
     * to be removed from this ProbabilityFunction.
     * @throws IllegalArgumentException if percent is not between 0.0 and 1.0 (exclusive)
     */
    fun prune(percent: Double) {
        if (percent >= 1.0 || percent <= 0.0) {
            throw IllegalArgumentException(
                "percent passed to prune() is not between 0.0 and 1.0 (exclusive)"
            )
        }
        val max = probabilityMap.values.max()
        val min = probabilityMap.values.min()
        if (getSize() == 1 || (max <= percent && min == max)) {
            return
        }
        val probabilities = probabilityMap.entries
        val it: MutableIterator<Map.Entry<T, Double>> = probabilities.iterator()
        var entry: Map.Entry<T, Double>
        while (it.hasNext()) {
            entry = it.next()
            if (entry.value <= percent && entry.value < max - roundingError) {
                it.remove()
                if (getSize() == 1) {
                    scaleProbabilities()
                    return
                }
            }
        }
        scaleProbabilities()
    }

    /**
     * Swaps two elements in this ProbabilityFunction.
     * Calling this method on a sorted ProbabilityFunction will make it unsorted.
     *
     * @param firstPosition
     * @param secondPosition
     */
    fun swapTwoItems(
        firstPosition: Int,
        secondPosition: Int
    ) {
        val oldMap = probabilityMap
        val keys = oldMap.keys.toMutableList()
        Collections.swap(
            keys,
            firstPosition,
            secondPosition
        )

        val swappedMap = LinkedHashMap<T, Double>()
        for (key in keys) {
            swappedMap[key] = oldMap[key]!!
        }
        probabilityMap = swappedMap
    }

    /**
     * Switches the position of a single item,
     * moving all items at its position and above to their position plus one.
     * Calling this method on a sorted ProbabilityFunction will make it unsorted.
     *
     * @param oldPosition
     * @param newPosition
     */
    fun switchOnesPosition(
        oldPosition: Int,
        newPosition: Int
    ) {
        val oldMap = probabilityMap
        val keys = oldMap.keys.toMutableList()
        val itemBeingMoved = keys.removeAt(oldPosition)
        keys.add(
            newPosition,
            itemBeingMoved
        )

        val newMap = LinkedHashMap<T, Double>()
        for (key in keys) {
            newMap[key] = oldMap[key]!!
        }
        probabilityMap = newMap
    }

    /**
     * Adjust the probability to make element more likely to be returned when next() is called.
     *
     * @param element The element to make appear more often
     * @param percentIncrease The percentage between 0 and 1 (exclusive)
     * to increase the probability of next() returning element.
     *
     * @return The new probability or -1 if the probability was not changed.
     * @throws NullPointerException if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    fun good(
        element: T,
        percentIncrease: Double
    ): Double {
        Objects.requireNonNull(element)
        require((percentIncrease < 1.0 && percentIncrease > 0.0)) {
            "percent passed to good() is not between 0.0 and 1.0 (exclusive)"
        }
        if ((!contains(element)) || (probabilityMap[element]!! >= maxPercent)) return -1.0

        val oldProbability = probabilityMap[element]!!
        var probabilityToAdd = percentToAddForGood(
            oldProbability,
            percentIncrease
        )

        // Don't let the adjustment go over max percent
        var newPercentIncrease = percentIncrease
        while (oldProbability + probabilityToAdd >= maxPercent - roundingError) {
            newPercentIncrease *= percentIncrease
            probabilityToAdd = percentToAddForGood(
                oldProbability,
                newPercentIncrease
            )
        }

        val newProbability = oldProbability + probabilityToAdd
        if (newProbability >= (1.0 - (getSize() * MIN_VALUE))) return -1.0

        probabilityMap[element] = newProbability
        val newLeftover = 1.0 - newProbability
        val oldLeftover = probabilitySum() - newProbability
        val leftoverScale = newLeftover / oldLeftover
        for (entry in probabilityMap.entries) {
            probabilityMap[entry.key] = (entry.value * leftoverScale)
        }
        probabilityMap[element] = newProbability
        fixProbabilitySum()
        return probabilityMap[element]!!
    }

    /**
     *
     * @param oldProbability The probability of the element to be changed.
     * @param percentChange The percent change provided by the client.
     *
     * @return The percent to multiply the probability of an element for good().
     */
    private fun percentToAddForGood(
        oldProbability: Double,
        percentChange: Double
    ): Double {
        return if (oldProbability > 0.5) {
            // Prevents going over 100%
            (1.0 - oldProbability) * percentChange
        } else {
            oldProbability * percentChange
        }
    }

    /**
     * Adjust the probability to make element less likely to be returned when next() is called.
     *
     * @param element The element to make appear less often
     * @param percentChange The percentage between 0 and 1 (exclusive),
     * to decrease the probability of next() returning element.
     *
     * @return The new probability or -1 if the probability was not changed.
     *
     * @throws NullPointerException if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    fun bad(
        element: T,
        percentChange: Double
    ): Double {
        Objects.requireNonNull(element)
        require((percentChange < 1.0 && percentChange > 0.0)) {
            "percent passed to good() is not between 0.0 and 1.0 (exclusive)"
        }
        val oldProbability = probabilityMap[element] ?: return -1.0
        val probabilityToSubtract = oldProbability * percentChange
        val newProbability = oldProbability - probabilityToSubtract
        if (newProbability <= roundingError ||
            newProbability <= (getSize() * MIN_VALUE)) return -1.0

        probabilityMap[element] = newProbability
        val newLeftover = 1.0 - newProbability
        val oldLeftover = probabilitySum() - newProbability
        val leftoverScale = newLeftover / oldLeftover
        for (entry in probabilityMap.entries) {
            probabilityMap[entry.key] = (entry.value * leftoverScale)
        }
        probabilityMap[element] = newProbability

        // TODO Fix how maxPercent can be lower than the max prob here
        fixProbabilitySum()
        return probabilityMap[element]!!
    }

    /**
     * Lowers the probabilities so there is about at most
     * [low] chance of getting any element from this ProbabilityFunction.
     *
     * @param low The low chance between 0.0 and 1.0.
     */
    fun lowerProbabilities(low: Double) {
        require((low >= (getSize() * MIN_VALUE)) && low <= (1.0 - (getSize() * MIN_VALUE)))
        val probabilities: Collection<T> = probabilityMap.keys
        for (item in probabilities) {
            if (probabilityMap[item]!! > low) {
                probabilityMap[item] = low
            }
        }
        var maxSum = 0.0
        var otherSum = 0.0
        for (item in probabilities) {
            if (probabilityMap[item] == low) {
                maxSum += probabilityMap[item]!!
            } else {
                otherSum += probabilityMap[item]!!
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
     * Sets the probabilities so there is an equal chance
     * of getting any element from this ProbabilityFunction.
     */
    fun resetProbabilities() {
        for (e in probabilityMap.entries.toList()) {
            probabilityMap[e.key] = 1.0
        }
        scaleProbabilities()
    }

    /**
     * @param item The item to get the probability of.
     *
     * @return The probability next() will return this element.
     */
    fun getProbability(item: T): Double {
        return probabilityMap[item]!!
    }

    /**
     * Returns a randomly picked element from this ProbabilityFunction.
     *
     * @return a randomly picked element from this ProbabilityFunction.
     * Any changes in the element will be reflected in this ProbabilityFunction.
     */
    fun next(): T {
        val randomChoice = Random.nextDouble()
        val entries = probabilityMap.entries.iterator()
        var entry = entries.next()
        var sumOfProbabilities = entry.value
        if(randomChoice != 0.0){
            while (randomChoice > sumOfProbabilities) {
                entry = entries.next()
                sumOfProbabilities += entry.value
            }
        }
        return entry.key
    }

    operator fun contains(t: T): Boolean {
        return probabilityMap.containsKey(t)
    }

    abstract override fun clone(): ProbabilityFunction<T>

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

}
