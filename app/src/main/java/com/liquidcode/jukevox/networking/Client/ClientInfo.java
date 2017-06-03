package com.liquidcode.jukevox.networking.Client;

/**
 * Created by mikev on 6/1/2017.
 */

public class ClientInfo {

    // our clients name
    private String m_clientName;
    // our clients id
    private byte m_ID;

    public ClientInfo() {
        m_ID = -1;
    }

    public String getClientName() { return m_clientName; }
    public byte getClientID() { return m_ID; }

    public void setClientName(String name) {
        m_clientName = name;
    }

    public void setClientID(byte id) {
        m_ID = id;
    }
}
