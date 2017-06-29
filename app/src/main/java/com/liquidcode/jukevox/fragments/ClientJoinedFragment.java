package com.liquidcode.jukevox.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.adapters.QueuedSongAdapter;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.MessageObjects.BasicStringWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfoWrapper;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.networking.Messaging.MessageBuilder;
import com.liquidcode.jukevox.networking.Messaging.MessageParser;
import com.liquidcode.jukevox.util.BTUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment that handles the clients current connection to a server room.
 * This class is responsible for pushing music data and other info to the server.
 * Also listens for updates from the server about other clients
 * Created by mikev on 5/24/2017.
 */
public class ClientJoinedFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "BTJClient";
    private TextView m_logText = null;
    private TextView m_clientCountText = null;
    // our BluetoothManager instance
    private BluetoothClient m_bluetoothClient = null;
    // are we currently connected to a room?
    private boolean m_isConnectedToRoom = false;
    private QueuedSongAdapter m_queueAdapter = null;
    private ListView m_queueListview = null;
    // list of queued song that we get from the server
    private ArrayList<SongInfoWrapper> m_queuedSongList = null;
    // our unique ID from the server
    private byte m_id;
    // our display name
    private String m_displayName;

    // Variables that keep track of the clients current song its streaming to the server
    private int m_currentPosition; // how much data we've sent so far
    private int m_maxSongLength; // how much data this song is
    private Song m_currentSong;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.room_layout, container, false);

        // init the text widgets so we can be updated by the server
        initWidgets(root);
        return root;
    }

    public void initializeClient(String displayName, BluetoothClient btc) {
        m_displayName = displayName;
        m_currentPosition = 0;
        m_maxSongLength = 0;
        m_currentSong = null;
        setBluetoothClient(btc);
    }

    /**
     * Sets the bluetooth client that we successfully connected to the server with
     * @param btc - the bluetooth client object that we made a successful connection with
     */
    private void setBluetoothClient(BluetoothClient btc) {
        m_bluetoothClient = btc;
        // update the handler so we get the new messages now
        if(m_bluetoothClient != null) {
            m_bluetoothClient.updateUIHandler(mHandler);
            m_isConnectedToRoom = true;
        }
        else {
            Log.e(TAG, "BTJoined received a NULL BluetoothClient!!");
        }
    }

    private void initWidgets(ViewGroup root) {
        // if the arraylist of songInfo is null create it
        if(m_queuedSongList == null) {
            m_queuedSongList = new ArrayList<>();
        }
        // init the listview and adapter
        m_queueListview = (ListView)root.findViewById(R.id.room_song_list);
        if(m_queueListview != null) {
            // create the adapter that we will notify changes with
            if(m_queueAdapter == null) {
                m_queueAdapter = new QueuedSongAdapter(getActivity(), m_queuedSongList);
            }
            m_queueListview.setAdapter(m_queueAdapter);
        }

        initTextWidgets(root);
    }

    private void initTextWidgets(ViewGroup root) {
        m_logText = (TextView)root.findViewById(R.id.serverLogText);
        m_logText.setMovementMethod(new ScrollingMovementMethod());
        m_clientCountText = (TextView)root.findViewById(R.id.clientCountText);
    }

    private synchronized void updateClientCount(int currentClients) {
        String formattedText = String.format(Locale.ENGLISH, "%d/%d", currentClients, BTUtils.MAX_BT_CLIENTS);
        // now set the text for the textview
        m_clientCountText.setText(formattedText);
    }

    public void sendSongInfo(String artist, String song) {
        byte[] outgoing = MessageBuilder.buildSongInfo(m_id, artist, song);
        m_bluetoothClient.sendDataToServer(outgoing, true);
    }

    /**
     * Returns whether or not we are currently connected to a room
     */
    public boolean isConnectedToRoom() {
        return m_isConnectedToRoom;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Disconnection from room!");
        // stop the connection thread
        // disconnect from the server here since we are actively connected
        if(m_bluetoothClient != null) {
            m_bluetoothClient.disconnectFromServer();
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case BTMessages.MESSAGE_READ:
                    byte[] incoming = (byte[])msg.obj;
                    processIncomingMessage(incoming);
                    break;
                case BTMessages.MESSAGE_STATE_CHANGE:
                    // not implemented
                    break;
                case BTMessages.MESSAGE_TOAST:
                    // not implemented
                    break;
                case BTMessages.MESSAGE_WRITE:
                    // not implemented
                    break;
                case BTMessages.MESSAGE_USER_DISCONNECT:
                    if (null != getActivity()) {
                        String serverName;
                        serverName = msg.getData().getString(BTMessages.SERVER_NAME);
                        Toast.makeText(getActivity(), "Disconnected from: " + serverName,
                                Toast.LENGTH_SHORT).show();
                        m_logText.append("Disconnected from server: " + serverName + "\n");
                        // close this fragment since we arent connected anymore
                        ((JukevoxMain)getActivity()).closeRoomFragments();
                        if(m_bluetoothClient != null) {
                            m_bluetoothClient.disconnectFromServer();
                        }
                    }
                    m_isConnectedToRoom = false;
                    break;
            }
        }
    };


    private void processIncomingMessage(byte[] buffer) {
        ArrayList<byte[]> messages = MessageParser.splitMessages(buffer);
        for(byte[] message : messages) {
            // check the first byte for message type
            switch (message[0]) {
                case BTMessages.SM_CLIENTCOUNT: {
                    // increase the index so we are reading our actual message
                    int currentClientCount = MessageParser.parseClientCount(message);
                    updateClientCount(currentClientCount);
                    // send our repsonse
                    sendResponse(BTMessages.SM_CLIENTCOUNT);
                    break;
                }
                case BTMessages.SM_SONGINFO: {
                    SongInfoWrapper songinfo = MessageParser.parseSongInfo(message);
                    if (songinfo != null) {
                        m_queuedSongList.add(songinfo);
                        m_queueAdapter.notifyDataSetChanged();
                        m_queueListview.setAdapter(m_queueAdapter);
                        sendResponse(BTMessages.SM_SONGINFO);
                    }
                    m_logText.append("-" + songinfo.getArtist() + " - " + songinfo.getSongName() + "\n");
                    break;
                }
                case BTMessages.SM_INFO: {
                    BasicStringWrapper info = MessageParser.parseInfoData(message);
                    m_logText.append("Info: " + info.getStringData() + "\n");
                    sendResponse(BTMessages.SM_INFO);
                    break;
                }
                case BTMessages.SM_CLIENTID: {
                    m_id = MessageParser.parseCientIDData(message);
                    if (m_bluetoothClient != null) {
                        // send our response
                        sendResponse(BTMessages.SM_CLIENTID);
                        // we got our client id and responded now tell the server our display name
                        m_bluetoothClient.sendDataToServer(MessageBuilder.buildClientNameData(m_id, m_displayName), true);
                    }
                    break;
                }
                case BTMessages.SMR_RESPONSE: {
                    // Handle the responses
                    if (m_bluetoothClient != null) {
                        String responseID = String.format("Response ID: (%d)", MessageParser.parseServerResponse(message));
                        byte responsebyte = MessageParser.parseServerResponse(message);
                        Log.i("CST", responseID);
                        m_bluetoothClient.handleResponseMessage(responsebyte);
                    }
                    break;
                }
                default: {
                    // dont append this right now
                    //m_logText.append("Unsupported message type received!\n");
                    Log.e(TAG, "Unsupported message type received!\n");
                    break;
                }
            }
        }
    }

    public void beginSongStreaming(String artist, Song songData) {
        // send the song info over
         if(m_bluetoothClient != null) {
             sendSongInfo(artist, songData.title);
             // start sending the bytes until we reached the max for the current song
             // new thread?
         }
    }

    private void sendResponse(byte messID) {
        if(m_bluetoothClient != null) {
            byte[] out = MessageBuilder.buildMessageResponse(m_id, messID);
            m_bluetoothClient.sendDataToServer(out, false);
        }
    }
}
