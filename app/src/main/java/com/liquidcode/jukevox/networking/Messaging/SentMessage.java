package com.liquidcode.jukevox.networking.Messaging;

/**
 * Created by mikev on 6/12/2017.
 */

public class SentMessage {
    private byte m_messageType;
    private byte[] m_messageData;

    public SentMessage(byte mess, byte[] data) {
        m_messageType = mess;
        m_messageData = data;
    }

    public byte getMessageID() { return m_messageType; }
    public byte[] getMessageData() { return m_messageData; }
}
