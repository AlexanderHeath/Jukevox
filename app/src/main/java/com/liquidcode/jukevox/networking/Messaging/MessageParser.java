package com.liquidcode.jukevox.networking.Messaging;

import com.liquidcode.jukevox.networking.MessageObjects.BasicStringWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfoWrapper;

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
    public static SongInfoWrapper parseSongInfo(byte[] incoming) {
        SongInfoWrapper songinfo = null;
        if(incoming.length > 0) {
            // get the client id
            byte clientID = incoming[1];
            // process buffer
            // convert to string
            String data = new String(incoming, 2, incoming.length-2);
            // split on our delimiter
            String[] parts = data.split(String.valueOf(BTMessages.SM_DELIM));
            String artist = parts[0];
            String songName = parts[1];
            songinfo = new SongInfoWrapper(artist, songName, clientID);
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
    public static BasicStringWrapper parseInfoData(byte[] incoming) {
        String info = null;
        byte clientID;
        BasicStringWrapper infowrapper = null;
        if(incoming.length > 0) {
            // get clients id
            clientID = incoming[1];
            // get the info string
            info = new String(incoming, 2, incoming.length-2);
            String[] parts = info.split(String.valueOf(BTMessages.SM_DELIM));
            // check to see if the last index is our message delimiter and remove it
            infowrapper = new BasicStringWrapper(parts[0], clientID);
        }
        return infowrapper;
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

    public static BasicStringWrapper parseClientDisplayName(byte[] incoming) {
        BasicStringWrapper info = null;
        String name;
        byte clientID;
        if(incoming.length > 0) {
            // get clients id
            clientID = incoming[1];
            // get the info string
            name = new String(incoming, 2, incoming.length-2);
            String[] parts = name.split(String.valueOf(BTMessages.SM_DELIM));
            // check to see if the last index is our message delimiter and remove it
            info = new BasicStringWrapper(parts[0], clientID);
        }
        return info;
    }

    /**
     * Parses a response message for the SM code
     * @param incoming
     * @return
     */
    public static byte parseResponse(byte[] incoming) {
        byte messageResponse = 0;
        if(incoming.length > 0) {
            messageResponse = incoming[2];
        }
        return messageResponse;
    }

    /**
     * Parses a response message from the server
     * @param incoming
     * @return
     */
    public static byte parseServerResponse(byte[] incoming) {
        byte messageResponse = 0;
        if(incoming.length > 0) {
            messageResponse = incoming[1];
        }
        return messageResponse;
    }
}
