package com.liquidcode.jukevox.networking.Client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.liquidcode.jukevox.util.BTMessages;
import com.liquidcode.jukevox.util.BTStates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by mikev on 5/23/2017.
 */

public class BluetoothClient {

    private static final String TAG = "BTClient";
    // Bluteooth variables for client/server
    private String LISTEN_NAME = "Jukevox";
    private UUID UUID_RFCOMM_JUKEVOX = UUID.fromString("8201b35e-2fbb-11e7-93ae-92361f002671");
    // our connection thread
    private ConnectThread m_connectThread  = null;
    // our connected thread that sends/receives messages
    private ConnectedThread m_connectedThread = null;
    // state variables
    private int m_state;
    // the device we want to connect to
    private BluetoothDevice m_serverDevice = null;
    // our bluetooth adapter
    private BluetoothAdapter m_btAdapter = null;
    // our handler to send messages back to the UI thread for various actions
    private Handler m_uiHandler = null;
    // client socket that we opened with our current server
    private BluetoothSocket m_clientSocket = null;


    // constructor
    public BluetoothClient(Handler uiHandler) {
        m_uiHandler = uiHandler;
        m_btAdapter = BluetoothAdapter.getDefaultAdapter();
        m_state = BTStates.STATE_NONE;
    }

    // our interface to connect to a device
    public synchronized boolean connectToServer(BluetoothDevice serverDevice, boolean secure) {
        boolean status = true;
        if(serverDevice == null) {
            status = false;
        }
        else {
            // setup the connect thread and start it so we can try to connect to a server
            m_serverDevice = serverDevice;
            // stop the connection thread if it was started
            disconnectFromServer();
            if(m_connectThread == null) {
                // lets create the thread
                m_connectThread = new ConnectThread(secure);
            }
            // start the thread
            m_connectThread.start();
        }
        return status;
    }

    public synchronized  void stopConnectionThread() {
        if(m_connectThread != null) {
            m_connectThread.cancel();
            m_connectThread = null;
        }
    }

    public synchronized void disconnectFromServer() {
        if(m_connectThread != null) {
            m_connectThread.cancel();
            m_connectThread = null;
        }

        if(m_connectedThread != null) {
            m_connectedThread.cancel();
            m_connectedThread = null;
        }
    }

    public void sendDataToServer(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (m_state != BTStates.STATE_CONNECTED) return;
            r = m_connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(String serverName) {
        // Send a failure message back to the Activity
        Message msg = m_uiHandler.obtainMessage(BTMessages.MESSAGE_USER_DISCONNECT);
        Bundle bundle = new Bundle();
        bundle.putString(BTMessages.SERVER_NAME, serverName);
        msg.setData(bundle);
        m_uiHandler.sendMessage(msg);
    }

    /**
     * We connected to our server
     * @param socket - server socket we opened
     * @param device - server name
     * @param socketType - BT type
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);
        // Cancel the thread that completed the connection
        if (m_connectThread != null) {
            m_connectThread.cancel();
            m_connectThread = null;
        }

        // update the state to connected
        m_state = BTStates.STATE_CONNECTED;
        // Start the thread to manage the connection and perform transmissions
        m_connectedThread = new ConnectedThread(socket, socketType);
        m_connectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = m_uiHandler.obtainMessage(BTMessages.MESSAGE_CONNECTED_TO_SERVER);
        Bundle bundle = new Bundle();
        bundle.putString(BTMessages.SERVER_NAME, device.getName());
        msg.setData(bundle);
        m_uiHandler.sendMessage(msg);
    }

    public void updateUIHandler(Handler uiHandler) {
        m_uiHandler = uiHandler;
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private String mSocketType;

        public ConnectThread(boolean secure) {
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if(secure) {
                    m_clientSocket = m_serverDevice.createRfcommSocketToServiceRecord(UUID_RFCOMM_JUKEVOX);
                }
                else {
                    m_clientSocket = m_serverDevice.createInsecureRfcommSocketToServiceRecord(UUID_RFCOMM_JUKEVOX);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            m_state = BTStates.STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            m_btAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                m_clientSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    m_clientSocket.close();
                    m_state = BTStates.STATE_NONE;
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothClient.this) {
                m_connectThread = null;
            }

            // Start the connected thread
            connected(m_clientSocket, m_serverDevice, mSocketType);
        }

        public void cancel() {
            try {
                m_clientSocket.close();
                m_state = BTStates.STATE_NONE;
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream m_inputStream;
        private final OutputStream m_outputStream;
        private final String m_deviceName;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            m_deviceName = socket.getRemoteDevice().getName();
            m_inputStream = tmpIn;
            m_outputStream = tmpOut;
            m_state = BTStates.STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (m_state == BTStates.STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = m_inputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    m_uiHandler.obtainMessage(BTMessages.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(m_deviceName);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                m_outputStream.write(buffer);
                // Share the sent message back to the UI Activity
                m_uiHandler.obtainMessage(BTMessages.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                // close the input/output stream first
                m_inputStream.close();
                m_outputStream.close();
                // now close the socket
                mmSocket.close();
                m_state = BTStates.STATE_NONE;
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
