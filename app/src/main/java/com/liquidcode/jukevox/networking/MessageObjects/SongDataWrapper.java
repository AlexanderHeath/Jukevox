package com.liquidcode.jukevox.networking.MessageObjects;

/**
 * Created by mikev on 7/2/2017.
 */

public class SongDataWrapper {
    private byte m_clientID;
    private byte[] m_songData;

    public SongDataWrapper(byte id, byte[] data) {
        m_clientID = id;
        m_songData = data;
    }

    public byte getClientID() { return m_clientID; }
    public byte[] getSongData() { return m_songData; }
    public int getSongDataSize() { return m_songData.length; }
}
