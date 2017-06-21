package com.liquidcode.jukevox.networking.Server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.liquidcode.jukevox.networking.Client.ClientInfo;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.networking.Messaging.MessageParser;
import com.liquidcode.jukevox.networking.Messaging.SentMessage;
import com.liquidcode.jukevox.networking.Messaging.SentMessageThread;
import com.liquidcode.jukevox.util.BTStates;
import com.liquidcode.jukevox.util.BTUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Class that represents all bluetooth server behavior. This class handles multiple clients
 * as well as the accept thread that waits for incoming connections
 * Created by mikev on 5/23/2017.
 */

public class BluetoothServer {

    private static final String TAG = "BTServer";
    // Bluteooth variables for client/server
    private String LISTEN_NAME = "Jukevox";
    private UUID UUID_RFCOMM_JUKEVOX = UUID.fromString("8201b35e-2fbb-11e7-93ae-92361f002671");
    // our handler to send messages back to the UI thread for various actions
    private Handler m_uiHandler = null;
    // our bluetooth adapter
    private BluetoothAdapter m_btAdapter = null;
    // state variables
    // our connection state
    private int m_state;
    // our listening state
    private int m_listenState;
    // thread that will run to accept incoming connections
    private AcceptThread m_acceptThread = null;
    private HashMap<ClientInfo, ConnectedThread> m_clientConnections = null;
    // the server socket
    private BluetoothServerSocket m_serverSocket = null;
    private byte m_currentClientID = 0;  // start client ID's at 0
    // Thread that runs in the background and resends messages
    private SentMessageThread m_sentThread = null;

    public BluetoothServer(Handler uiHandler) {
        m_uiHandler = uiHandler;
        m_btAdapter = BluetoothAdapter.getDefaultAdapter();
        m_state = BTStates.STATE_NONE;
        m_listenState = BTStates.STATE_NONE;
        m_clientConnections = new HashMap<>();
        m_sentThread = new SentMessageThread(this);
    }

    /**
     * Starts the thread that will listen for client connections
     */
    public synchronized void startServerListen() {
        // stop listening for connections if we were before
        stopServerListen();
        // start the accept thread here
        if(m_acceptThread == null) {
            m_acceptThread = new AcceptThread(false);
        }
        // start the thread
        m_acceptThread.start();
        // start the SentMessageThread too so it can process sent messages
        if(m_sentThread == null) {
            // Create the Sent Thread if we need to
            m_sentThread = new SentMessageThread(this);
        }
        if(m_sentThread != null) {
            m_sentThread.start();
        }
    }

    /**
     * Stops the listening thread
     */
    private synchronized  void stopServerListen() {
        // stop accepting connections
        if(m_acceptThread != null) {
            m_acceptThread.cancel();
            m_acceptThread = null;
        }
        if(m_sentThread != null) {
            m_sentThread.cancel();
            m_sentThread = null;
        }
    }

    /**
     * Ends the accept (listening) thread and closes all opened client connections
     *
     */
    public synchronized void endAllConnections() {
        if (m_acceptThread != null) {
            m_acceptThread.cancel();
            m_acceptThread = null;
        }

        for(HashMap.Entry<ClientInfo, ConnectedThread> clientConnection : m_clientConnections.entrySet()) {
            // close all connections and null out the thread
            clientConnection.getValue().cancel();
        }
        // clear the map
        m_clientConnections.clear();
        // update the states that we are no longer listening nor connected
        m_state = BTStates.STATE_NONE;
        m_listenState = BTStates.STATE_NONE;
    }

    /**
     * Sends a message to a specific client
     * @param clientID - the client
     * @param out - the message in byte array form
     * @param addToSentQueue - if we need to add this message to the sent queue
     */
    public void sendDataToClient(byte clientID, byte[] out, boolean addToSentQueue) {
        ConnectedThread outThread;
        for(ClientInfo info : m_clientConnections.keySet()) {
            // find our client
            if(clientID == info.getClientID()) {
                // send the message
                synchronized (this) {
                    if (m_state != BTStates.STATE_CONNECTED) return;
                    outThread = m_clientConnections.get(info);
                }
                outThread.write(out);
                if(m_sentThread != null && addToSentQueue) {
                    m_sentThread.addMessage(clientID, out);
                }
            }
        }
    }

