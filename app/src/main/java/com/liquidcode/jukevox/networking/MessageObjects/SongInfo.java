package com.liquidcode.jukevox.networking.MessageObjects;

/**
 * SongInfo.java
 * Artist/song name that is wrapped in a object
 * Built from incoming socket byte[]
 * Created by mikev on 5/26/2017.
 */
public class SongInfo {
    private String m_artist;
    private String m_songName;

    public SongInfo(String artist, String song) {
        m_artist = artist;
        m_songName = song;
    }

    public String getArtist() {
        return m_artist;
    }

    public String getSongName() {
        return m_songName;
    }
}
