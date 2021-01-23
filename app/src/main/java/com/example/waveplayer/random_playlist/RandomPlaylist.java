package com.example.waveplayer.random_playlist;

import android.content.Context;

import com.example.waveplayer.AudioFileLoader;
import com.example.waveplayer.Song;

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

    private final List<Long> playlistArray = new ArrayList<>();

    transient private ListIterator<Long> playlistIterator;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // The ProbFun that randomly picks the media to play
    private final ProbFun<Song> probabilityFunction;

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
    public RandomPlaylist(String name, List<Song> music, double maxPercent,
                          boolean comparable, long mediaStoreUriID) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<Song> files = new LinkedHashSet<>(music);
        if (comparable) {
            probabilityFunction = new ProbFunTreeMap<>(files, maxPercent);
        } else {
            probabilityFunction = new ProbFunLinkedMap<>(files, maxPercent);
        }
        this.name = name;
        this.mediaStoreUriID = mediaStoreUriID;
        for(Song song : music){
            playlistArray.add(song.id);
        }
        playlistIterator = playlistArray.listIterator();
    }

    public List<Song> getSongs() {
        return probabilityFunction.getKeys();
    }

    public void add(Song song) {
        probabilityFunction.add(song);
    }

    public void add(Song song, double probability) {
        probabilityFunction.add(song, probability);
    }

    public void remove(Song song) {
        probabilityFunction.remove(song);
    }

    public boolean contains(Song song) {
        return probabilityFunction.contains(song);
    }

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    public void good(Context context, Song song, double percent, boolean scale) {
        if(AudioFileLoader.getAudioUri(context, song.id).good(percent, scale)) {
            probabilityFunction.good(song, percent, scale);
        }
    }

    public void bad(Context context, Song song, double percent) {
        if(AudioFileLoader.getAudioUri(context, song.id).bad(percent)) {
            probabilityFunction.bad(song, percent);
        }

    }

    public double getProbability(Song song) {
        return probabilityFunction.getProbability(song);
    }

    public void clearProbabilities(Context context) {
        for(Song song : probabilityFunction.getKeys()){
            AudioFileLoader.getAudioUri(context, song.id).clearProbabilities();
        }
        probabilityFunction.clearProbabilities();
    }

    public AudioUri next(Context context, Random random) {
        Song song;
        AudioUri audioUri = null;
        boolean next = false;
        while(!next) {
            song = probabilityFunction.fun(random);
            audioUri = AudioFileLoader.getAudioUri(context, song.id);
            next = audioUri.shouldPlay(random);
        }
        return audioUri;
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
        if(playlistIterator == null){
            playlistIterator = playlistArray.listIterator();
        }
        return playlistIterator.hasNext();
    }

    public Long next() {
        if(playlistIterator == null){
            playlistIterator = playlistArray.listIterator();
        }
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return playlistIterator.next();
    }

    public boolean hasPrevious() {
        if(playlistIterator == null){
            playlistIterator = playlistArray.listIterator();
        }
        return playlistIterator.hasPrevious();
    }

    public Long previous() {
        if(playlistIterator == null){
            playlistIterator = playlistArray.listIterator();
        }
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