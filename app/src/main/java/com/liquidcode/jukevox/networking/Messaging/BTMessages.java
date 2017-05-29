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

    // Key names received from the BluetoothChatService Handler
    String CLIENT_NAME = "client_name";
    String SERVER_NAME = "server_name";
    String TOAST = "toast";

    // size of our message headers sent through sockets
    byte SM_MESSAGEHEADERSIZE = 1;
    byte SM_DELIMITERSIZE = 1;
    // Message headers
    byte SM_CLIENTCOUNT = 1;
    byte SM_SONGINFO = 2;
    byte SM_SONGDATA = 3;
    byte SM_INFO = 4;

    char SM_DELIM = '#';
}
