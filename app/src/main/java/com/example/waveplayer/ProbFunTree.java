package com.example.waveplayer;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * A tree node where a set of elements are picked from a probability function to decide which child node
 * will produce the next element when fun() is called.
 *
 * @param <T> The type of the elements that make up the elements in the probability function.
 * @author Alexander Johnston
 * @since Copyright 2020
 */
public class ProbFunTree<T extends Comparable<T>> implements Serializable, Comparable<T> {

    private static final long serialVersionUID = -6556634307811294014L;

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    private LinkedHashMap<T, Double> probMap = new LinkedHashMap<>();

    // The set of elements to be picked from, mapped to the probabilities of getting picked
    private LinkedHashMap<T, ProbFunTree<T>> children = new LinkedHashMap<>();

    // The parent ProbFunTree of this ProbFunTree
    private ProbFunTree<T> parent = null;

    // The last elements returned from the probFun of this ProbFunTree
    private T previousElement = null;

    // The unique id of this ProbFunTree
    private int id = 0;

    // The number of layers deep this ProbFunTree is
    private int layer;

    public void setMaxPercent(double maxPercent) {
        this.maxPercent = maxPercent;
    }

    private double maxPercent;

    // The rounding error to prevent over and under flow
    private double roundingError = 0;

    /**
     * Creates a ProbFunTree where there is an equal chance of getting any element from choices when fun() in called.
     * Note that the elements in choices passed into this constructor will NOT be copied and will be added by reference.
     *
     * @param choices as the choices to be randomly picked from.
     * @param layers  as the number of layers for this ProbFunTree to generate.
     *                Ex: for choices[0, 1] and layers=2, the following data structure will be made,
     *                <br>{@literal [[0->0.5][1->0.5]]} where the first choice is propagated like so
     *                {@literal [[0->[[0->0.5][1->0.5]]][1->[[0->0.5][1->0.5]]]]}.
     * @throws NullPointerException     if choices is null.
     * @throws IllegalArgumentException if there isn't at least one element in choices, or
     *                                  layers is not at least 1.
     */
    public ProbFunTree(Set<T> choices, int layers) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (layers < 1) {
            throw new IllegalArgumentException("layers passed into the ProbFunTree constructor must be at least 1");
        }
        // Invariants secured
        this.layer = 0;
        this.maxPercent = 1.0;
        for (T choice : choices) {
            this.probMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
        if (layers != this.layer + 1) {
            for (T t : this.probMap.keySet()) {
                this.children.put(t, new ProbFunTree<>(choices, layers, this));
            }
        }
    }

    public ProbFunTree(Set<T> choices, int layers, double maxPercent) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (layers < 1) {
            throw new IllegalArgumentException("layers passed into the ProbFunTree constructor must be at least 1");
        }
        if (maxPercent <= 0 || maxPercent > 1.0) {
            throw new IllegalArgumentException("maxPercent passed into the ProbFunTree constructor must be above 0 and 1.0 or under");
        }
        // Invariants secured
        this.layer = 0;
        this.maxPercent = maxPercent;
        for (T choice : choices) {
            this.probMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
        if (layers != this.layer + 1) {
            for (T t : this.probMap.keySet()) {
                this.children.put(t, new ProbFunTree<>(choices, layers, maxPercent, this));
            }
        }
    }

    private ProbFunTree(Set<T> choices, int layers, double maxPercent, ProbFunTree<T> parent) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (layers < 1) {
            throw new IllegalArgumentException("layers passed into the ProbFunTree constructor must be at least 1");
        }
        if (maxPercent <= 0 || maxPercent > 1.0) {
            throw new IllegalArgumentException("maxPercent passed into the ProbFunTree constructor must be above 0 and 1.0 or under");
        }
        // Invariants secured
        this.layer = parent.layer + 1;
        this.parent = parent;
        this.maxPercent = maxPercent;
        for (T choice : choices) {
            this.probMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
        if (this.layer + 1 < layers) {
            for (T t : this.probMap.keySet()) {
                this.children.put(t, new ProbFunTree<>(choices, layers, maxPercent, this));
            }
        }
    }