    /**
     * Sends Message to all clients
     * @param out - the message to send
     * @param addToSentQueue - if we need to add this message to the sent queue or not
     */
    public void sendDataToClients(byte[] out, boolean addToSentQueue) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        for(HashMap.Entry<ClientInfo, ConnectedThread> clientConnection : m_clientConnections.entrySet()) {
            synchronized (this) {
                if (m_state != BTStates.STATE_CONNECTED) return;
                r = clientConnection.getValue();
            }
            // Perform the write unsynchronized
            r.write(out);
            if(m_sentThread != null && addToSentQueue) {
                m_sentThread.addMessage(clientConnection.getKey().getClientID(), out);
            }
        }
    }

    public void handleResponseMessage(byte clientID, byte message) {
        if(m_sentThread != null) {
            m_sentThread.handleResponseMessage(clientID, message);
        }
    }

    private synchronized void connectToClient(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);
        // TODO: Generate client ID a different way?
        byte newClientID = m_currentClientID++;
        // Create a new thread for the incoming connection and start it
        // also add it to the hashmap so we can keep track of it
        ConnectedThread newClientThread = new ConnectedThread(socket, socketType, newClientID);
        String clientName = device.getName();
        // create a new ID for this client and add that to the hashmap
        ClientInfo newClient = new ClientInfo();
        newClient.setClientID(newClientID);
        // this is probably not the correct name but its okay because we will ask the client
        // to send us their real display name
        newClient.setClientName(clientName);
        newClientThread.start();
        m_clientConnections.put(newClient, newClientThread);
        // Send the name of the connected device back to the UI Activity
        // This is the UI Handler from our ServerFragment that we need to update
        Message msg = m_uiHandler.obtainMessage(BTMessages.MESSAGE_CLIENT_DEVICE_CONNECTED);
        // send the client ID to the client we just connected to
        Bundle idBundle = new Bundle();
        idBundle.putByte(BTMessages.CLIENT_ID, newClient.getClientID());
        msg.setData(idBundle);
        m_uiHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void disconnectClient(byte clientID) {
        // cancel this connection thread and remove it
        String clientName;
        ConnectedThread clientThread = null;
        ClientInfo disconnectedClient = null;
        // find the client
        for(HashMap.Entry<ClientInfo, ConnectedThread> clientConnection : m_clientConnections.entrySet()) {
            if(clientConnection.getKey().getClientID() == clientID) {
                clientName = clientConnection.getKey().getClientName();
                clientThread = clientConnection.getValue();
                disconnectedClient = new ClientInfo();
                disconnectedClient.setClientID(clientID);
                disconnectedClient.setClientName(clientName);
            }
        }
        // stop this users ConnectedThread
        if(clientThread != null) {
            clientThread.cancel();
            // now remove from our connected client list
            m_clientConnections.remove(disconnectedClient);
        }

        // if there are no clients changes the state to STATE_NONE
        if(m_clientConnections.isEmpty()) {
            m_state = BTStates.STATE_NONE;
        }
        if(disconnectedClient != null) {
            // Send a message back to the Activity so the UI can be updated and messages can be sent to clients
            Message msg = m_uiHandler.obtainMessage(BTMessages.MESSAGE_USER_DISCONNECT);
            Bundle bundle = new Bundle();
            bundle.putString(BTMessages.CLIENT_NAME, disconnectedClient.getClientName());
            msg.setData(bundle);
            m_uiHandler.sendMessage(msg);
            // now notify all connected clients that the room counter has changed
            byte[] outgoing = new byte[2];
            outgoing[0] = BTMessages.SM_CLIENTCOUNT;
            outgoing[1] = (byte) m_clientConnections.size();
            sendDataToClients(outgoing, true);
        }
    }

    public synchronized void updateClientDisplayName(byte clientid, String displayName) {
        for(ClientInfo info : m_clientConnections.keySet()) {
            // find our client
            if (clientid == info.getClientID()) {
                info.setClientName(displayName);
            }
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private String mSocketType;

        private AcceptThread(boolean secure) {
            mSocketType = "Secure";

            // Create a new listening server socket
            try {
                if(secure) {
                    m_serverSocket = m_btAdapter.listenUsingRfcommWithServiceRecord(LISTEN_NAME, UUID_RFCOMM_JUKEVOX);
                }
                else {
                    m_serverSocket = m_btAdapter.listenUsingInsecureRfcommWithServiceRecord(LISTEN_NAME, UUID_RFCOMM_JUKEVOX);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            m_listenState = BTStates.STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            // our temp client socket
            BluetoothSocket clientSocket;
            // Listen to the server socket if we're not connected
            while (m_listenState != BTStates.STATE_NONE) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    clientSocket = m_serverSocket.accept();
                } catch (IOException e) {
                    if(m_serverSocket != null) {
                        try {
                            m_serverSocket.close();
                            m_listenState = BTStates.STATE_NONE;
                        } catch (IOException ex) {
                            Log.e(TAG, "Failed to close BTServer Socket");
                        }
                    }
                    Log.e(TAG, "Socket Type: " + mSocketType + " accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (clientSocket != null) {
                    synchronized (BluetoothServer.this) {
                        switch (m_listenState) {
                            case BTStates.STATE_LISTEN:
                                // Situation normal. Start the connected thread.
                                connectToClient(clientSocket, clientSocket.getRemoteDevice(), mSocketType);
                                break;
                            case BTStates.STATE_NONE:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    clientSocket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                    // set the clientsocket to null since we want to keep listening
                    synchronized (BluetoothServer.this) {
                        clientSocket = null;
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        private void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + " cancel " + this);
            try {
                if(m_serverSocket != null) {
                    m_serverSocket.close();
                }
                m_listenState = BTStates.STATE_NONE;
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + " close() of server failed", e);
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
        private final byte m_clientId;

        private ConnectedThread(BluetoothSocket socket, String socketType, byte clientID) {
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
            m_clientId = clientID;
            m_state = BTStates.STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[BTUtils.MAX_SOCKET_READ];
            byte[] processBuffer = null;
            int bytesReceived;
            // Keep listening to the InputStream while connected
            while (m_state == BTStates.STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytesReceived = m_inputStream.read(buffer);
                    if(bytesReceived > 0) {
                        // create the new copy buffer that we are going to process messages from
                        processBuffer = new byte[bytesReceived];
                        System.arraycopy(buffer, 0, processBuffer, 0, bytesReceived);
                        // Send the obtained bytes to the UI Activity
                        m_uiHandler.obtainMessage(BTMessages.MESSAGE_READ, bytesReceived, -1, processBuffer)
                                .sendToTarget();

                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    disconnectClient(m_clientId);
                    break;
                }
            }
        }

        private void cancel() {
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

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        private void write(byte[] buffer) {
            try {
                m_outputStream.write(buffer);
                // Share the sent message back to the UI Activity
                m_uiHandler.obtainMessage(BTMessages.MESSAGE_WRITE, -1, buffer.length, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
    }

}
