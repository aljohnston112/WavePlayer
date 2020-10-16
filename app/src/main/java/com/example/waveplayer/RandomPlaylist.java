package com.example.waveplayer;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
public class RandomPlaylist implements Serializable {

    private static final long serialVersionUID = 2323326608918863420L;

    private final String name;

    // The ProbFun that randomly picks the media to play
    private final ProbFunTree<AudioURI> probabilityFunction;

    /**
     * Creates a random playlist.
     *
     * @param music as the List of AudioURIs to add to this playlist.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    public RandomPlaylist(List<AudioURI> music, double maxPercent, String name) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<AudioURI> files = new HashSet<>(music);
        probabilityFunction = new ProbFunTree<>(files, maxPercent);
        this.name = name;
    }

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    /**
     * @return the ProbFunTree that controls the probability of which media gets played.
     */
    public ProbFunTree<AudioURI> getProbFun() {
        return probabilityFunction;
    }

    public String getName(){
        return name;
    }

}