    /**
     * Private constructor for tracking parent nodes in the ProbFunTree.
     *
     * @param choices as the elements for the ProbFunTree to generate.
     * @param layers  as the number of layers to make the ProbFunTree.
     * @param parent  as the parent node in the ProbFunTree.
     * @throws NullPointerException     if choices is null.
     * @throws IllegalArgumentException if there isn't at least one element in choices, or
     *                                  layers and currentLayer are not at least 1.
     */
    private ProbFunTree(Set<T> choices, int layers, ProbFunTree<T> parent) {
        Objects.requireNonNull(choices);
        if (choices.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the choices passed to the ProbFunTree constructor\n");
        if (layers < 1) {
            throw new IllegalArgumentException("layers passed into the ProbFunTree constructor must be at least 1");
        }
        // Invariants secured
        this.layer = parent.layer + 1;
        this.parent = parent;
        this.maxPercent = 1.0;
        for (T choice : choices) {
            this.probMap.put(choice, 1.0 / choices.size());
        }
        fixProbSum();
        if (this.layer + 1 < layers) {
            for (T t : this.probMap.keySet()) {
                this.children.put(t, new ProbFunTree<>(choices, layers, this));
            }
        }
    }

	/**
     * Constructor for making a ProbFunTree under a parent ProbFunTree.
     *
     * @param probMap as the Object-probability pairs where the probabilities must add up to 1.0 using double addition.
     * @param layers  as the number of layers to make the ProbFunTree.
     * @param parent  as the parent node in the ProbFunTree.
     * @throws NullPointerException     if probMap is null.
     * @throws IllegalArgumentException if there isn't at least one entry in probMap,
     *                                  probMap entries do not add up to 1.0 using double addition, or
     *                                  layers or currentLayer are not at least 1.
     */
    private ProbFunTree(Map<T, Double> probMap, int layers, ProbFunTree<T> parent) {
        Objects.requireNonNull(probMap);
        if (probMap.size() < 1)
            throw new IllegalArgumentException("Must have at least 1 entry in the probMap passed to the ProbFunTree constructor\n");
        if (layers < 1) {
            throw new IllegalArgumentException("layers passed into the ProbFunTree constructor must be at least 1");
        }
        double sum = 0;
        for (double d : probMap.values()) {
            sum += d;
        }
        if (sum != 1.0) {
            throw new IllegalArgumentException("probMap values must add up to 1.0 using double addition "
                    + "when passed to the ProbFunTree constructor\n");
        }
        // Invariants secured
        this.layer = parent.layer + 1;
        this.parent = parent;
        this.maxPercent = 1.0;
        for (Entry<T, Double> choice : probMap.entrySet()) {
            this.probMap.put(choice.getKey(), choice.getValue());
        }
        fixProbSum();
        if (this.layer + 1 != layers) {
            for (T t : probMap.keySet()) {
                this.children.put(t, new ProbFunTree<>(probMap, layers, this));
            }
        }
    }

    /**
     * returns the Map of element-probability pairs that make up this ProbFunTree.
     * Any changes in the returned Map will be reflected in this ProbFunTree.
     *
     * @return the Map of element-probability pairs that make up this ProbFunTree.
     * Any changes in the returned Map will be reflected in this ProbFunTree.
     */
    public LinkedHashMap<T, Double> getProbMap() {
        return this.probMap;
    }

    public void setProbMap(LinkedHashMap<T, Double> probMap) {
        this.probMap = probMap;
    }

    /**
     * Scales the probabilities so they add up to 1.0.
     */
    private void scaleProbs() {
        double scale = 1.0 / probSum();
        Set<Entry<T, Double>> probabilities = this.probMap.entrySet();
        for (Entry<T, Double> e : probabilities) {
            e.setValue(e.getValue() * scale);
        }
        fixProbSum();
    }

    /**
     * @return the sum of all the probabilities in order to fix rounding error.
     */
    private double probSum() {
        Collection<Double> probabilities = this.probMap.values();
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
        Entry<T, Double> firstProb = this.probMap.entrySet().iterator().next();
        this.roundingError = 1.0 - probSum();
        firstProb.setValue(firstProb.getValue() + this.roundingError);
    }

    /**
     * Sets the probabilities to there being an equal chance of getting any element from this ProbFunTree.
     */
    public void clearProbs() {
        this.probMap = (new ProbFunTree<>(this.probMap.keySet(), 1)).probMap;
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
     * Propagation of past values determine the next value, therefore this method
     * clears the past values, but not the probabilities produced by feedback,
     * so the next generation is a sequence that starts from the first layer.
     */
    public void clearHistory() {
        this.previousElement = null;
        for (ProbFunTree<T> t : this.children.values()) {
            t.clearHistory();
        }
    }

    /**
     * Adds element to this ProbFunTree, making the probability equal to 1.0/n
     * where n is the number of elements contained in this ProbFunTree,
     * and appends elements as a child ProbFunTree, where elements are the choices.
     *
     * @param element  as the element to add to this ProbFunTree.
     * @param elements as the elements to be picked from after fun() returns element.
     *                 It may be empty or null if no elements should be picked from after fun() is called.
     *                 In this case, the probability function in this ProbFunTree will be used to generate
     *                 the next value based on the Objects it contains.
     * @throws NullPointerException if element is null.
     */
    public void add(T element, Set<T> elements) {
        Objects.requireNonNull(element);
        // Invariants secured
        double probability = 1.0 / (this.probMap.size());
        if (!this.probMap.containsKey(element)) {
            this.probMap.put(element, probability);
        } else {
            return;
        }
        scaleProbs();
        if (!this.children.containsKey(element) && elements != null && !elements.isEmpty()) {
            int layers = this.layer + 2;
            this.children.put(element, new ProbFunTree<>(elements, layers, this));
        } else if (this.children.containsKey(element) && elements != null && !elements.isEmpty()) {
            for (T t : elements) {
                this.children.get(element).add(t, null);
            }
        }
    }

    /**
     * Adds an element to this ProbFunTree with the specified probability.
     * If the element exists in this ProbFunTree then it's probability will be overwritten with percent.
     *
     * @param element  as the element to add to this ProbFunTree.
     * @param elements as the elements to be picked from after fun() returns element.
     *                 It may be empty or null if no elements should be picked from after fun() is called.
     *                 In this case, the probability function in this ProbFunTree will be used to generate
     *                 the next value based on the Objects it contains.
     * @param percent  between 0 and 1 exclusive, as the chance of this ProbFunTree returning element.
     * @throws NullPointerException     if element is null.
     * @throws IllegalArgumentException if percent is not between 0.0 and 1.0 (exclusive).
     */
    public void add(T element, Set<T> elements, double percent) {
        Objects.requireNonNull(element);
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException("percent passed to add() is not between 0.0 and 1.0 (exclusive)");
        }
        // Invariants secured
        double scale = (1.0 - percent);
        Set<Entry<T, Double>> probabilities = this.probMap.entrySet();
        for (Entry<T, Double> e : probabilities) {
            e.setValue(e.getValue() * scale);
        }
        this.probMap.put(element, percent);
        scaleProbs();
        if (!this.children.isEmpty() && !this.children.containsKey(element) && elements != null && !elements.isEmpty()) {
            int layers = this.layer + 2;
            this.children.put(element, new ProbFunTree<>(elements, layers, this));
            for (ProbFunTree<T> t : Objects.requireNonNull(this.children.get(element)).children.values()) {
                t.remove(element);
                t.add(element, elements, percent);
            }
        } else if (this.children.containsKey(element) && elements != null && !elements.isEmpty()) {
            for (T t : elements) {
                this.children.get(element).add(t, null, percent);
            }
        }
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
        if (parentSize() == 1) {
            return false;
        }
        // Invariants secured
        if (this.probMap.remove(element) == null) {
            return false;
        } else {
            this.children.remove(element);
        }
        scaleProbs();
        return true;
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
     * Adjust the probabilities to make the elements less likely to be returned when fun() is called
     * in the order they appear in elements.
     *
     * @param elements as the elements to make appear less often in the order they should not appear in.
     * @param percent  as the percentage between 0 and 1 (exclusive),
     *                 of the probabilities of getting the elements to subtract from the probabilities.
     * @throws NullPointerException     if elements is null.
     * @throws IllegalArgumentException if the percent isn't between 0 and 1 exclusive
     *                                  or elements is empty.
     */
    public void bad(List<T> elements, double percent) {
        Objects.requireNonNull(elements);
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("elements passed to bad() must not be empty");
        }
        if (percent >= 1.0 || percent <= 0.0) {
            throw new IllegalArgumentException("percent passed to bad() is not between 0.0 and 1.0 (exclusive)");
        }
        // Invariants secured
		List<T> l = new ArrayList<>(elements);
        bad(l.get(0), percent);
        if (l.size() == 1) {
            return;
        }
        this.children.get(l.remove(0)).bad(l, percent);
    }

    /**
     * Returns a randomly picked element from this ProbFunTree, based on the previously returned elements.
     *
     * @return a randomly picked element from this ProbFunTree.
     * Any changes in the element will be reflected in this ProbFunTree.
     */
    public T fun(Random random) {
        ArrayList<T> previousElements = new ArrayList<>();
        if (this.previousElement == null) {
            return nextValue(random);
        } else if (!this.children.isEmpty()) {
            previousElements.add(previousElement);
            ProbFunTree<T> pf = this.children.get(this.previousElement);
            T t = pf.previousElement;
            if (t == null && pf != null) {
                return pf.nextValue(random);
            } else if (t == null) {
                return this.nextValue(random);
            }
            while (t != null) {
                previousElements.add(t);
                if (!pf.children.isEmpty()) {
                    pf = pf.children.get(t);
                    t = pf.previousElement;
                } else {
                    t = null;
                }
            }
            Iterator<T> it = previousElements.iterator();
            if (it.hasNext()) {
                this.previousElement = it.next();
                pf = this.children.get(this.previousElement);
                while (it.hasNext()) {
                    pf.previousElement = it.next();
                    pf = pf.children.get(pf.previousElement);
                }
                if (pf == null) {
                    clearHistory();
                    this.previousElement = previousElements.get(previousElements.size() - 1);
                    return this.nextValue(random);
                }
                return pf.fun(random);
            } else {
                return this.children.get(this.previousElement).fun(random);
            }
        } else {
            return nextValue(random);
        }
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
        this.previousElement = element;
        return element;
    }

    /**
     * Returns the number of elements in this ProbFunTree.
     *
     * @return the number of elements in this ProbFunTree.
     */
    public int parentSize() {
        return this.probMap.size();
    }


    /**
     * Private copy constructor for clone
     *
     * @param probFunTree as the ProbFunTree to copy
     */
    private ProbFunTree(ProbFunTree<T> probFunTree) {
        for (Entry<T, Double> s : probFunTree.probMap.entrySet()) {
            this.probMap.put(s.getKey(), s.getValue());
        }
        for (Entry<T, ProbFunTree<T>> e : probFunTree.children.entrySet()) {
            this.children.put(e.getKey(), e.getValue().clone());
        }
        if (!this.children.isEmpty()) {
            for (ProbFunTree<T> e : this.children.values()) {
                e.parent = this;
            }
        }
    }

    @NonNull
	@Override
    public ProbFunTree<T> clone() {
        return new ProbFunTree<>(this);
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
            sb.append("%]");
        }
        if (!this.children.isEmpty()) {
            sb.append("]\n");
            for (int i = 0; i < 15; i++) {
                sb.append(" ");
            }
        }
        if (this.parent != null) {
            for (int i = 0; i < 31; i++) {
                sb.append(" ");
            }
        }
        sb.append("Children: [");
        int count = 0;
        for (Entry<T, ProbFunTree<T>> e : this.children.entrySet()) {
            count++;
            sb.append("[");
            sb.append(e.getKey());
            sb.append(" = ");
            sb.append(e.getValue());
            sb.delete(sb.length() - 1, sb.length());
            sb.append("]\n");
            if (this.parent != null && this.parent.parent != null && count == this.children.size()) {
                sb.delete(sb.length() - 2, sb.length());
                sb.append("\n");
            }
            for (int i = 0; i < 26; i++) {
                sb.append(" ");
            }
            if (this.parent != null) {
                for (int i = 0; i < 31; i++) {
                    sb.append(" ");
                }
            }
        }
        if (this.children.isEmpty()) {
            sb.delete(sb.length() - 11, sb.length());

            if (this.parent != null) {
                sb.delete(sb.length() - 31, sb.length());
            }
            sb.append("]");

            sb.append("\n");
        } else {
            sb.delete(sb.length() - 28, sb.length());
            if (this.parent != null) {
                sb.delete(sb.length() - 31, sb.length());
            }
            sb.append("]]");
            sb.append("\n");
        }
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
        return this.id - ((ProbFunTree<T>) o).id;
    }

}