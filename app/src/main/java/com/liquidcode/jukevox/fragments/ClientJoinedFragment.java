package com.liquidcode.jukevox.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfo;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.networking.Messaging.MessageBuilder;
import com.liquidcode.jukevox.networking.Messaging.MessageParser;
import com.liquidcode.jukevox.util.BTUtils;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.room_layout, container, false);

        // init the text widgets so we can be updated by the server
        initTextWidgets(root);
        return root;
    }

    /**
     * Sets the bluetooth client that we successfully connected to the server with
     * @param btc - the bluetooth client object that we made a successful connection with
     */
    public void setBluetoothClient(BluetoothClient btc) {
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
    private void initTextWidgets(ViewGroup root) {
        m_logText = (TextView)root.findViewById(R.id.serverLogText);
        m_logText.setMovementMethod(new ScrollingMovementMethod());
        m_clientCountText = (TextView)root.findViewById(R.id.clientCountText);
    }

    private void updateClientCount(int currentClients) {
        String formattedText = String.format(Locale.ENGLISH, "%d/%d", currentClients, BTUtils.MAX_BT_CLIENTS);
        // now set the text for the textview
        m_clientCountText.setText(formattedText);
    }

    public void sendSongInfo(String artist, String song) {
        byte[] outgoing = MessageBuilder.buildSongData(artist, song);
        m_bluetoothClient.sendDataToServer(outgoing);
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
    private final Handler mHandler = new Handler() {
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
                    }
                    m_isConnectedToRoom = false;
                    break;
            }
        }
    };

    private void processIncomingMessage(byte[] buffer) {

        // check the first byte for message type
        switch(buffer[0]) {
            case BTMessages.SM_CLIENTCOUNT: {
                // increase the index so we are reading our actual message
                int currentClientCount = MessageParser.parseClientCount(buffer);
                updateClientCount(currentClientCount);
                break;
            }
            case BTMessages.SM_SONGINFO: {
                SongInfo songinfo = MessageParser.parseSongInfo(buffer);
                m_logText.append("-" + songinfo.getArtist() + " - " + songinfo.getSongName() + "\n");
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
