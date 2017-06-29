package com.liquidcode.jukevox.networking.Messaging;

import android.util.Log;

import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.Server.BluetoothServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by mikev on 6/19/2017.
 */

public class ClientSentMessageThread extends Thread {
    // this is our list of sent messages
    // the key is the client. and the data is a list of SentMessage's
    private ArrayList<SentMessage> m_sentMessageList = null;
    private BluetoothClient m_clientRef;
    private boolean m_running;

    public ClientSentMessageThread(BluetoothClient clientRef) {
        m_sentMessageList = new ArrayList<>();
        m_running = false;
        m_clientRef = clientRef;
    }

    public void addMessage(byte[] data) {
        synchronized (m_sentMessageList) {
            // build a new sent message and add it to the list
            SentMessage newSent = new SentMessage(data[0], data);
            m_sentMessageList.add(newSent);
        }
    }

    public synchronized void handleResponseMessage(byte messageID) {
        synchronized (m_sentMessageList) {
            Iterator<SentMessage> iter = m_sentMessageList.iterator();
            while (iter.hasNext()) {
                SentMessage mess = iter.next();
                if (mess.getMessageID() == messageID) {
                    // we got a response to this message now remove it
                    String messageIDStr;
                    messageIDStr = String.format("(%d)", mess.getMessageID());
                    Log.i("CST", "Removing " + messageIDStr);
                    iter.remove();
                    break;
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
            // now loop through this clients sentMessages and see if we need to resend
            Iterator<SentMessage> iter = m_sentMessageList.iterator();
            while (iter.hasNext()) {
                SentMessage mess = iter.next();
                if (mess != null && mess.checkSend(dt)) {
                    String messageID;
                    messageID = String.format("(%d)", mess.getMessageID());
                    Log.i("CST", "Sending message " + messageID);
                    m_clientRef.sendDataToServer(mess.getMessageData(), false);
                }
            }
        }
    }
}
