package com.liquidcode.jukevox.networking.Messaging;

/**
 * Created by mikev on 6/12/2017.
 */

public class SentMessage {
    private byte m_messageType;
    private byte[] m_messageData;
    private float m_currentTime;
    private float m_delay;

    public SentMessage(byte mess, byte[] data) {
        m_messageType = mess;
        m_messageData = data;
        m_currentTime = 0.0f;
        m_delay = BTMessages.DEFAULT_DELAY;
    }

    public boolean checkSend(float delta) {
        boolean resendMessage = false;
        // add to the current time
        m_currentTime += delta;
        if(m_currentTime > m_delay) {
            // if this time is greater than the delay
            resendMessage = true;
            // reset the current time also
            // this might not actually get the response we want so this will happen until this
            // message gets removed from the queue.
            m_currentTime = 0.0f;
        }
        return resendMessage;
    }

    public byte getMessageID() { return m_messageType; }
    public byte[] getMessageData() { return m_messageData; }
    public float getSendDelay() { return m_delay; }
}
