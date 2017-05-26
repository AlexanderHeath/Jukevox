package com.liquidcode.jukevox.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.LibraryLoader;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.adapters.ArtistListAdapter;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.util.ResultCode;

import java.util.ArrayList;

/**
 * Created by mikev on 5/11/2017.
 */

public class LibraryFragment extends Fragment {
    private LibraryLoader m_libLoader = null;
    // request code for reading external storage
    private final int REQUEST_EXTERNAL_READ = 0;
    // boolean if the request was granted
    private boolean m_readExternalGranted = false;
    private ViewGroup m_rootView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        m_rootView = (ViewGroup) inflater.inflate(
                R.layout.library_main_layout, container, false);
        if(m_rootView != null) {
            // request external storage reading
            requestExternalStorageRead();
        }

        return m_rootView;
    }

    /*
        Initializes and creates the ArtistListAdapter
        Sets up the onChildClick for the expandable list that will create the SongListFragment
     */
    public void InitializeArtistListAdapter(ViewGroup root) {
        ArtistListAdapter artistAdapter = new ArtistListAdapter(m_libLoader, getActivity());
        ExpandableListView expandableBase = (ExpandableListView)root.findViewById(R.id.artistListView);
        if (expandableBase != null) {
            expandableBase.setAdapter(artistAdapter);
            expandableBase.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                    // get the list of songs from this artist/album
                    ArrayList<Song> songList = m_libLoader.getArtists().get(i).getAlbums().get(i1).songList;
                    String albumName = m_libLoader.getArtists().get(i).getAlbums().get(i1).albumName;
                    String artistName = m_libLoader.getArtists().get(i).artist;
                    Uri albumImageUri = m_libLoader.getArtists().get(i).getAlbums().get(i1).albumArt;
                    // tell the activity to replace with songListFragment
                    ((JukevoxMain)getActivity()).createSongListFragment(songList, albumName, artistName, albumImageUri);
                    return true;
                }
            });
        }
    }

    private void requestExternalStorageRead() {
        // todo? wrap this in API level check?
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_READ);
            } else {
                // we have permission read the external storage
                m_readExternalGranted = true;
                if (m_libLoader == null) {
                    // read music library
                    if (InitializeLibraryLoader() == ResultCode.RESULT_SUCCESS && m_readExternalGranted) {
                        if (m_libLoader.prepare() == ResultCode.RESULT_SUCCESS) {
                            InitializeArtistListAdapter(m_rootView);
                        }
                    }
                }
            }
        } else {
            if (m_libLoader == null) {
                m_readExternalGranted = true;
                // read music library
                if (InitializeLibraryLoader() == ResultCode.RESULT_SUCCESS && m_readExternalGranted) {
                    if (m_libLoader.prepare() == ResultCode.RESULT_SUCCESS) {
                        InitializeArtistListAdapter(m_rootView);
                    }
                }
            }
        }
    }

    /**
     * Checks the result of our permissions request
     **/
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_READ: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    m_readExternalGranted = true;
                    if (m_libLoader == null) {
                        // read music libray
                        if (InitializeLibraryLoader() == ResultCode.RESULT_SUCCESS && m_readExternalGranted) {
                            if (m_libLoader.prepare() == ResultCode.RESULT_SUCCESS) {
                                InitializeArtistListAdapter(m_rootView);
                            }

                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes the library loader and reads the users phone for media
     * Creates lists and sorts.
     */
    private ResultCode InitializeLibraryLoader() {
        if (m_libLoader == null) {
            m_libLoader = new LibraryLoader(getActivity().getContentResolver());
        }
        return ResultCode.RESULT_SUCCESS;
    }

}
