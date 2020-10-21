package com.example.waveplayer;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
public class ProbFunLinkedMap<T extends Comparable<T>> extends ProbFunTree<T> implements Serializable, Comparable<T> {

    private static final long serialVersionUID = -6556634307811294014L;

    // The unique id of this ProbFunTree
    private int id = 0;

    // The rounding error to prevent over and under flow
    private double roundingError = 0;

    public ProbFunLinkedMap(Set<T> choices, double maxPercent) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (maxPercent <= 0 || maxPercent > 1.0) {
            throw new IllegalArgumentException("maxPercent passed into the ProbFunTree constructor must be above 0 and 1.0 or under");
        }
        // Invariants secured
        this.maxPercent = maxPercent;
        for (T choice : choices) {
            this.probMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
    }

    public void setProbMap(LinkedHashMap<T, Double> probMap) {
        this.probMap = probMap;
    }

    /**
     * Lowers the probabilities so there is about at most a low chance of getting any element from this ProbFunTree.
     *
     * @param low as the low chance.
     */
    public void lowerProbs(double low) {
        Collection<T> probs = this.probMap.keySet();
        for (T t : probs) {
            while (probMap.get(t) > low) {
                bad(t, 0.1);
            }
        }
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
        Double oldProb = this.probMap.get(element);
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
        this.probMap.put(element, goodProbability);
        double leftover = 1.0 - goodProbability;
        double sumOfLeftovers = probSum() - goodProbability;
        double leftoverScale = leftover / sumOfLeftovers;
        for (Entry<T, Double> e : this.probMap.entrySet()) {
            e.setValue(e.getValue() * leftoverScale);
        }
        this.probMap.put(element, goodProbability);
        fixProbSum();
        return this.probMap.get(element);
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
        Double oldProb = this.probMap.get(element);
        if (oldProb == null) {
            return -1;
        }
        double sub = (oldProb * percent);
        if (oldProb - sub <= this.roundingError)
            return oldProb;
        double badProbability = oldProb - sub;
        this.probMap.put(element, badProbability);
        double leftover = 1.0 - badProbability;
        double sumOfLeftovers = probSum() - badProbability;
        double leftoverScale = leftover / sumOfLeftovers;
        for (Entry<T, Double> e : this.probMap.entrySet()) {
            e.setValue(e.getValue() * leftoverScale);
        }
        this.probMap.put(element, badProbability);
        fixProbSum();
        return this.probMap.get(element);
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
        Iterator<Entry<T, Double>> entries = this.probMap.entrySet().iterator();
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
        return this.probMap.size();
    }


    /**
     * Private copy constructor for clone
     *
     * @param probFunTreeMap as the ProbFunTree to copy
     */
    private ProbFunLinkedMap(ProbFunLinkedMap<T> probFunTreeMap) {
        for (Entry<T, Double> s : probFunTreeMap.probMap.entrySet()) {
            this.probMap.put(s.getKey(), s.getValue());
        }
    }

    @NonNull
    @Override
    public ProbFunLinkedMap<T> clone() {
        return new ProbFunLinkedMap<>(this);
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
        for (Entry<T, Double> e : this.probMap.entrySet()) {
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

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(T o) {
        if (this.id == 0) {
            this.id = System.identityHashCode(this);
        }
        return this.id - ((ProbFunLinkedMap<T>) o).id;
    }

    @Override
    public void clearProbs() {
        this.probMap = (new ProbFunLinkedMap<>(this.probMap.keySet(), 1)).probMap;
    }
}