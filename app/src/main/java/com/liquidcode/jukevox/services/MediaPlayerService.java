package com.liquidcode.jukevox.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.util.MessageIntents;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by mikev on 11/18/2016.
 */
public class MediaPlayerService extends Service {
    // List of songs that we want to play
    private ArrayList<Song> m_songList = null;
    // the current song index in the list of songs
    private int m_currentSongIndex = -1;
    // media player object
    private MediaPlayer m_mediaPlayer = null;
    // our binder we use as an interface to the fragment
    private MediaBinder m_mediaBinder = new MediaBinder();
    private boolean m_wasPlaying = false;
    // our broadcast     receiver for intents we might get
    private BroadcastReceiver m_receiver = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(m_mediaBinder == null) {
            m_mediaBinder = new MediaBinder();
        }
        return m_mediaBinder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();
        createMediaPlayer();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(), "Media Player Service Destroyed!", Toast.LENGTH_LONG).show();
        m_mediaPlayer.stop();
        m_mediaPlayer.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {

        return Service.START_STICKY;
    }

    private boolean createMediaPlayer() {
        boolean created = true;
        if(m_mediaPlayer == null) {
            m_mediaPlayer = new MediaPlayer();
        }
        // set callback methods
        m_mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(m_wasPlaying) {
                    onSongFinished();
                }
            }
        });
        return created;
    }

    public Song getCurrentSong() {
        return m_songList.get(m_currentSongIndex);
    }

    // helper functions for skipping songs and pause/play
    public void previousSong(boolean isPaused)
    {
        // stop the current song
        m_mediaPlayer.reset();
        if(m_currentSongIndex > 0)
        {
            m_currentSongIndex -= 1;
            startSong(isPaused);
        }
        else
        {
            // start back at the last song
            m_currentSongIndex = m_songList.size()-1;
            startSong(isPaused);
        }
        Log.d("MPS", "Current Song Index: " + m_currentSongIndex);
        // send our broadcast
        Intent prevSongIntent = new Intent(MessageIntents.PREV_SONG_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(prevSongIntent);
    }

    public void nextSong(boolean isPaused)
    {
        // stop the current song
        m_mediaPlayer.reset();
        if(m_currentSongIndex < m_songList.size()-1)
        {
            m_currentSongIndex += 1;
            startSong(isPaused);
        }
        else
        {
            // start back at 0
            m_currentSongIndex = 0;
            startSong(isPaused);
        }
        Log.d("MPS", "Current Song Index: " + m_currentSongIndex);
        // send our broadcast
        Intent nextSongIntent = new Intent(MessageIntents.NEXT_SONG_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(nextSongIntent);
    }

    public boolean startSong(boolean isPaused) {
        long id = m_songList.get(m_currentSongIndex).id;
        // set up the file to be played
        Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            // create the audio attributes
            m_mediaPlayer.setDataSource(getApplicationContext(), fileUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            m_mediaPlayer.prepare();
            if(!isPaused) {
                m_mediaPlayer.start();
                m_wasPlaying = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to start media", Toast.LENGTH_SHORT).show();
            return false;
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to start media", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void onSongFinished()
    {
        if(m_currentSongIndex < m_songList.size()-1)
        {
            m_mediaPlayer.reset();
            m_currentSongIndex += 1;
            startSong(false); // now start the next one
        }
    }

    public void stopSong()
    {
        m_mediaPlayer.stop();
        m_mediaPlayer.reset();
    }

    public void pauseMediaPlayback() {
        if(m_mediaPlayer != null) {
            m_mediaPlayer.pause();
        }
    }

    public void continueMediaPlayback() {
        if(m_mediaPlayer != null) {
            m_mediaPlayer.start();
            m_wasPlaying = true;
        }
    }

    public void setCurrentSongIndex(int songIndex) {
        m_currentSongIndex = songIndex;
    }

    public int getCurrentSongIndex() {
        return m_currentSongIndex;
    }
    public MediaPlayer getMediaPlayer() {
        return m_mediaPlayer;
    }

    public void initMediaPlayer(ArrayList<Song> songList) {
        m_songList = songList;
    }

    private void killSelf() {
        m_mediaPlayer.stop();
        m_mediaPlayer.release();
        stopSelf();
    }

    private void initBroadcastReceiver() {
        if(m_receiver == null) {
            m_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // check which action we are handling
                    switch(intent.getAction()) {
                        case MessageIntents.KILL_SERVICE:
                            killSelf();
                            Toast.makeText(getApplicationContext(), "Killing Service", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };
        }
    }

    /**
     * Extend the IBinder class so we can talk to our calling fragment
     */
    public class MediaBinder extends Binder {
        public MediaPlayerService getMediaPlayerService() {
            return MediaPlayerService.this;
        }
    }
}
