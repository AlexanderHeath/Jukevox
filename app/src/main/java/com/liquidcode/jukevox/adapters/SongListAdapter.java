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

import com.liquidcode.jukevox.LibraryLoader;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.musicobjects.Song;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by mikev on 11/7/2016.
 */

public class SongListAdapter extends RecyclerView.Adapter implements ListAdapter {
    private ArrayList<Song> m_songList = null;
    private Context m_context = null;

    public SongListAdapter(ArrayList<Song> songList, Context c) {
        m_songList = songList;
        m_context = c;
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
        return m_songList.size();
    }

    @Override
    public Object getItem(int i) {
        return m_songList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return m_songList.size();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // setup the view
        LayoutInflater lf = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = lf.inflate(R.layout.song_list_song, viewGroup, false);
        // now get the artist text box and set the text
        TextView artistNameText = (TextView) view.findViewById(R.id.songName);
        artistNameText.setText(m_songList.get(i).title);
        // set the duration string
        TextView durationText = (TextView)view.findViewById(R.id.durationText);
        durationText.setText(getTimeString(m_songList.get(i).duration));
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
        return (m_songList.isEmpty());
    }

    private String getTimeString(long millis) {
        StringBuffer buf = new StringBuffer();

        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

        String hoursStr = String.format(Locale.ENGLISH, "%02d", hours);
        if(!hoursStr.equals("00"))
        {
            buf.append(String.format(Locale.ENGLISH, "%02d", hours)).append(":");
        }
        buf.append(String.format(Locale.ENGLISH, "%02d", minutes))
                .append(":")
                .append(String.format(Locale.ENGLISH, "%02d", seconds));

        return buf.toString();
    }
}
