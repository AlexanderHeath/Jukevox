package com.liquidcode.jukevox.networking.Messaging;

import android.util.Log;

import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.Server.BluetoothServer;
import com.liquidcode.jukevox.util.BTStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by mikev on 6/18/2017.
 */

public class SentMessageThread extends Thread {

    // this is our list of sent messages
    // the key is the client. and the data is a list of SentMessage's
    private HashMap<Byte, ArrayList<SentMessage>> m_sentMessageList = null;
    private BluetoothServer m_serverRef;
    private boolean m_running;

    public SentMessageThread(BluetoothServer serverRef) {
        m_sentMessageList = new HashMap<>();
        m_running = false;
        m_serverRef = serverRef;
    }

    public void addMessage(byte clientID, byte[] data) {
        synchronized (m_sentMessageList) {
            // build a new sent message and add it to the list
            SentMessage newSent = new SentMessage(data[0], data);
            if (m_sentMessageList.containsKey(clientID)) {
                // it exists. get the arraylist and push the message
                m_sentMessageList.get(clientID).add(newSent);
            } else {
                // doesnt exist Create a new array list and add this message
                ArrayList<SentMessage> newList = new ArrayList<>();
                newList.add(newSent);
                m_sentMessageList.put(clientID, newList);
            }
        }
    }

    public synchronized void handleResponseMessage(byte clientID, byte messageID) {
        synchronized (m_sentMessageList) {
            if (m_sentMessageList.containsKey(clientID)) {
                ArrayList<SentMessage> sentMessages = m_sentMessageList.get(clientID);
                Iterator<SentMessage> iter = sentMessages.iterator();
                while (iter.hasNext()) {
                    SentMessage mess = iter.next();
                    if (mess.getMessageID() == messageID) {
                        // we got a response to this message now remove it
                        Log.d("SentMessage", "Removing Message");
                        iter.remove();
                        break;
                    }
                }
            }
        }
    }
    public void run() {
        m_running = true;
        long lastTime = System.currentTimeMillis();
        // Keep listening to the InputStream while connected
        while (m_running) {
            long time = System.currentTimeMillis();
            float dt = (float) ((time - lastTime) / 1000.0);
            updateSentMessages(dt);
            lastTime = time;
        }
    }

    public void cancel() {
        m_running = false;
    }

    public synchronized void updateSentMessages(float dt) {
        synchronized (m_sentMessageList) {
            // go through all the sentMessages and see if we need to resend any of them
            for (HashMap.Entry<Byte, ArrayList<SentMessage>> entry : m_sentMessageList.entrySet()) {
                // now loop through this clients sentMessages and see if we need to resend
                Iterator<SentMessage> iter = entry.getValue().iterator();
                while (iter.hasNext()) {
                    SentMessage mess = iter.next();
                    if (mess != null && mess.checkSend(dt)) {
                        Log.d("SentThread", "Sending message");
                        //m_serverRef.sendDataToClient(entry.getKey(), mess.getMessageData(), false);
                    }
                }
            }
        }
    }
}
