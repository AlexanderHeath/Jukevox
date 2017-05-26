package com.liquidcode.jukevox.util;

public interface BTMessages {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_CLIENT_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;
    int MESSAGE_USER_DISCONNECT = 6;
    int MESSAGE_CONNECTED_TO_SERVER = 7;
    int MESSAGE_CLIENT_NUM_CHANGED = 8;

    // Key names received from the BluetoothChatService Handler
    String CLIENT_NAME = "client_name";
    String SERVER_NAME = "server_name";
    String TOAST = "toast";
    String CLIENT_COUNT = "client_count";

    byte SM_CLIENTCOUNT = 1;
    byte SM_SONGINFO = 2;
    byte SM_SONGDATA = 3;
}
