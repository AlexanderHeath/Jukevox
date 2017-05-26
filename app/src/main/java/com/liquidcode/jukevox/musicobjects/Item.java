package com.liquidcode.jukevox.musicobjects;

import android.net.Uri;

public class Item {
	public String title;
	public String artist;
	public String album;
	public long id;
	public long duration;
	public Uri albumArtPath; 
	public byte[] data;
	
	public Item(String _artist, String _album, String _title, long _duration, long _id, Uri _albumArtPath, byte[] _data)
	{
		artist = _artist; 
		album = _album;
		title = _title;
		duration = _duration;
		id = _id;
		albumArtPath = _albumArtPath;
		data = _data;
	}
}
