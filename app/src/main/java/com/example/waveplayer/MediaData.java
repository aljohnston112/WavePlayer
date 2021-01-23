package com.example.waveplayer;

import android.content.Context;

import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MediaData {

    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    private final HashMap<Long, MediaPlayerWUri> songIDToMediaPlayerWUriHashMap = new HashMap<>();

    private Settings settings = new Settings(0.1, 0.1, 0.5);

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public Settings getSettings() {
        return settings;
    }

    private RandomPlaylist masterPlaylist;

    public void setMasterPlaylist(RandomPlaylist masterPlaylist) {
        this.masterPlaylist = masterPlaylist;
    }

    public RandomPlaylist getMasterPlaylist() {
        return masterPlaylist;
    }

    public List<Song> getAllSongs() {
        return masterPlaylist.getSongs();
    }

    private final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    public ArrayList<RandomPlaylist> getPlaylists() {
        return playlists;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        playlists.add(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        playlists.add(position, randomPlaylist);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        playlists.remove(randomPlaylist);
    }

    public static MediaData INSTANCE;

    private MediaData(Context context) {
        SaveFile.loadSaveFile(context, this);
    }

    public static MediaData getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MediaData(context);
        }
        return INSTANCE;
    }

    public double getMaxPercent() {
        return settings.maxPercent;
    }

    public void setMaxPercent(double maxPercent) {
        masterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : playlists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        settings = new Settings(maxPercent, settings.percentChangeUp, settings.percentChangeDown);
    }

    public void setPercentChangeUp(double percentChangeUp) {
        settings = new Settings(settings.maxPercent, percentChangeUp, settings.percentChangeDown);

    }

    public double getPercentChangeUp() {
        return settings.percentChangeUp;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        settings = new Settings(settings.maxPercent, settings.percentChangeUp, percentChangeDown);
    }

    public double getPercentChangeDown() {
        return settings.percentChangeDown;
    }

    MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return songIDToMediaPlayerWUriHashMap.get(songID);
    }

    public void addMediaPlayerWUri(Long id, MediaPlayerWUri mediaPlayerWURI) {
        songIDToMediaPlayerWUriHashMap.put(id, mediaPlayerWURI);
    }

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWUri> iterator = songIDToMediaPlayerWUriHashMap.values().iterator();
            MediaPlayerWUri mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
    }

    private void removeMissingSongs(List<Song> newSongs) {
        for (Song song : masterPlaylist.getSongs()) {
            if (!newSongs.contains(song)) {
                masterPlaylist.remove(song);
                songIDToMediaPlayerWUriHashMap.remove(song.id);
            }
        }
    }

    private void addNewSongs(List<Song> newSongs) {
        for (Song newSong : newSongs) {
            if (newSong != null) {
                if (!masterPlaylist.contains(newSong)) {
                    masterPlaylist.add(newSong);
                }
            }
        }
    }

    void loadMediaFiles(List<Song> newUris) {
        if (newUris != null) {
            if (masterPlaylist != null) {
                addNewSongs(newUris);
                removeMissingSongs(newUris);
            } else {
                masterPlaylist = new RandomPlaylist(
                        MASTER_PLAYLIST_NAME, new ArrayList<>(newUris),
                        settings.maxPercent, true, -1);
            }
        }
    }

}