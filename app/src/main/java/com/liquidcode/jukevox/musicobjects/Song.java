package com.liquidcode.jukevox.musicobjects;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Song implements Parcelable, Serializable {
	
	public String title;
	public long duration;
	public long id;
	public long data;
	
	public Song(String t, long _id, long dur)
	{
		title = t;
		id = _id;
		duration = dur;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeLong(duration);
		dest.writeLong(id);
		dest.writeString(title);
		dest.writeLong(data);
	}
	
	public static final Parcelable.Creator<Song> CREATOR
	= new Parcelable.Creator<Song>() {
		public Song createFromParcel(Parcel in) {
			return new Song(in);
		}

		public Song[] newArray(int size) {
			return new Song[size];
		}
	};

	private Song(Parcel in) {
		duration = in.readLong();
		id = in.readLong();
		title = in.readString();
		data = in.readLong();
}
}
