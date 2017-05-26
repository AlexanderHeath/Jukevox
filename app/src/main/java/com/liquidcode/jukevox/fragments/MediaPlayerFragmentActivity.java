package com.liquidcode.jukevox.fragments;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.util.MessageIntents;

import java.util.ArrayList;

public class MediaPlayerFragmentActivity extends Activity {
    private MediaPlayerFragment m_mediaPlayerFragment = null;
    private String m_mediaPlayerTag = null;
    public MediaPlayerFragmentActivity() {
        super();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // set the content view
        setContentView(R.layout.media_player);
        Intent extras = getIntent();
        // extras variables
        ArrayList<Song> songList = null;
        String albumName = null;
        String artistName = null;
        int currentSongIndex = -1;
        Uri albumImageUri = null;
        if(extras != null) {
            songList = extras.getParcelableArrayListExtra("song_list");
            albumName = extras.getStringExtra("album_name");
            artistName = extras.getStringExtra("artist_name");
            currentSongIndex = extras.getIntExtra("current_song_index", -1);
            albumImageUri = Uri.parse(extras.getStringExtra("album_uri"));
        }
        // set the song list
        if(m_mediaPlayerFragment == null) {
            m_mediaPlayerFragment = new MediaPlayerFragment();
        }
        MediaPlayerFragment existing = (MediaPlayerFragment)getFragmentManager().findFragmentById(R.id.mediaPlayerContainer);
        if(existing != null) {

        }
        // generate the tag for this media player instance
        // replace " " with "_" in album name
        albumName = albumName.replace(' ', '_');
        m_mediaPlayerTag = "mpf_" + albumName;
        m_mediaPlayerFragment.initMediaPlayer(songList, artistName, currentSongIndex, albumImageUri, getApplicationContext());
        FragmentTransaction ft =  getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.add(R.id.mediaPlayerContainer, m_mediaPlayerFragment, m_mediaPlayerTag);
        ft.commit();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
