package com.example.waveplayer.random_playlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
public class RandomPlaylist implements Serializable {

    private static final long serialVersionUID = 2323326608918863420L;

    private ArrayList<Long> playlistArray;
    private ListIterator<Long> playlistIterator;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // The ProbFun that randomly picks the media to play
    private final ProbFun<Long> probabilityFunction;

    public final long mediaStoreUriID;

    /**
     * Creates a random playlist.
     *
     * @param name            The name of this RandomPlaylist.
     * @param music           The List of AudioURIs to add to this playlist.
     * @param maxPercent      The max percentage that any AudioUri can have
     *                        of being returned when fun() is called.
     * @param mediaStoreUriID The UriID used by the MediaStore.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    public RandomPlaylist(String name, List<Long> music, double maxPercent,
                          boolean comparable, long mediaStoreUriID) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<Long> files = new LinkedHashSet<>(music);
        if (comparable) {
            probabilityFunction = new ProbFunTreeMap<>(files, maxPercent);
        } else {
            probabilityFunction = new ProbFunLinkedMap<>(files, maxPercent);
        }
        this.name = name;
        this.mediaStoreUriID = mediaStoreUriID;
        playlistIterator = playlistArray.listIterator();
    }

    public ArrayList<Long> getSongIDs() {
        return probabilityFunction.getKeys();
    }

    public void add(Long songID) {
        probabilityFunction.add(songID);
    }

    public void add(Long songID, double probability) {
        probabilityFunction.add(songID, probability);
    }

    public void remove(Long songID) {
        probabilityFunction.remove(songID);
    }

    public boolean contains(Long songID) {
        return probabilityFunction.contains(songID);
    }

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    public void good(Long songID, double percent, boolean scale) {
        /*
        if(songID.good(percent, scale)) {
            probabilityFunction.good(songID, percent, scale);
        }
                // TODO when AudioUris are in files
         */
    }

    public void bad(Long songID, double percent) {
        /*
        if(songID.bad(percent)) {
            probabilityFunction.bad(songID, percent);
        }
        // TODO when AudioUris are in files
         */
    }

    public double getProbability(Long songID) {
        return probabilityFunction.getProbability(songID);
    }

    public void clearProbabilities() {
        /*
        for(Long songID : probabilityFunction.getKeys()){
            songID.clearProbabilities();
        }
        probabilityFunction.clearProbabilities();
        // TODO when AudioUris are in files
         */
    }

    public AudioUri next(Random random) {
        /*
        AudioUri audioUri = null;
        boolean next = false;
        while(!next) {
            audioUri = probabilityFunction.fun(random);
            next = audioUri.shouldPlay(random);
        }
        return audioUri;
                // TODO when AudioUris are in files
         */
        return null;
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

    public boolean hasNext() {
        return playlistIterator.hasNext();
    }

    public Long next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return playlistIterator.next();
    }

    public boolean hasPrevious() {
        return playlistIterator.hasPrevious();
    }

    public Long previous() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        return playlistIterator.previous();
    }

    public void goToFront() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator();
    }

    public void goToBack() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator(playlistArray.size());
    }

    public void setIndexTo(Long songID) {
        if (playlistArray != null) {
            int i = playlistArray.indexOf(songID);
            playlistIterator = playlistArray.listIterator(i + 1);
        }
    }


}