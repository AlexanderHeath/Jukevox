package com.liquidcode.jukevox.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.liquidcode.jukevox.LibraryLoader;
import com.liquidcode.jukevox.R;

/**
 * Created by mikev on 10/30/2016.
 */

public class ArtistListAdapter extends BaseExpandableListAdapter {

    private LibraryLoader m_libraryLoader = null;
    private Context m_context = null;

    // ctor with a reference to the library loader
    public ArtistListAdapter(LibraryLoader libLoader, Context context) {
        super();
        m_libraryLoader = libLoader;
        m_context = context;
    }

    @Override
    public int getGroupCount() {
        return m_libraryLoader.getArtists().size();
    }

    @Override
    public int getChildrenCount(int i) {
        return m_libraryLoader.getArtists().get(i).albumList.size();
    }

    @Override
    public Object getGroup(int i) {
        return m_libraryLoader.getArtists().get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return m_libraryLoader.getArtists().get(i).albumList.get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return 0;
    }

    @Override
    public long getChildId(int i, int i1) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        LayoutInflater lf = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = lf.inflate(R.layout.artist_list_artist, null);
        // now get the artist text box and set the text
        TextView artistNameText = (TextView) view.findViewById(R.id.artistName);
        artistNameText.setText(m_libraryLoader.getArtists().get(i).artist);
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        LayoutInflater lf = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = lf.inflate(R.layout.artist_list_album, null);
        // now get the album text box and set the text
        TextView albumNameText = (TextView) view.findViewById(R.id.albumName);
        albumNameText.setText(m_libraryLoader.getArtists().get(i).getAlbums().get(i1).albumName);
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
