package com.liquidcode.jukevox.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.adapters.QueuedSongAdapter;
import com.liquidcode.jukevox.musicobjects.ByteDataSource;
import com.liquidcode.jukevox.networking.MessageObjects.BasicByteWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.BasicStringWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongDataWrapper;
import com.liquidcode.jukevox.networking.MessageObjects.SongInfoWrapper;
import com.liquidcode.jukevox.networking.Messaging.MessageBuilder;
import com.liquidcode.jukevox.networking.Messaging.MessageParser;
import com.liquidcode.jukevox.networking.Server.BluetoothServer;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.util.BTUtils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.media.MediaPlayer;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ServerFragment extends android.support.v4.app.Fragment {

	private static final String TAG = "BTServer";
	/* request BT enable */
	private static final int REQUEST_ENABLE = 0x1;
	private static final int REQUEST_ENABLED_REJECTED = 0x00;
	private String m_roomName = null;
	private BluetoothAdapter m_adapter = null;
	private BluetoothA2dp m_a2dpProfile = null;
	private BluetoothProfile.ServiceListener m_btServiceListener = null;
	private TextView m_logText = null;
	private TextView m_clientCountText = null;
	private ArrayList<BluetoothSocket> m_connectedDevices = null;
	// our bluetoothManager instance to make conecctions and send data with
	private BluetoothServer m_bluetoothServer = null;
	// our connection variables for the UI
	private int m_currentClients = 0; // start at 0 clients
	// Queue list variables
	private QueuedSongAdapter m_queueAdapter = null;
	private ListView m_queueListview = null;
	// list of queued song that we get from the clients
	private ArrayList<SongInfoWrapper> m_queuedSongList = null;
	// list of songs we are processing from the client currently
	private ArrayList<SongInfoWrapper> m_processingList = null;
	private MediaPlayer m_mediaPlayer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(
				R.layout.room_layout, container, false);

		initWidgets(root);
		m_logText.append("-Starting Server...\n");
		if (getArguments() != null) {
			m_roomName = getArguments().getString("roomName");
		}
		if (m_roomName != null) {
			m_adapter = BluetoothAdapter.getDefaultAdapter();
			if (m_adapter != null) {
				// request for the bluetooth to be enabled
				m_logText.append("-Requesting Bluetooth access...\n");
				RequestBluetoothPermission();
			} else {
				m_logText.append("Bluetooth not supported on this device!\n");
				Log.e(TAG, "Bluetooth not supported on this device!\n");
			}
		} else {
			m_logText.append("Failed to create room!\n");
			((JukevoxMain) getActivity()).removeCurrentFragment();
		}
		return root;
	}

	public void initializeRoom() {
		m_currentClients = 0;
		if (m_queuedSongList != null) {
			m_queuedSongList.clear();
		}
		if (m_processingList != null) {
			m_processingList.clear();
		}
		if (m_queueAdapter != null) {
			m_queueAdapter.notifyDataSetChanged();
		}
		if (m_mediaPlayer == null) {
			m_mediaPlayer = new MediaPlayer();
			m_mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					m_mediaPlayer.start();
				}
			});
		}
	}

	private void initWidgets(ViewGroup root) {
		// if the arraylist of songInfo is null create it
		if (m_queuedSongList == null) {
			m_queuedSongList = new ArrayList<>();
		}
		if (m_processingList == null) {
			m_processingList = new ArrayList<>();
		}
		// init the listview and adapter
		m_queueListview = (ListView) root.findViewById(R.id.room_song_list);
		if (m_queueListview != null) {
			// create the adapter that we will notify changes with
			if (m_queueAdapter == null) {
				m_queueAdapter = new QueuedSongAdapter(getActivity(), m_queuedSongList);
			}
			m_queueListview.setAdapter(m_queueAdapter);
		}
		// init our text widgets here
		initTextWidgets(root);
	}

	private void initTextWidgets(ViewGroup root) {
		m_logText = (TextView) root.findViewById(R.id.serverLogText);
		m_logText.setMovementMethod(new ScrollingMovementMethod());
		m_clientCountText = (TextView) root.findViewById(R.id.clientCountText);
	}

	private void updateClientCount() {
		String formattedText = String.format(Locale.ENGLISH, "%d/%d", m_currentClients, BTUtils.MAX_BT_CLIENTS);
		// now set the text for the textview
		m_clientCountText.setText(formattedText);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE && resultCode != REQUEST_ENABLED_REJECTED) {
			m_adapter.enable();
			// set the bluetooth device name to the room name chosen
			m_adapter.setName(m_roomName);
			m_logText.append("-Room Name: " + m_roomName + "\n");
			m_logText.append("-Server is now discoverable!\n");
			m_logText.append("-Listening on RFCOMM Channel.\n");
			m_logText.append("-Waiting for connections.\n");
			// if the BluetoothManager is null. create it.
			if (m_bluetoothServer == null) {
				m_bluetoothServer = new BluetoothServer(mHandler);
			}
			// try and get the A2DP profile
			initBluetoothServiceListener();
			// start the listener thread
			m_bluetoothServer.startServerListen();
		} else {
			((JukevoxMain) getActivity()).removeCurrentFragment();
		}
	}

	private void initBluetoothServiceListener() {
		m_btServiceListener = new BluetoothProfile.ServiceListener() {
			@Override
			public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
				if (bluetoothProfile != null && i == BluetoothProfile.A2DP) {
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
		if (m_adapter != null) {
			boolean result = m_adapter.getProfileProxy(getActivity(), m_btServiceListener, BluetoothProfile.A2DP);
			if (result) {
				Log.d(TAG, "Acquired A2DP profile");
			} else {
				Log.d(TAG, "Failed to get A2DP profile");
			}
		}
	}

	private void RequestBluetoothPermission() {
		Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivityForResult(i, REQUEST_ENABLE);
	}

	/**
	 * The Handler that gets information back from the BluetoothChatService
	 */
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case BTMessages.MESSAGE_CLIENT_DEVICE_CONNECTED: {
					// save the connected device's name
					byte newClientID = msg.getData().getByte(BTMessages.CLIENT_ID);
					// send a message to this client, with their ID, and requesting their display name
					byte[] outgoing = MessageBuilder.buildClientIdData(newClientID);
					if (m_bluetoothServer != null) {
						// send the client his ID
						m_bluetoothServer.sendDataToClient(newClientID, outgoing, true);
					}
					break;
				}
				case BTMessages.MESSAGE_READ: {
					byte[] readBuf = (byte[]) msg.obj;
					// construct a string from the valid bytes in the buffer
					processIncomingMessage(readBuf);
					break;
				}
				case BTMessages.MESSAGE_STATE_CHANGE: {
					// not implemented
					break;
				}
				case BTMessages.MESSAGE_TOAST: {
					if (null != getActivity()) {
						Toast.makeText(getActivity(), msg.getData().getString(BTMessages.TOAST),
								Toast.LENGTH_SHORT).show();
					}
					break;
				}
				case BTMessages.MESSAGE_USER_DISCONNECT: {
					// decrease the client count
					String username = msg.getData().getString(BTMessages.CLIENT_NAME);
					m_logText.append("-User Disconnected: " + username + "\n");
					// decrement client connections we have
					--m_currentClients;
					// update our UI
					updateClientCount();
					// notify clients that we have a new client connected with us
					// now notify all connected clients that the room counter has changed
					byte[] outgoing = MessageBuilder.buildClientCountData(m_currentClients);
					if (m_bluetoothServer != null) {
						m_bluetoothServer.sendDataToClients(outgoing, true);
					}
					break;
				}
			}
		}
	};

	private void processIncomingMessage(byte[] buffer) {
		ArrayList<byte[]> messages = MessageParser.splitMessages(buffer);
		for (byte[] message : messages) {
			// message[0] is always the byte that tells us what message this is ALWAYS
			switch (message[0]) {
				case BTMessages.SM_SONGINFO: {
					SongInfoWrapper songinfo = MessageParser.parseSongInfo(message);
					if (songinfo != null) {
						// add to the processing list
						addNewSongInfo(songinfo);
						if (m_bluetoothServer != null) {
							// send the songinfo response so they stop sending this message
							m_bluetoothServer.sendDataToClient(songinfo.getClientID(), MessageBuilder.buildMessageResponse(BTMessages.SM_SONGINFO), false);

						}
						m_logText.append("-" + songinfo.getArtist() + " - " + songinfo.getSongName() + "\n");
					}
					break;
				}
				case BTMessages.SM_SONGDATA: {
					SongDataWrapper songData = MessageParser.parseSongData(message);
					// update the existing buffer in the prcessinglist
					updateSongData(songData);
					if (songData != null && m_bluetoothServer != null) {
						m_bluetoothServer.sendDataToClient(songData.getClientID(), MessageBuilder.buildMessageResponse(BTMessages.SM_SONGDATA), false);
					}

					break;
				}
				case BTMessages.SM_INFO: {
					BasicStringWrapper info = MessageParser.parseInfoData(message);
					m_logText.append("Info: " + info.getStringData() + "\n");
					// send the response

					break;
				}
				case BTMessages.SM_CLIENTDISPLAYNAME: {
					// a client is sending its name to us. parse it and notify of new connection
					BasicStringWrapper newclient = MessageParser.parseClientDisplayName(message);
					if (newclient != null) {
						// we got a valid client
						if (m_bluetoothServer != null) {
							m_bluetoothServer.updateClientDisplayName(newclient.getClientID(), newclient.getStringData());
							// send response to client
							m_bluetoothServer.sendDataToClient(newclient.getClientID(), MessageBuilder.buildMessageResponse(BTMessages.SM_CLIENTDISPLAYNAME), false);
							m_logText.append("User: " + newclient.getStringData() + " joined the room!\n");
							// increase the number of connected clients
							++m_currentClients;
							// now lets tell the clients that a new client connected
							m_bluetoothServer.sendDataToClients(MessageBuilder.buildClientCountData(m_currentClients), true);
							updateClientCount();
						}
					}
					break;
				}
				case BTMessages.SMR_RESPONSE: {
					// Handle the responses
					BasicByteWrapper response = MessageParser.parseResponse(message);
					if (m_bluetoothServer != null) {
						m_bluetoothServer.handleResponseMessage(response.getClientID(), response.getByteData());
					}
					break;
				}
				default: {
					m_logText.append("Unsupported message type received!\n");
					break;
				}
			}
		}
	}

	private void addNewSongInfo(SongInfoWrapper info) {
		boolean addSong = true;
		for (SongInfoWrapper existing : m_processingList) {
			if (info.getClientID() == existing.getClientID()) {
				addSong = false;
				break; // this client is already sending us something
			}
		}
		if (addSong) {
			m_processingList.add(info);
		}
	}

	private void updateSongData(SongDataWrapper data) {
		for (SongInfoWrapper existing : m_processingList) {
			if (existing.getClientID() == data.getClientID()) {
				existing.addToBuffer(data.getSongData());
				// we updated the buffer lets see if this should be removed from the processing list
				// and added to the queued list
				if (data.isSongFinshed()) {
					addSongToQueueAndNotifyClients(existing);
					// remove from this list
					m_processingList.remove(existing);
					playSong(existing.getSongData().getSongData());
				}
				break;
			}
		}
	}

	private void playSong(byte[] songData) {
		try {
			// create temp file that will hold byte array
			File tempMp3 = File.createTempFile("jukevox", "mp3", getActivity().getCacheDir());
			tempMp3.deleteOnExit();
			FileOutputStream fos = new FileOutputStream(tempMp3);
			fos.write(songData);
			fos.close();

			// resetting mediaplayer instance to evade problems
			m_mediaPlayer.reset();

			// Tried passing path directly, but kept getting
			// "Prepare failed.: status=0x1"
			// so using file descriptor instead
			FileInputStream fis = new FileInputStream(tempMp3);
			m_mediaPlayer.setDataSource(fis.getFD());

			m_mediaPlayer.prepare();
			m_mediaPlayer.start();
		} catch (IOException ex) {
			String s = ex.toString();
			ex.printStackTrace();
		}
	}

	public void addSongToQueueAndNotifyClients(SongInfoWrapper songinfo) {
		// update the queued list to reflect the new song we received from a client
		m_queuedSongList.add(songinfo);
		m_queueAdapter.notifyDataSetChanged();
		m_queueListview.setAdapter(m_queueAdapter);
		// send this buffer to all clients since the data was good
		// this will build our queue of songs upon being received.
		// if there is no song playing there should be a follow up to this message that
		// send the new song to the clients
		byte[] message = MessageBuilder.buildSongInfo(songinfo.getClientID(), songinfo.getArtist(), songinfo.getSongName());
		m_bluetoothServer.sendDataToClients(message, true);
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
		if(m_mediaPlayer != null && m_mediaPlayer.isPlaying()) {
			m_mediaPlayer.stop();
			m_mediaPlayer.release();
		}
	}
}
