package com.example.waveplayer;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * A tree node where a set of elements are picked from a probability function to decide which child node
 * will produce the next element when fun() is called.
 *
 * @param <T> The type of the elements that make up the elements in the probability function.
 * @author Alexander Johnston
 * @since Copyright 2020
 */
class ProbFun<T extends Comparable<T>> implements Serializable {

    private static final long serialVersionUID = -6556634307811294014L;

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    protected Map<T, Double> probabilityMap;
    public ArrayList<T> getKeys() {
        return new ArrayList<>(this.probabilityMap.keySet());
    }

    // The unique id of this ProbFunTree
    private int id = 0;

    private double roundingError = 0;

    private boolean comparable;

    private double maxPercent;

    public void setMaxPercent(double maxPercent) {
        this.maxPercent = maxPercent;
    }

    protected ProbFun(Set<T> choices, double maxPercent, boolean comparable) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (maxPercent <= 0 || maxPercent > 1.0) {
            throw new IllegalArgumentException("maxPercent passed into the ProbFunTree constructor must be above 0 and 1.0 or under");
        }
        // Invariants secured
        this.comparable = comparable;
        if (comparable) {
            this.probabilityMap = new TreeMap<T, Double>();
        } else {
            this.probabilityMap = new LinkedHashMap<T, Double>();
        }
        this.maxPercent = maxPercent;
        for (T choice : choices) {
            this.probabilityMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
    }

    /**
     * Scales the probabilities so they add up to 1.0.
     */
    private void scaleProbs() {
        double scale = 1.0 / probSum();
        Set<Entry<T, Double>> probabilities = this.probabilityMap.entrySet();
        for (Entry<T, Double> e : probabilities) {
            e.setValue(e.getValue() * scale);
        }
        fixProbSum();
    }

    /**
     * @return the sum of all the probabilities in order to fix rounding error.
     */
    private double probSum() {
        Collection<Double> probabilities = this.probabilityMap.values();
        double sum = 0;
        for (Double d : probabilities) {
            sum += d;
        }
        return sum;
    }

    /**
     * Fixes rounding error in the probabilities by adding up the probabilities
     * and changing the first probability so all probabilities add up to 1.0.
     */
    private void fixProbSum() {
        Entry<T, Double> firstProb = this.probabilityMap.entrySet().iterator().next();
        this.roundingError = 1.0 - probSum();
        while ((firstProb.getValue() * 2.0) < roundingError) {
            double p;
            for (Entry<T, Double> e : this.probabilityMap.entrySet()) {
                p = e.getValue();
                p += roundingError / this.probabilityMap.size();
                e.setValue(p);
            }
            firstProb = this.probabilityMap.entrySet().iterator().next();
            this.roundingError = 1.0 - probSum();
        }
        firstProb.setValue(firstProb.getValue() + this.roundingError);
    }

    /**
     * Sets the probabilities to there being an equal chance of getting any element from this ProbFunTree.
     */
    public void clearProbabilities() {
        if (comparable) {
            this.probabilityMap = (new ProbFunTreeMap<>(this.probabilityMap.keySet(), 1)).probabilityMap;
        } else {
            this.probabilityMap = (new ProbFunLinkedMap<>(this.probabilityMap.keySet(), 1)).probabilityMap;
        }
    }

    /**
     * Lowers the probabilities so there is about at most a low chance of getting any element from this ProbFunTree.
     *
     * @param low as the low chance.
     */
    public void lowerProbs(double low) {
        Collection<T> probs = this.probabilityMap.keySet();
        for (T t : probs) {
            while (probabilityMap.get(t) > low) {
                bad(t, 0.1);
            }
        }
    }

    public double getProbability(T t){
        return probabilityMap.get(t);
    }

    /**
     * Adds element to this ProbFunTree, making the probability equal to 1.0/n
     * where n is the number of elements contained in this ProbFunTree,
     * and appends elements as a child ProbFunTree, where elements are the choices.
     *
     * @param element as the element to add to this ProbFunTree.
     * @throws NullPointerException if element is null.
     */
    public void add(T element) {
        Objects.requireNonNull(element);
        // Invariants secured
        double probability = 1.0 / (this.probabilityMap.size());
        if (!this.probabilityMap.containsKey(element)) {
            this.probabilityMap.put(element, probability);
        } else {
            return;
        }
        scaleProbs();
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
    public void add(T element, double percent) {
        Objects.requireNonNull(element);
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException("percent passed to add() is not between 0.0 and 1.0 (exclusive)");
        }
        // Invariants secured
        double scale = (1.0 - percent);
        Set<Entry<T, Double>> probabilities = this.probabilityMap.entrySet();
        for (Entry<T, Double> e : probabilities) {
            e.setValue(e.getValue() * scale);
        }
        this.probabilityMap.put(element, percent);
        scaleProbs();
    }

    /**
     * Removes an element from this ProbFunTree unless there is only one element.
     *
     * @param element as the element to remove from this ProbFunTree.
     * @return True if this ProbFunTree's parent contained the element and it was removed, else false.
     * @throws NullPointerException if element is null.
     */
    public boolean remove(T element) {
        Objects.requireNonNull(element);
        if (size() == 1) {
            return false;
        }
        // Invariants secured
        if (this.probabilityMap.remove(element) == null) {
            return false;
        }
        scaleProbs();
        return true;
    }

    public boolean contains(T t){
        return probabilityMap.containsKey(t);
    }

