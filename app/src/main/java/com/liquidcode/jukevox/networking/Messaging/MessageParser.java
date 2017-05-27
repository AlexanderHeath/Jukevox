package com.liquidcode.jukevox.networking.Messaging;

import com.liquidcode.jukevox.networking.MessageObjects.SongInfo;

/**
 * MessageParser
 * This class will parse incoming byte[] from the socket and get specified data from it
 * Created by mikev on 5/26/2017.
 */
public class MessageParser {

    /**
     * Parses the SM_SONGINFO message and returns our processed data
     * @param incoming - incoming byte[] from the socket
     * @return - An object wrapping the artist/song name
     */
    public static SongInfo parseSongInfo(byte[] incoming) {
        SongInfo songinfo = null;
        if(incoming.length > 0) {
            // process buffer
            // convert to string
            String data = new String(incoming, 1, incoming.length-1);
            // split on our delimiter
            String[] parts = data.split(String.valueOf(BTMessages.SM_DELIM));
            String artist = parts[0];
            String songName = parts[1];
            songinfo = new SongInfo(artist, songName);
        }
        return songinfo;
    }

    /**
     * Parses message for current client count
     * @param incoming - incoming byte[] from socket
     * @return - the current number of clients
     */
    public static int parseClientCount(byte[] incoming) {
        int clientCount = -1;
        if(incoming.length > 0) {
            // skip the first header byte and get the number
            clientCount = incoming[1];
        }
        return clientCount;
    }
}
