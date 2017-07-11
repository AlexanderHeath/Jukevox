package com.liquidcode.jukevox.networking;

import com.liquidcode.jukevox.musicobjects.Song;

/**
 * This class is responsible for streaming the song data to the server in chunks.
 * It keeps track of the current amount of data sent.
 * Created by mikev on 7/2/2017.
 */

public class StreamingThread extends Thread {
    private boolean m_running = false;
    // Variables that keep track of the clients current song its streaming to the server
    private int m_currentPosition; // how much data we've sent so far
    private int m_maxSongLength; // how much data this song is
    private Song m_currentSong;

    public StreamingThread(Song song) {
        m_currentPosition = 0;
        m_maxSongLength = 0;
        m_currentSong = song;
    }

    public void run() {
        // start running the thread until we get stopped
        m_running = true;
        while(m_running) {
            // do stuff
        }
    }

    public void cancel() {
        m_running = false;
    }

}
