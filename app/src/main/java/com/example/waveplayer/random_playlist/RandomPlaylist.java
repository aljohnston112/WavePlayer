package com.example.waveplayer.random_playlist;

import android.content.Context;

import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
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

    /**
     * Creates a random playlist.
     *
     * @param name            The name of this RandomPlaylist.
     * @param music           The List of AudioURIs to add to this playlist.
     * @param maxPercent      The max percentage that any AudioUri can have
     *                        of being returned when fun() is called.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    public RandomPlaylist(String name, List<Song> music, double maxPercent,
                          boolean comparable) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<Song> files = new LinkedHashSet<>(music);
        if (comparable) {
            probabilityFunction = new ProbFunTreeMap<>(files, maxPercent);
        } else {
            probabilityFunction = new ProbFunLinkedMap<>(files, maxPercent);
        }
        this.name = name;
        for (Song song : music) {
            playlistArray.add(song.id);
        }
        playlistIterator = playlistArray.listIterator();
    }

    public List<Song> getSongs() {
        return probabilityFunction.getKeys();
    }

    public List<Long> getSongIDs(){
        return playlistArray;
    }

    public void add(Song song) {
        probabilityFunction.add(song);
        playlistArray.add(song.id);
    }

    public void add(Song song, double probability) {
        probabilityFunction.add(song, probability);
        playlistArray.add(song.id);
    }

    public void remove(Song song) {
        probabilityFunction.remove(song);
        playlistArray.remove(song.id);
    }

    public boolean contains(Song song) {
        return probabilityFunction.contains(song);
    }

    public boolean contains(Long songID) {
        return playlistArray.contains(songID);
    }

    public void setMaxPercent(double maxPercent) {
        probabilityFunction.setMaxPercent(maxPercent);
    }

    public void good(Context context, Song song, double percent, boolean scale) {
        if (MediaData.getAudioUri(context, song.id).good(percent)) {
            probabilityFunction.good(song, percent, scale);
        }
    }

    public void bad(Context context, Song song, double percent) {
        if (MediaData.getAudioUri(context, song.id).bad(percent)) {
            probabilityFunction.bad(song, percent);
        }

    }

    public double getProbability(Song song) {
        return probabilityFunction.getProbability(song);
    }

    public void clearProbabilities(Context context) {
        for (Song song : probabilityFunction.getKeys()) {
            MediaData.getAudioUri(context, song.id).clearProbabilities();
        }
        probabilityFunction.clearProbabilities();
    }

    public void lowerProbabilities(Context context, double lowerProb) {
        probabilityFunction.lowerProbs(lowerProb);
    }

    public AudioUri next(Context context, Random random) {
        Song song;
        AudioUri audioUri = null;
        boolean next = false;
        while (!next) {
            song = probabilityFunction.fun(random);
            audioUri = MediaData.getAudioUri(context, song.id);
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

    public AudioUri next(Context context, Random random, boolean looping, boolean shuffling) {
        if (playlistIterator == null) {
            playlistIterator = playlistArray.listIterator();
        }
        if (shuffling) {
            return next(context, random);
        } else {
            if (looping && !playlistIterator.hasNext()) {
                playlistIterator = playlistArray.listIterator();
            }
            return MediaData.getAudioUri(context, playlistIterator.next());
        }
    }

    public AudioUri previous(Context context, Random random, boolean looping, boolean shuffling) {
        if (playlistIterator == null) {
            playlistIterator = playlistArray.listIterator();
        }
        if (shuffling) {
            return next(context, random);
        } else {
            if (looping && !playlistIterator.hasPrevious()) {
                playlistIterator = playlistArray.listIterator(playlistArray.size()-1);
            }
            return MediaData.getAudioUri(context, playlistIterator.previous());
        }

    }

    /*
    public void goToFront() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator();
    }

    public void goToBack() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator(playlistArray.size());
    }

     */

    public void setIndexTo(Long songID) {
        if (playlistArray != null) {
            int i = playlistArray.indexOf(songID);
            playlistIterator = playlistArray.listIterator(i + 1);
        }
    }

    public void globalBad(Song song, double percentChangeDown) {
        probabilityFunction.bad(song, percentChangeDown);
    }
}