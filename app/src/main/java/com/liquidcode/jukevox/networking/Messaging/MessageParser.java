package com.liquidcode.jukevox.networking.Messaging;

import android.util.Log;

import com.liquidcode.jukevox.networking.MessageObjects.BasicByteWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.BasicStringWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongDataWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfoWrapper;

import java.util.ArrayList;

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
            // skip message header and length = 3bytes
            // get the client id
            byte clientID = incoming[BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID];
            // process buffer
            // convert to string
            String data = new String(incoming, BTMessages.SM_MESSAGEHEADERSIZE, incoming.length-BTMessages.SM_MESSAGEHEADERSIZE);
            // split on our delimiter
            String[] parts = data.split(String.valueOf(BTMessages.SM_DELIM));
            String artist = parts[0];
            String songName = parts[1];
            songinfo = new SongInfoWrapper(artist, songName, clientID);
        }
        return songinfo;
    }

    public static SongDataWrapper parseSongData(byte[] incoming) {
        SongDataWrapper songData = null;
        if(incoming.length > 0) {
            int currentIndex = BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID; // set to index 3 (msg type(index 0) + 2length(index 2)
            byte clientID = incoming[currentIndex];
            currentIndex = BTMessages.SM_MESSAGEHEADER_SONGFINISHED_INDEX;  // set to index 4
            byte songFinishedValue = incoming[currentIndex];
            boolean isSongFinished = (songFinishedValue == 1) ? true : false;
            // typically normal messages start at index 4 but since this has an extra boolean
            // we are going to set to the BTMessages.SM_MESSAGEHEADERSIZE + 1 = index 5.
            currentIndex = BTMessages.SM_MESSAGEHEADERSIZE + 1; /// start at index 5
            // get the new byte array of song data
            byte[] songbuffer = new byte[incoming.length-currentIndex];
            System.arraycopy(incoming, currentIndex, songbuffer, 0, incoming.length-currentIndex);
            songData = new SongDataWrapper(clientID, songbuffer, isSongFinished);
        }
        return songData;
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
            clientCount = incoming[BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID];
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
            clientID = incoming[BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID];
            // get the info string
            info = new String(incoming, BTMessages.SM_MESSAGEHEADERSIZE, incoming.length-BTMessages.SM_MESSAGEHEADERSIZE);
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
    public static byte parseClientIDData(byte[] incoming) {
        byte id = 0;
        if(incoming.length > 0) {
            id = incoming[BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID];
        }
        return id;
    }

    public static BasicStringWrapper parseClientDisplayName(byte[] incoming) {
        BasicStringWrapper info = null;
        String name;
        byte clientID;
        if(incoming.length > 0) {
            // get clients id
            clientID = incoming[BTMessages.SM_MESSAGEHEADERSIZE_NOCLIENTID];
            // get the client display name
            name = new String(incoming, BTMessages.SM_MESSAGEHEADERSIZE, incoming.length-BTMessages.SM_MESSAGEHEADERSIZE);
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
    public static BasicByteWrapper parseResponse(byte[] incoming) {
        BasicByteWrapper byteWrapper = null;
        if(incoming.length > 0) {
            // skip MESSAGETYPE (1) + LENGTH (2) = 3
            byteWrapper = new BasicByteWrapper(incoming[3], incoming[4]);
        }
        return byteWrapper;
    }

    /**
     * Parses a response message from the server
     * @param incoming
     * @return
     */
    public static byte parseServerResponse(byte[] incoming) {
        byte messageResponse = 0;
        if(incoming.length > 0) {
            // skip MESSAGETYPE(1) + LENGTH (2)
            messageResponse = incoming[3];
        }
        return messageResponse;
    }

    /**
     * Takes the incoming buffer and splits the messages..Sometimes multiple messages come in at once
     * they need to be broken apart so we can process them individually.
     * @param buffer
     * @return
     */
    public static ArrayList<byte[]> splitMessages(byte[] buffer) {
        ArrayList<byte[]> messageList = new ArrayList<>();
        int currentIndex = 0, startPosition = 0;
        // get the message header
        while(buffer[startPosition] != 0) {
            if(buffer[0] == BTMessages.SM_SONGDATA) {
                Log.d("tag", "Stop here");
            }
            // get the size of this whole message
            int messSize;
            // move the current pointer (lo byte) (header + 1byte)
            ++currentIndex;
            int lo = buffer[startPosition + currentIndex];
            ++currentIndex;
            // get hi byte
            int hi = buffer[startPosition + currentIndex];
            ++currentIndex;
            messSize = (short)((hi << 8) | (lo & 0xFF));
            // we got the size
            if(messSize <= 0) {
                break;
            }
            // now lets do a check that the "length" of the message isnt bigger than what we actually received.
            if(buffer.length < messSize) {
                messSize = buffer.length;
            }
            // copy this message into the array list
            byte[] newMessage = new byte[messSize];
            System.arraycopy(buffer, startPosition, newMessage, 0, messSize);
            messageList.add(newMessage);
            startPosition += messSize; // skip this whole message
            // if the nextStart position is outside the array bounds we need to break out
            if(startPosition >= buffer.length) {
                break; // stop the loop we reached the end of buffer
            }
            // reset current position
            currentIndex = 0;
        }
        return messageList;
    }
}
