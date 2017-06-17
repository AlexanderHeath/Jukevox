package com.liquidcode.jukevox.networking.MessageObjects;

/**
 * Created by mikev on 6/17/2017.
 */

public class BasicStringWrapper {
    private byte m_clientID;
    private String m_info;

    public BasicStringWrapper(String data, byte id) {
        m_info = data;
        m_clientID = id;
    }

    public byte getClientID() { return m_clientID; }
    public String getStringData() { return m_info; }
}
