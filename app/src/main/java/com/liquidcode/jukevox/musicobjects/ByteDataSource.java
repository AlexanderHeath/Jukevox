package com.liquidcode.jukevox.musicobjects;

import android.media.MediaDataSource;

import java.io.IOException;

/**
 * Created by mikev on 7/8/2017.
 */

public class ByteDataSource extends MediaDataSource {

    private byte[] m_data;

    // ctor
    public ByteDataSource(byte[] data) {
        if(data != null) {
            m_data = data;
            m_data = new byte[data.length];
            System.arraycopy(data, 0, m_data, 0, data.length);
        }
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        synchronized (m_data) {
            int length = m_data.length;
            if (position >= length) {
                return -1; // -1 indicates EOF
            }
            if (position + size > length) {
                size -= (position + size) - length;
            }
            System.arraycopy(m_data, (int) position, buffer, offset, size);
        }
        return size;
    }

    @Override
    public long getSize() throws IOException {
        return m_data.length;
    }

    @Override
    public void close() throws IOException {

    }
}
