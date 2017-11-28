package com.liquidcode.jukevox.networking.Messaging;

/**
 * Class that holds networking message definitions
 * Created by mikev on 5/8/2017.
 */

public interface BTMessages {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_CLIENT_DEVICE_CONNECTED = 4;
    int MESSAGE_TOAST = 5;
    int MESSAGE_USER_DISCONNECT = 6;
    int MESSAGE_CONNECTED_TO_SERVER = 7;
    int MESSAGE_CLIENT_NUM_CHANGED = 8;
    int MESSAGE_UPDATE_STREAM_PROGRESS = 9;
    int MESSAGE_UPDATE_STREAM_END = 10;

    // Key names received from the BluetoothChatService Handler
    String CLIENT_NAME = "client_name";
    String SERVER_NAME = "server_name";
    String CLIENT_ID = "client_id";
    String TOAST = "toast";
    String STREAM_PROGRESS = "stream_progress";

    // default time delay that a sent message waits before resending
    float DEFAULT_DELAY = 50.0f;

    // INDEXES
    // index where the boolean value for a song data being finished lives
    byte SM_MESSAGEHEADER_SONGFINISHED_INDEX = 4;
    // SIZES
    // size in bytes of a whole message header not including actual data
    // HEADERSIZE (1) - LENGTH (2) - CLIENTID (1)
    byte SM_MESSAGEHEADERSIZE = 4;
    // starts right where the client ID begins
    byte SM_MESSAGEHEADERSIZE_NOCLIENTID = 3;
    // size of our message headers sent through sockets
    byte SM_MESSAGETYPESIZE = 1;
    byte SM_LENGTH = 2; // two bytes for the length
    byte SM_DELIMITERSIZE = 1;
    byte SM_CLIENTIDSIZE = 1;
    byte SM_BOOLEAN = 1;
    // MESSAGE HEADERS ( 1 - 50 reserved for requests )
    byte SM_CLIENTCOUNT = 1;
    byte SM_SONGINFO = 2;
    byte SM_SONGDATA = 3;
    byte SM_INFO = 4;
    byte SM_CLIENTDISPLAYNAME = 5;
    byte SM_CLIENTID = 6;

    // RESPONSES
    // Response message headers (reserved 51 - 100)
    byte SMR_RESPONSE = 51;

    char SM_DELIM = '#';
}
