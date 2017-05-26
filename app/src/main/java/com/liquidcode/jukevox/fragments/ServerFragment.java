package com.liquidcode.jukevox.fragments;

import java.util.ArrayList;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.networking.Server.BluetoothServer;
import com.liquidcode.jukevox.util.BTMessages;
import com.liquidcode.jukevox.util.BTUtils;

import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ServerFragment extends android.support.v4.app.Fragment {

	private static final String TAG = "BTServer";
	/* request BT enable */
	private static final int REQUEST_ENABLE = 0x1;
	private static final int REQUEST_ENABLED_REJECTED = 0x00;
	private String mRoomName = null;
	private BluetoothAdapter mAdapter = null;
	private TextView m_logText = null;
	private TextView m_clientCountText = null;
	private ArrayList<BluetoothSocket> m_connectedDevices = null;
    // our bluetoothManager instance to make conecctions and send data with
    private BluetoothServer m_bluetoothServer = null;
	// our connection variables for the UI
	private int m_currentClients = 0; // start at 0 clients

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(
				R.layout.room_layout, container, false);

		initTextWidgets(root);
		m_logText.append("-Starting Server...\n");
		if(getArguments() != null) {
			mRoomName = getArguments().getString("roomName");
		}
		if(mRoomName != null)
		{
			mAdapter = BluetoothAdapter.getDefaultAdapter();
			if(mAdapter != null) {
				// request for the bluetooth to be enabled
				m_logText.append("-Requesting Bluetooth access...\n");
				RequestBluetoothPermission();
			}
			else {
				m_logText.append("Bluetooth not supported on this device!\n");
				Log.e(TAG, "Bluetooth not supported on this device!\n");
			}
		}
		else
		{
			m_logText.append("Failed to create room!\n");
			((JukevoxMain)getActivity()).removeCurrentFragment();
		}
		return root;
	}

	private void initTextWidgets(ViewGroup root) {
		m_logText = (TextView)root.findViewById(R.id.serverLogText);
		m_logText.setMovementMethod(new ScrollingMovementMethod());
		m_clientCountText = (TextView)root.findViewById(R.id.clientCountText);
	}

	private void updateClientCount() {
		String formattedText = String.format("%d/%d", m_currentClients, BTUtils.MAX_BT_CLIENTS);
		// now set the text for the textview
		m_clientCountText.setText(formattedText);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == REQUEST_ENABLE && resultCode != REQUEST_ENABLED_REJECTED)
		{
            mAdapter.enable();
			// set the bluetooth device name to the room name chosen
			mAdapter.setName(mRoomName);
			m_logText.append("-Server is now discoverable!\n");
			m_logText.append("-Listening on RFCOMM Channel.\n");
			m_logText.append("-Waiting for connections.\n");
            // if the BluetoothManager is null. create it.
            if(m_bluetoothServer == null) {
                m_bluetoothServer = new BluetoothServer(mHandler);
            }
            // start the listener thread
            m_bluetoothServer.startServerListen();
		}
		else
		{
			((JukevoxMain)getActivity()).removeCurrentFragment();
		}
	}
	
	private void RequestBluetoothPermission()
	{
		Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivityForResult(i, REQUEST_ENABLE);
	}

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case BTMessages.MESSAGE_CLIENT_DEVICE_NAME:
                    // make a toast with our server we connected to
                    // save the connected device's name
                    String clientName = msg.getData().getString(BTMessages.CLIENT_NAME);
                    m_logText.append("-User Conntected: " + clientName + "\n");
					++m_currentClients;
					updateClientCount();
                    break;
				case BTMessages.MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					processIncomingMessage(readBuf, readBuf.length);
					break;
				case BTMessages.MESSAGE_STATE_CHANGE:
					// not implemented
					break;
				case BTMessages.MESSAGE_TOAST:
					if (null != getActivity()) {
						Toast.makeText(getActivity(), msg.getData().getString(BTMessages.TOAST),
								Toast.LENGTH_SHORT).show();
					}
					break;
				case BTMessages.MESSAGE_USER_DISCONNECT:
					// decrease the client count
					String username = msg.getData().getString(BTMessages.CLIENT_NAME);
					m_logText.append("-User Disconnected: " + username + "\n");
					--m_currentClients;
					updateClientCount();
					break;
            }
        }
    };

	private void processIncomingMessage(byte[] buffer, int size) {

		// check the first byte for message type
		int currentIndex = 0;
		switch(buffer[currentIndex]) {
			case BTMessages.SM_SONGINFO:
				// split on our delimeter
				break;
			case BTMessages.SM_SONGDATA:
				// not implemented yet
				// this will be where we take our streamed data and send it to the media service's  AudioTrack
				break;
			default:
				m_logText.append("Unable to read incoming message. Ignoring");
				break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(m_bluetoothServer != null) {
			m_bluetoothServer.endAllConnections();
		}
	}
}
