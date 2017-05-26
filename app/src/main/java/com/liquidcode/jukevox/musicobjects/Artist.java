package com.liquidcode.jukevox.musicobjects;

import java.util.ArrayList;

public class Artist {
    public String artist;
    public ArrayList<Album> albumList = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if(o instanceof Artist) {
            isEqual = (((Artist) o).artist.equals(this.artist));
        }
        return isEqual;
    }


    public Artist(String _artist) {
        artist = _artist;
    }
    public ArrayList<Album> getAlbums() { return albumList; }
}
