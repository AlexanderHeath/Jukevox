package com.liquidcode.jukevox.networking.MessageObjects;

/**
 * Created by mikev on 7/2/2017.
 */

public class SongDataWrapper {
    private byte m_clientID;
    private byte[] m_songData;
    private boolean m_songFinished;
    private int m_currentIndex;

    public SongDataWrapper(byte id, byte[] data, boolean songFinished) {
        m_clientID = id;
        if(data.length > 0) {
            m_songData = new byte[data.length];
            System.arraycopy(data, 0, m_songData, 0, data.length);
        }
        m_songFinished = songFinished;
        m_currentIndex = 0;
    }

    public void updateSongBuffer(byte[] buffer) {
        if(buffer.length > 0) {
            byte[] newBuffer = new byte[m_currentIndex + buffer.length];
            if(m_currentIndex != 0) {
                // copy our existing buffer
                System.arraycopy(m_songData, 0, newBuffer, 0, m_currentIndex);
            }
            // now do the new data
            System.arraycopy(buffer, 0, newBuffer, m_currentIndex, buffer.length);
            // reallocate
            m_songData = new byte[m_currentIndex + buffer.length];
            System.arraycopy(newBuffer, 0, m_songData, 0, newBuffer.length);
            // now increase the currentIndex
            m_currentIndex += buffer.length;
        }
    }

    public byte getClientID() { return m_clientID; }
    public byte[] getSongData() { return m_songData; }
    public int getSongDataSize() { return m_songData.length; }
    public boolean isSongFinshed() { return m_songFinished; }
}
