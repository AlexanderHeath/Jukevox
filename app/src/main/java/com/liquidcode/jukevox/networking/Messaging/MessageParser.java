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

    /**
     * Parses info sent by server/client
     * @param incoming
     * @return
     */
    public static String parseInfoData(byte[] incoming) {
        String info = null;
        if(incoming.length > 0) {
            info = new String(incoming, 1, incoming.length-1);
            // check to see if the last index is our message delimiter and remove it
            if(info.endsWith(String.valueOf(BTMessages.SM_DELIM))) {
                StringBuilder sb = new StringBuilder(info);
                sb.deleteCharAt(info.length()-1);
                info = sb.toString();
            }
        }
        return info;
    }

    /**
     * Parses the client ID message and gets the new ID value
     * @param incoming - the incoming byte array
     * @return
     */
    public static byte parseCientIDData(byte[] incoming) {
        byte id = 0;
        if(incoming.length > 0) {
            id = incoming[1];
        }
        return id;
    }

    public static byte parseResponse(byte[] incoming) {
        byte messageResponse = 0;
        if(incoming.length > 0) {
            messageResponse = incoming[2];
        }
        return messageResponse;
    }
}