    /**
     * Adjust the probability to make element more likely to be returned when fun() is called from this ProbFunTree.
     *
     * @param element as the element to make appear more often
     * @param percent as the percentage between 0 and 1 (exclusive),
     *                of the probability of getting element to add to the probability.
     * @return the adjusted probability.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    public double good(T element, double percent) {
        Objects.requireNonNull(element);
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException("percent passed to good() is not between 0.0 and 1.0 (exclusive)");
        }
        // Invariants secured
        Double oldProb = this.probabilityMap.get(element);
        if (oldProb == null) {
            return -1;
        }
        double add;
        if (oldProb > 0.5)
            add = ((1.0 - oldProb) * percent);
        else
            add = (oldProb * percent);
        if (oldProb + add >= (maxPercent - this.roundingError))
            return oldProb;
        double goodProbability = oldProb + add;
        this.probabilityMap.put(element, goodProbability);
        double leftover = 1.0 - goodProbability;
        double sumOfLeftovers = probSum() - goodProbability;
        double leftoverScale = leftover / sumOfLeftovers;
        for (Entry<T, Double> e : this.probabilityMap.entrySet()) {
            e.setValue(e.getValue() * leftoverScale);
        }
        this.probabilityMap.put(element, goodProbability);
        fixProbSum();
        return this.probabilityMap.get(element);
    }

    /**
     * Adjust the probability to make element less likely to be returned when fun() is called from this ProbFunTree.
     *
     * @param element as the element to make appear less often
     * @param percent as the percentage between 0 and 1 (exclusive),
     *                of the probability of getting element to subtract from the probability.
     * @return the adjusted probability or -1 if this ProbFunTree didn't contain the element.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive.
     */
    public double bad(T element, double percent) {
        Objects.requireNonNull(element);
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException("percent passed to good() is not between 0.0 and 1.0 (exclusive)");
        }
        // Invariants secured
        Double oldProb = this.probabilityMap.get(element);
        if (oldProb == null) {
            return -1;
        }
        double sub = (oldProb * percent);
        if (oldProb - sub <= this.roundingError)
            return oldProb;
        double badProbability = oldProb - sub;
        this.probabilityMap.put(element, badProbability);
        double leftover = 1.0 - badProbability;
        double sumOfLeftovers = probSum() - badProbability;
        double leftoverScale = leftover / sumOfLeftovers;
        for (Entry<T, Double> e : this.probabilityMap.entrySet()) {
            e.setValue(e.getValue() * leftoverScale);
        }
        this.probabilityMap.put(element, badProbability);
        fixProbSum();
        return this.probabilityMap.get(element);
    }

    /**
     * Returns a randomly picked element from this ProbFunTree, based on the previously returned elements.
     *
     * @return a randomly picked element from this ProbFunTree.
     * Any changes in the element will be reflected in this ProbFunTree.
     */
    public T fun(Random random) {
        return nextValue(random);
    }

    /**
     * For generating the next value.
     *
     * @return the next generated value.
     */
    private T nextValue(Random random) {
        double randomChoice = random.nextDouble();
        double sumOfProbabilities = 0.0;
        Iterator<Entry<T, Double>> entries = this.probabilityMap.entrySet().iterator();
        T element = null;
        Entry<T, Double> e;
        while ((randomChoice > sumOfProbabilities)) {
            e = entries.next();
            element = e.getKey();
            sumOfProbabilities += e.getValue();
        }
        return element;
    }

    /**
     * Returns the number of elements in this ProbFunTree.
     *
     * @return the number of elements in this ProbFunTree.
     */
    public int size() {
        return this.probabilityMap.size();
    }


    /**
     * Private copy constructor for clone
     *
     * @param probFun as the ProbFunTree to copy
     */
    private ProbFun(ProbFun<T> probFun) {
        for (Entry<T, Double> s : probFun.probabilityMap.entrySet()) {
            this.probabilityMap.put(s.getKey(), s.getValue());
        }
    }

    @NonNull
    @Override
    public ProbFun<T> clone() {
        return new ProbFun<>(this);
    }

    @NonNull
    @Override
    public String toString() {
        if (this.id == 0) {
            this.id = System.identityHashCode(this);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PF ");
        sb.append(this.id);
        sb.append(": [");
        for (Entry<T, Double> e : this.probabilityMap.entrySet()) {
            sb.append("[");
            sb.append(e.getKey());
            sb.append(" = ");
            sb.append(e.getValue() * 100.0);
            sb.append("%],");
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        if (this.id == 0) {
            this.id = System.identityHashCode(this);
        }
        return this.id;
    }

    public void swapPositions(int oldPosition, int newPosition) {
        Map<T, Double> oldMap = probabilityMap;
        ArrayList<T> keySetList = new ArrayList<>(oldMap.keySet());
        Collections.swap(keySetList, oldPosition, newPosition);
        LinkedHashMap<T, Double> swappedMap = new LinkedHashMap<>();
        for (T oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        probabilityMap = swappedMap;
    }

    public void switchPositions(int oldPosition, int newPosition) {
        Map<T, Double> oldMap = probabilityMap;
        ArrayList<T> keySetList = new ArrayList<>(oldMap.keySet());
        keySetList.add(newPosition, keySetList.get(oldPosition));
        keySetList.remove(oldPosition + 1);
        LinkedHashMap<T, Double> swappedMap = new LinkedHashMap<>();
        for (T oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        probabilityMap = swappedMap;
    }
}