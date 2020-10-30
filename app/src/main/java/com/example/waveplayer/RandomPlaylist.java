package com.example.waveplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
public class RandomPlaylist implements Serializable {

    private static final long serialVersionUID = 2323326608918863420L;

    private String name;
    public String getName(){
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // The ProbFun that randomly picks the media to play
    private final ProbFun<AudioUri> probabilityFunction;

    public final long mediaStoreUriID;

    /**
     * Creates a random playlist.
     *
     * @param name                      The name of this RandomPlaylist.
     * @param music                     The List of AudioURIs to add to this playlist.
     * @param maxPercent                The max percentage that any AudioUri can have
     *                                  of being returned when fun() is called.
     * @param mediaStoreUriID           The UriID used by the MediaStore.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    public RandomPlaylist(String name, List<AudioUri> music, double maxPercent,
                          boolean comparable, long mediaStoreUriID) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<AudioUri> files = new LinkedHashSet<>(music);
        if(comparable){
            probabilityFunction = new ProbFunTreeMap<>(files, maxPercent);
        } else {
            probabilityFunction = new ProbFunLinkedMap<>(files, maxPercent);
        }
        this.name = name;
        this.mediaStoreUriID = mediaStoreUriID;
    }

    public ArrayList<AudioUri> getAudioUris(){
        return probabilityFunction.getKeys();
    }

    public void add(AudioUri audioUri){
        probabilityFunction.add(audioUri);
    }

    public void add(AudioUri audioURI, double probability) {
        probabilityFunction.add(audioURI, probability);
    }

    public void remove(AudioUri audioUri){
        probabilityFunction.remove(audioUri);
    }

    public boolean contains(AudioUri audioUri){
        return probabilityFunction.contains(audioUri);
    }

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    public void good(AudioUri audioUri, double percent) {
        probabilityFunction.good(audioUri, percent);
    }

    public void bad(AudioUri audioUri, double percent) {
        probabilityFunction.bad(audioUri, percent);
    }

    public double getProbability(AudioUri audioUri){
        return probabilityFunction.getProbability(audioUri);
    }

    public void clearProbabilities(){
        probabilityFunction.clearProbabilities();
    }

    public AudioUri next(Random random) {
        return probabilityFunction.fun(random);
    }

    public int size() {
        return probabilityFunction.size();
    }

    public void swapSongPositions(int oldPosition, int newPosition) {
        probabilityFunction.swapPositions(oldPosition, newPosition);
    }

    public void switchSongPositions(int oldPosition, int newPosition) {
        probabilityFunction.switchPositions(oldPosition, newPosition);
    }

}