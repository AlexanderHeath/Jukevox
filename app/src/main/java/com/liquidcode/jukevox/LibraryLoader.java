package com.liquidcode.jukevox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import com.liquidcode.jukevox.musicobjects.Album;
import com.liquidcode.jukevox.musicobjects.Artist;
import com.liquidcode.jukevox.musicobjects.Item;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.util.ResultCode;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class LibraryLoader {

	final String TAG = "LibraryLoader";

	private ContentResolver mContentResolver;

	// the items (songs) we have queried
	private ArrayList<Artist> mArtists = new ArrayList<Artist>();
	private ArrayList<Item> mItems = new ArrayList<Item>();
	private ResultCode mResultCode = ResultCode.RESULT_FAILURE;

	public LibraryLoader(ContentResolver cr) {
		mContentResolver = cr;
	}

	/**
	 * Loads music data. This method may take long, so be sure to call it asynchronously without
	 * blocking the main thread.
	 */
	public ResultCode prepare() {
		String[] STAR = { "*" };        
		Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
		Cursor cur = mContentResolver.query(allsongsuri, STAR, selection, null, null);

		if (cur == null) {
			// Query failed...
			Log.e(TAG,"Failed to retrieve music: cursor is null :-(");
			mResultCode = ResultCode.RESULT_FAILURE;
			return mResultCode;
		}
		if (!cur.moveToFirst()) {
			// Nothing to query. There is no music on the device. How boring.
			Log.e(TAG, "Failed to move cursor to first row (Library was empty!).");
			mResultCode = ResultCode.RESULT_FAILURE;
			return mResultCode;
		}

		// retrieve the indices of the columns where the ID, title, etc. of the song are
		int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
		int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
		int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
		int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
		int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
		int albumArtColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
		int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

		// add each song to mItems
		do {
			long _id = cur.getLong(idColumn);
			String artist = cur.getString(artistColumn);
			String title = cur.getString(titleColumn);
			String album = cur.getString(albumColumn);
			long duration = cur.getLong(durationColumn);
			long artID = cur.getLong(albumArtColumn);
			byte[] data = cur.getBlob(dataColumn);

			// parse for the album art
			Uri sArtworkUri = Uri
					.parse("content://media/external/audio/albumart");
			Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, artID);

			AddQueryToLibrary(artist, album, title, duration, _id, albumArtUri, data);

		} while (cur.moveToNext());

		cur.close();

		Collections.sort(mArtists, new Comparator<Artist>() {
			public int compare(Artist a, Artist b)
			{
				return a.artist.compareToIgnoreCase(b.artist);
			}
		});
		
		mResultCode = ResultCode.RESULT_SUCCESS;
		return mResultCode; // success :D
	}

	//	While we are looping through the phones storage finding music.  We need to start adding them to our internal
	//	library.
	private void AddQueryToLibrary(String artist, String album, String title, long duration, long id, Uri albumURI, byte[] data) {
		// lets see if we have the artist
		// if we dont this artist is completely new and needs to be added
		if(!mArtists.contains(new Artist(artist))) {
			// we dont have this artist. create.
			Artist newArtist = new Artist(artist);
			// we dont have this album either create and add song.
			Album newAlbum = new Album(album);
			newAlbum.albumArt = albumURI;
			// dont have this song create it
			Song newSong = new Song(title, id, duration);
			newSong.data = data;
			// add the song to album
			newAlbum.songList.add(newSong);
			// add the album to the artist
			newArtist.albumList.add(newAlbum);
			// we're done add the artist to the artistList
			mArtists.add(newArtist);
		}
		else {
			// the artist exists lets see if the album does
			int indexOfArtist = mArtists.indexOf(new Artist(artist));
			int indexOfAlbum = mArtists.get(indexOfArtist).albumList.indexOf(new Album(album));
			if(indexOfAlbum == -1) {
				// this album didnt exist lets create it and add the song to it
				Album newAlbum = new Album(album);
				newAlbum.albumArt = albumURI;
				Song newSong = new Song(title, id, duration);
				newSong.data = data;
				// add song to album
				newAlbum.songList.add(newSong);
				// add album to artist
				mArtists.get(indexOfArtist).albumList.add(newAlbum);
			}
			else {
				// both the artist and the album exists so lets just add the song to the album
				indexOfAlbum = mArtists.get(indexOfArtist).albumList.indexOf(new Album(album));
				Song newSong = new Song(title, id, duration);
				newSong.data = data;
				mArtists.get(indexOfArtist).albumList.get(indexOfAlbum).songList.add(newSong);
			}
		}
	}

	public ArrayList<Artist> getArtists() { return mArtists; }
}
