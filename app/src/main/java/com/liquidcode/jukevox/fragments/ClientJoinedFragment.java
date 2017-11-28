package com.liquidcode.jukevox.fragments;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.adapters.QueuedSongAdapter;
import com.liquidcode.jukevox.musicobjects.ByteDataSource;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.MessageObjects.BasicStringWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfoWrapper;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.networking.Messaging.MessageBuilder;
import com.liquidcode.jukevox.networking.Messaging.MessageParser;
import com.liquidcode.jukevox.networking.Streaming.StreamingThread;
import com.liquidcode.jukevox.util.BTUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
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
    private ProgressBar m_sendingProgress = null;
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
    private BluetoothA2dp m_a2dpProfile = null;
    private BluetoothProfile.ServiceListener m_btServiceListener = null;
    private MediaPlayer m_mediaPlayer = null;
    // the streaming thread
    private StreamingThread m_streamingThread = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.room_layout, container, false);

        // init the text widgets so we can be updated by the server
        initWidgets(root);
        getActivity().registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        getActivity().registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED));
        // init the bluetooth service a2dp
        //initBluetoothServiceListener();
        connectUsingBluetoothA2dp(getActivity(), m_bluetoothClient.getServerDevice());
        return root;
    }

    public void initializeClient(String displayName, BluetoothClient btc) {
        m_displayName = displayName;
        setBluetoothClient(btc);
        if(m_mediaPlayer == null) {
            m_mediaPlayer = new MediaPlayer();
            m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
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
        // init the sending progress bar so we can use it when we need to
        m_sendingProgress = (ProgressBar) root.findViewById(R.id.sendingProgress);
        m_sendingProgress.setVisibility(View.INVISIBLE);
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
        //stopSong();
        getActivity().unregisterReceiver(mReceiver);
        if(m_bluetoothClient != null) {
            m_bluetoothClient.getBTAdapter().closeProfileProxy(BluetoothProfile.A2DP, m_a2dpProfile);
        }
        if(m_streamingThread != null) {
            m_streamingThread.cancel();
            m_streamingThread = null;
        }
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
        if(m_streamingThread != null) {
            // it should be cleaned up by here but check in case
            m_streamingThread.cancel();
            m_streamingThread = null;
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
                case BTMessages.MESSAGE_UPDATE_STREAM_PROGRESS:
                    long newProgress = msg.getData().getLong(BTMessages.STREAM_PROGRESS);
                    if(m_sendingProgress != null) {
                        m_sendingProgress.setProgress((int)newProgress);
                    }
                    break;
                case BTMessages.MESSAGE_UPDATE_STREAM_END:
                    if(m_streamingThread != null) {
                        m_streamingThread.cancel();
                        m_streamingThread = null;
                    }
                    // update the Progressbar
                    if(m_sendingProgress != null) {
                        m_sendingProgress.setProgress(0);
                        m_sendingProgress.setMax(0);
                        m_sendingProgress.setVisibility(View.VISIBLE);
                    }
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
                    // send our response
                    sendResponse(BTMessages.SM_CLIENTCOUNT);
                    // let the client know they connected
                    m_logText.append("-Connected to room: " + m_bluetoothClient.getServerName());
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
                    m_id = MessageParser.parseClientIDData(message);
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
                        byte responsebyte = MessageParser.parseServerResponse(message);
                        if(responsebyte == BTMessages.SM_SONGDATA) {
                            if(m_streamingThread != null) {
                                m_streamingThread.signalStream();
                            }
                        }
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

    private void initBluetoothServiceListener() {
        m_btServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                if (bluetoothProfile != null && i == BluetoothProfile.A2DP) {
//                    boolean a2dpOn = mAudioManager.isBluetoothA2dpOn();

                    // we have a valid profile
                    m_a2dpProfile = (BluetoothA2dp) bluetoothProfile;
                }
            }

            @Override
            public void onServiceDisconnected(int i) {

            }
        };

        // set up the bluetooth a2dp profile
        // TODO: change this to only be done when bluetooth connection is selected
        if(m_bluetoothClient.getBTAdapter() != null) {
            boolean result = m_bluetoothClient.getBTAdapter().getProfileProxy(getActivity(), m_btServiceListener, BluetoothProfile.A2DP);
            if(result) {
                Log.d(TAG, "Acquired A2DP profile");
            }
            else {
                Log.d(TAG, "Failed to get A2DP profile");
            }
        }
    }

    /**
     * This function will take care of getting the song info and passing it to
     * the streaming thread in the format its expecting.
     * @param artist - the artist we picked
     * @param songData - the song info the StreamingThread needs
     */
    public void beginSongStreaming(String artist, Song songData) {
        // send the song info over
        if(m_bluetoothClient != null) {
            sendSongInfo(artist, songData.title);



            long maxSongLength = songData.data;
            byte[] currentSongByteArray = new byte[(int)maxSongLength];
            Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songData.id);
            try {
                InputStream inp = getContext().getContentResolver().openInputStream(fileUri);
                try {
                    inp.read(currentSongByteArray, 0, (int) maxSongLength);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch(FileNotFoundException ex) {
                ex.printStackTrace();
            }
            // lets set up the seind progress bar
            if(m_sendingProgress != null) {
                m_sendingProgress.setVisibility(View.VISIBLE);
                m_sendingProgress.setMax((int)maxSongLength);
                m_sendingProgress.setProgress(0);
            }
            // start sending the bytes until we reached the max for the current song
             if(m_streamingThread == null) {
                 m_streamingThread = new StreamingThread(mHandler, m_bluetoothClient, currentSongByteArray, m_id);
             }
             m_streamingThread.start();
        }
    }

    private void sendResponse(byte messID) {
        if(m_bluetoothClient != null) {
            byte[] out = MessageBuilder.buildMessageResponse(m_id, messID);
            m_bluetoothClient.sendDataToServer(out, false);
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "receive intent for action : " + action);
            if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    Log.d(TAG, "A2DP Connected!");
                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                    Log.d(TAG, "A2DP Not Connected!");

                }
            } else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                if (state == BluetoothA2dp.STATE_PLAYING) {
                    Log.d(TAG, "A2DP start playing");
                } else {
                    Log.d(TAG, "A2DP stop playing");
                }
            }
        }
    };

    public void connectUsingBluetoothA2dp(Context context,
                                          final BluetoothDevice deviceToConnect) {

        try {
            Class<?> c2 = Class.forName("android.os.ServiceManager");
            Method m2 = c2.getDeclaredMethod("getService", String.class);
            IBinder b = (IBinder) m2.invoke(c2.newInstance(), "bluetooth_a2dp");
            if (b == null) {
                // For Android 4.2 Above Devices
                m_bluetoothClient.getBTAdapter().getProfileProxy(context,
                        new BluetoothProfile.ServiceListener() {

                            @Override
                            public void onServiceDisconnected(int profile) {

                            }

                            @Override
                            public void onServiceConnected(int profile,
                                                           BluetoothProfile proxy) {
                                m_a2dpProfile = (BluetoothA2dp) proxy;
                                try {
                                    m_a2dpProfile.getClass()
                                            .getMethod("connect",BluetoothDevice.class)
                                            .invoke(m_a2dpProfile, deviceToConnect);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, BluetoothProfile.A2DP);
            }
            else {
                // For Android below 4.2 devices
                Class<?> c3 = Class.forName("android.bluetooth.IBluetoothA2dp");
                Class<?>[] s2 = c3.getDeclaredClasses();
                Class<?> c = s2[0];
                Method m = c.getDeclaredMethod("asInterface", IBinder.class);
                m.setAccessible(true);

                IBluetoothA2dp a2dp = (IBluetoothA2dp) m.invoke(null, b);
                a2dp.connect(deviceToConnect);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
