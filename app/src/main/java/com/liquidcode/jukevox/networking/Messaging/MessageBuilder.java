package com.liquidcode.jukevox.networking.Messaging;

import java.nio.charset.Charset;

/**
 * MessageBuilder.java
 * Builds different messages and puts the data into formatted byte arrays to be sent over socket
 * Created by mikev on 5/26/2017.
 */

public class MessageBuilder {

    /**
     * Builds the SongData class to be sent over the socket
     * @param artist - the artist name
     * @param song - the song name
     * @return byte[] of our data
     */
    public static byte[] buildSongData(String artist, String song) {
        // get the sizes for the data we're sending
        // outgoing song data = 1byte header (SM_SONGINFO) + artist length + 1byte delim + song length + 1 byte delim
        int outSize = 1 + artist.length() + 1 + song.length() + 1;
        byte[] outgoing = new byte[outSize];
        // now build our byte data
        int currentIndex = 0;
        // song data header
        outgoing[currentIndex] = BTMessages.SM_SONGINFO;
        ++currentIndex;
        // copy artist name bytes
        System.arraycopy(artist.getBytes(Charset.forName("UTF-8")), 0, outgoing, currentIndex, artist.length());
        currentIndex += artist.length();
        // put in delimeter
        outgoing[currentIndex] = BTMessages.SM_DELIM;
        ++currentIndex;
        // copy song name
        System.arraycopy(song.getBytes(Charset.forName("UTF-8")), 0, outgoing, currentIndex, song.length());
        currentIndex += song.length();
        // put in delim
        outgoing[currentIndex] = BTMessages.SM_DELIM;
        return outgoing;
    }

    public static byte[] buildClientCountData(int clientCount) {
        byte[] outgoing = new byte[2];
        outgoing[0] = BTMessages.SM_CLIENTCOUNT;
        outgoing[1] = (byte)clientCount;
        return outgoing;
    }
}
