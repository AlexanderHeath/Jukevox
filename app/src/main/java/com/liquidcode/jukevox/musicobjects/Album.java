package com.liquidcode.jukevox.musicobjects;

import java.util.ArrayList;

import android.net.Uri;

public class Album {
	public String albumName;
	public ArrayList<Song> songList;
	public Uri albumArt;
	
	public Album(String name)
	{
		albumName = name;
		songList = new ArrayList<Song>();
		albumArt = null;
	}

	@Override
	public boolean equals(Object o) {
		boolean isEqual = false;
		if(o instanceof Album) {
			isEqual = (((Album) o).albumName.equals(this.albumName));
		}
		return isEqual;
	}
}
