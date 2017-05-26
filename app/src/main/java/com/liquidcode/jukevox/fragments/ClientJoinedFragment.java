package com.liquidcode.jukevox.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.util.BTMessages;
import com.liquidcode.jukevox.util.BTUtils;

/**
 * Created by mikev on 5/24/2017.
 */

public class ClientJoinedFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "BTJClient";
    private TextView m_logText = null;
    private TextView m_clientCountText = null;
    // our BluetoothManager instance
    private BluetoothClient m_bluetoothClient = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.room_layout, container, false);

        // init the text widgets so we can be updated by the server
        initTextWidgets(root);
        return root;
    }

    public void setBluetoothClient(BluetoothClient btc) {
        m_bluetoothClient = btc;
        // update the handler so we get the new messages now
        if(m_bluetoothClient != null) {
            m_bluetoothClient.updateUIHandler(mHandler);
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
        String formattedText = String.format("%d/%d", currentClients, BTUtils.MAX_BT_CLIENTS);
        // now set the text for the textview
        m_clientCountText.setText(formattedText);
    }

    public void sendSongData(String artist, String song) {
        // get the sizes for the data we're sending
        // outgoing song data = 1byte header (SM_SONGDATA) + artist length + 1byte delim + song length + 1 byte delim
        int outSize = 1 + artist.length() + 1 + song.length() + 1;
        byte[] outgoing = new byte[outSize];
        m_bluetoothClient.sendDataToServer(outgoing);
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
                case BTMessages.MESSAGE_CONNECTED_TO_SERVER:
                    // make a toast with our server we connected to
                    // save the connected device's name
                    String serverName = msg.getData().getString(BTMessages.SERVER_NAME);
                    if (null != getActivity()) {
                        Toast.makeText(getActivity(), "Connected to server: " + serverName, Toast.LENGTH_SHORT).show();
                    }
                    m_logText.append("Connected to server: " + serverName + "\n");
                    break;
                case BTMessages.MESSAGE_READ:
                    byte[] incoming = (byte[])msg.obj;
                    int begin = (int)msg.arg1;
                    int end = (int)msg.arg2;
                    processIncomingMessage(incoming, end);
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
                        serverName = msg.getData().getString(BTMessages.SERVER_NAME);
                        Toast.makeText(getActivity(), "Disconnected from: " + serverName,
                                Toast.LENGTH_SHORT).show();
                        m_logText.append("Disconnected from server: " + serverName + "\n");
                    }
                    break;
            }
        }
    };

    private void processIncomingMessage(byte[] buffer, int size) {

        // check the first byte for message type
        int currentIndex = 0;
        switch(buffer[currentIndex]) {
            case BTMessages.SM_CLIENTCOUNT:
                // increase the index so we are reading our actual message
                ++currentIndex;
                int currentClientCount = buffer[currentIndex];
                updateClientCount(currentClientCount);
                break;
            default:
                m_logText.append("Unable to read incoming message. Ignoring");
                break;
        }
    }
}
