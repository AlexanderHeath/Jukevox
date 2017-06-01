package com.liquidcode.jukevox.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfo;
import java.util.ArrayList;

/**
 * Created by mikev on 5/30/2017.
 */

public class QueuedSongAdapter extends RecyclerView.Adapter implements ListAdapter {
    private ArrayList<SongInfo> m_queuedList = null;
    private Context m_context = null;

    public QueuedSongAdapter(Context context, ArrayList<SongInfo> items) {
        m_context = context;
        m_queuedList = items;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public int getCount() {
        return m_queuedList.size();
    }

    @Override
    public SongInfo getItem(int i) {
        return m_queuedList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return m_queuedList.size();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // setup the view
        LayoutInflater lf = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = lf.inflate(R.layout.server_song_list_child, viewGroup, false);
        // now get the artist text box and set the text
        TextView artistNameText = (TextView) view.findViewById(R.id.sl_artist_name);
        artistNameText.setText(m_queuedList.get(i).getArtist());
        // set song name
        TextView songNameText = (TextView)view.findViewById(R.id.sl_song_name);
        songNameText.setText(m_queuedList.get(i).getSongName());
        return view;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemViewType(int i) {
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return m_queuedList.isEmpty();
    }
}
