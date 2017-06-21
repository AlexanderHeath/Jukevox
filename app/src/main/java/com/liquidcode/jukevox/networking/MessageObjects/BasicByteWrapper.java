package com.liquidcode.jukevox.networking.MessageObjects;

/**
 * Created by mikev on 6/20/2017.
 */

public class BasicByteWrapper {
    private byte m_clientID;
    private byte m_byteData;

    public BasicByteWrapper(byte id, byte data) {
        m_clientID = id;
        m_byteData = data;
    }

    public byte getClientID() { return m_clientID; }
    public byte getByteData() { return m_byteData; }
}
