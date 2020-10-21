package com.example.waveplayer;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
public class RandomPlaylistTreeMap extends RandomPlaylist implements Serializable {

    private static final long serialVersionUID = 2323326608918863420L;

    /**
     * Creates a random playlist.
     *
     * @param music as the List of AudioURIs to add to this playlist.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    public RandomPlaylistTreeMap(List<AudioURI> music, double maxPercent, String name) {
        if (music.isEmpty())
            throw new IllegalArgumentException("List music must contain at least one AudioURI");
        Set<AudioURI> files = new LinkedHashSet<>(music);
        probabilityFunction = new ProbFunTreeMap<>(files, maxPercent);
        this.name = name;
    }

}