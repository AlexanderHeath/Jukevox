package com.liquidcode.jukevox.fragments;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.services.MediaPlayerService;
import com.liquidcode.jukevox.util.MessageIntents;

import java.util.ArrayList;

/**
 * Created by mikev on 11/11/2016.
 */
public class SongFragmentActivity extends Activity {
    private SongFragment m_songFragment = null;

    public SongFragmentActivity() {
        super();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.fragment_song_list);
        // get the extras
        Intent extra = getIntent();
        ArrayList<Song> songList = null;
        String albumName = null;
        String artistName = null;
        Uri albumImageUri = null;
        if(extra != null) {
            songList = extra.getParcelableArrayListExtra("song_list");
            albumName = extra.getStringExtra("album_name");
            artistName = extra.getStringExtra("artist_name");
            albumImageUri = Uri.parse(extra.getStringExtra("album_uri"));
        }
        // set the song list
        if(m_songFragment == null) {
            m_songFragment = new SongFragment();
        }
        MediaPlayerFragment existing = (MediaPlayerFragment)getFragmentManager().findFragmentById(R.id.mediaPlayerContainer);
        if(existing != null) {

        }
        m_songFragment.initSongInfo(songList, albumName, artistName, albumImageUri, getApplicationContext());
//        FragmentTransaction ft =  getFragmentManager().beginTransaction();
//        ft.add(R.id.songFragmentContainer, m_songFragment, "Song Fragment")
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
//                .commit();
        // create the media player service
        Intent mediaPlayerIntent = new Intent(this, MediaPlayerService.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister here
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
