package com.liquidcode.jukevox.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.adapters.SongListAdapter;
import com.liquidcode.jukevox.musicobjects.Song;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SongFragment extends Fragment {

    private ArrayList<Song> m_songList = null;
    private Context m_context = null;
    private String m_albumName = null;
    private String m_artistName = null;
    private Uri m_albumArtUri = null;
    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private MediaPlayerFragmentActivity m_mediaPlayerFragmentActivity = null;
    OnSongSelectedListener mSongSelectedListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mSongSelectedListener = (OnSongSelectedListener)context;
        }
        catch(ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement OnSongSelectedListener!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song_list, container, false);
        ListView list = (ListView)view.findViewById(R.id.songList);
        if(list != null) {
            list.setAdapter(new SongListAdapter(m_songList, m_context));
            // set the onItemClick listener to start the MediaPlayer
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // create the intent to start the MediaPlayer
                    if(m_mediaPlayerFragmentActivity == null) {
                        m_mediaPlayerFragmentActivity = new MediaPlayerFragmentActivity();
                    }
                    Intent mediaPlayerIntent = new Intent(getActivity(), m_mediaPlayerFragmentActivity.getClass());
//                    mediaPlayerIntent.putParcelableArrayListExtra("song_list", m_songList);
//                    mediaPlayerIntent.putExtra("album_name", m_albumName);
//                    mediaPlayerIntent.putExtra("artist_name", m_artistName);
//                    mediaPlayerIntent.putExtra("current_song_index", i);
//                    mediaPlayerIntent.putExtra("album_uri", m_albumArtUri.toString());
//                    startActivity(mediaPlayerIntent);
                    mSongSelectedListener.onSongSelected(m_artistName, m_songList.get(i));
                }
            });
        }
        // set the album name
        TextView albumText = (TextView)view.findViewById(R.id.songListAlbum);
        if(!m_albumName.isEmpty()) {
            albumText.setText(m_albumName);
        }
        else {
            albumText.setText(R.string.unable_load_album);
        }
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(new SongListAdapter(m_songList, m_context));
        }
        return view;
    }

    public void initSongInfo(ArrayList<Song> songList, String albumName, String artistName, Uri albumArtUri, Context context) {
        m_songList = songList;
        m_context = context;
        m_albumName = albumName;
        m_artistName = artistName;
        m_albumArtUri = albumArtUri;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // Container Activity must implement this interface
    public interface OnSongSelectedListener {
        void onSongSelected(String artist, Song songData);
    }

}
