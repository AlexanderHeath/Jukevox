package com.liquidcode.jukevox.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import static android.app.Activity.RESULT_OK;

public class ClientFragment extends android.support.v4.app.Fragment {

	private static final String TAG = "BTClient";
	/* request BT enable */
	private static final int  REQUEST_ENABLE = 0x01;
	/* request permissions to use bluetooth device discovery */
	private static final int REQUEST_DEVICE_DISCOVERY = 0x03;

	// our list adapter for potential rooms
	private ArrayAdapter m_deviceListAdapter = null;
	private BluetoothAdapter m_btAdapter = null;
	// map of found devices
	private HashMap<String, BluetoothDevice> m_btDevices = new HashMap<>();
	// list of the names to be displayed in the list
	private ArrayList<String> m_btDeviceNames = new ArrayList<>();
	private ListView m_deviceListView = null;
	private BluetoothDevice m_currentDevice = null;
	// our BluetoothManager instance
	private BluetoothClient m_bluetoothClient = null;
	// temporary EditText to test message sending
	private EditText m_editText = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(
				R.layout.server_list_layout, container, false);

		m_deviceListView = (ListView)root.findViewById(R.id.bluetoothList);
		// get the edit text button
		m_editText = (EditText)root.findViewById(R.id.sendText);
		m_editText.setOnEditorActionListener(mWriteListener);
		// get the bluetooth adapter and enable it if it isnt already
		m_btAdapter = BluetoothAdapter.getDefaultAdapter();
        // Register the onItemClickListener()
        initializeBTDeviceListview();
        if(!m_btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE);
        }
        else {
            checkPermissionAndRegisterReceivers();
        }
		return root;
	}

	/**
	 *  Initialize onItemClickListener for the device list
	 *  This will set our current server device based on position in the m_btDevice list
	 */
	private void initializeBTDeviceListview()
	{
		// init the bluetoothManager
		if(m_bluetoothClient == null) {
			m_bluetoothClient = new BluetoothClient(mHandler);
		}
		// set the adapter we are going to continually update
		m_deviceListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, m_btDeviceNames);
		m_deviceListView.setAdapter(m_deviceListAdapter);
		// set onItemClickListener
		m_deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
				Intent result = new Intent();
				result.putExtra(BluetoothDevice.EXTRA_DEVICE, m_btDevices.get(position));
				getActivity().setResult(RESULT_OK, result);
				if(m_btAdapter.isDiscovering()) {
					// if we clicked on a device to connect to stop discovering
					m_btAdapter.cancelDiscovery();
				}
				// get the name (key) from the arraylist
				String deviceKey = m_btDeviceNames.get(position);
				if(deviceKey != null) {
					m_currentDevice = m_btDevices.get(deviceKey);
					// make sure we grabbed a valid BluetoothDevice object to connect to
					if(m_currentDevice != null) {
						Toast.makeText(getActivity(), "Connecting to Server: " + m_currentDevice.getName(), Toast.LENGTH_SHORT).show();
						// update the bluetooth clients handler to ours
						// there's a possibility that it was updated the the ClientJoinedFragments
						// handler if we already connected to a room
						m_bluetoothClient.updateUIHandler(mHandler);
						// create the connect thread and try to connect to the server
						// try secure connect first
						m_bluetoothClient.connectToServer(m_currentDevice, false);
						// if that failed try with an insecure method
//						if(m_bluetoothManager.getCurrentState() != BTStates.STATE_CONNECTED) {
//							m_bluetoothManager.connectToServer(m_currentDevice, false);
//						}
					}
					else {
						// there was a problem and our BluetoothDevice object was null in the hashmap BADDDDDD!!!!!
						Toast.makeText(getActivity(), "BluetoothDevice was NULL!", Toast.LENGTH_SHORT).show();
						((JukevoxMain)getActivity()).removeCurrentFragment();
					}
				}
				else {
					Toast.makeText(getActivity(), "Problem selecting BT Device! (NULL ROOM NAME)", Toast.LENGTH_SHORT).show();
					((JukevoxMain)getActivity()).removeCurrentFragment();
				}
			}
		});
	}

	/**
	 * Receiver
	 * When the discovery finished be called.
	 */
	private BroadcastReceiver btReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				/* get the search results */
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// add bluetooth device to list
				String key = device.getName();
				// check to see if this device exists in the list. if not add it
				if(key != null && !m_btDevices.containsKey(key)) {
					m_btDevices.put(key, device);
					// notify the list adapter of the change
					m_btDeviceNames.add(key);
					m_deviceListAdapter.notifyDataSetChanged();
				}
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Toast.makeText(getActivity(), "BT Discovery Started!", Toast.LENGTH_SHORT).show();
			}
			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Toast.makeText(getActivity(), "BT Discovery Finished!", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private void RegisterReceivers()
	{
		/* Register Receiver*/
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		getActivity().registerReceiver(btReceiver, filter);
	}

	private void checkPermissionAndRegisterReceivers() {
        // check for the discovery permissions and start discovering
        checkDiscoveryPermissions();
        // set up the bluetooth receivers
        RegisterReceivers();
    }

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("EF-BTBee", ">>unregisterReceiver");
		// try to unregister the receiver but wrap in a try/catch if it isnt registered
		try {
			getActivity().unregisterReceiver(btReceiver);
		}
		catch(IllegalArgumentException ex) {
			// fail gracefully
			Log.d(TAG, "Receiver not registered but that's ok!");
		}
		if(m_btAdapter.isDiscovering()) {
			m_btAdapter.cancelDiscovery();
		}
		// stop the connection thread
		if(m_bluetoothClient != null) {
			m_bluetoothClient.stopConnectionThread();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQUEST_ENABLE) {
			if(resultCode == RESULT_OK) {
				// check for permissions and register the receivers
                checkPermissionAndRegisterReceivers();
			}
			else {
				((JukevoxMain)getActivity()).removeCurrentFragment();
			}
		}
	}

	private void checkDiscoveryPermissions()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
			switch (ContextCompat.checkSelfPermission(getActivity().getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
				case PackageManager.PERMISSION_DENIED:
					((TextView) new AlertDialog.Builder(getActivity())
							.setTitle("Runtime Permissions up ahead")
							.setMessage(Html.fromHtml("<p>To find nearby bluetooth devices please click \"Allow\" on the runtime permissions popup.</p>" +
									"<p>For more info see <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">here</a>.</p>"))
							.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (ContextCompat.checkSelfPermission(getActivity().getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
										ActivityCompat.requestPermissions(getActivity(),
												new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
												REQUEST_DEVICE_DISCOVERY);
									}
								}
							})
							.show()
							.findViewById(android.R.id.message))
							.setMovementMethod(LinkMovementMethod.getInstance());       // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
					break;
				case PackageManager.PERMISSION_GRANTED:
					startBluetoothDiscovery();
					break;
			}
		}
		else {
			// older devices dont need this permissions check so just start the discovery
			startBluetoothDiscovery();
		}
	}

	private void startBluetoothDiscovery() {
		if (m_btAdapter != null) {
			if (m_btAdapter.isDiscovering()) {
				m_btAdapter.cancelDiscovery();
			}
			/* Start search device */
			m_btAdapter.startDiscovery();
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
					// we successfully connected
					// call the clientjoined fragment
					((JukevoxMain)getActivity()).createClientJoinedRoomFragment(m_bluetoothClient);
					break;
				case BTMessages.MESSAGE_READ:
					// not implemented
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
						Toast.makeText(getActivity(), "Disconnected from: " + msg.getData().getString(BTMessages.SERVER_NAME),
								Toast.LENGTH_SHORT).show();
					}
					break;
			}
		}
	};

	/**
	 * The action listener for the EditText widget, to listen for the return key
	 */
	private TextView.OnEditorActionListener mWriteListener
			= new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			// consume the ACTION_DOWN key press
			if(event != null) {
				if(actionId == EditorInfo.IME_ACTION_DONE && event.getAction() == KeyEvent.ACTION_DOWN) {
					return true;
				}
				else if(actionId == EditorInfo.IME_ACTION_DONE && event.getAction() == KeyEvent.ACTION_UP) {
					String message = view.getText().toString();
					if(m_bluetoothClient != null) {
						// Check that there's actually something to send
						if (message.length() > 0) {
							// Get the message bytes
							byte[] send = message.getBytes();
							m_bluetoothClient.sendDataToServer(send);
							// Reset out string buffer to zero and clear the edit text field
							m_editText.setText("");
						}
					}
				}
			}
			else if(actionId == EditorInfo.IME_ACTION_DONE) {
				String message = view.getText().toString();
				if(m_bluetoothClient != null) {
					// Check that there's actually something to send
					if (message.length() > 0) {
						// Get the message bytes
						byte[] send = message.getBytes();
						m_bluetoothClient.sendDataToServer(send);
						// Reset out string buffer to zero and clear the edit text field
						m_editText.setText("");
					}
				}
			}
			return true;
		}
	};
}
