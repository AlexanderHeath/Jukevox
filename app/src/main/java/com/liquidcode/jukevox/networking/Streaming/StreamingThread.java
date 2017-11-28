package com.liquidcode.jukevox.networking.Streaming;

import android.content.ContentUris;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;

import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;
import com.liquidcode.jukevox.networking.Messaging.BTMessages;
import com.liquidcode.jukevox.networking.Messaging.MessageBuilder;
import com.liquidcode.jukevox.util.BTUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Streaming thread that handles sending the song chunk data to the server
 * Created by mikev on 11/27/2017.
 */
public class StreamingThread extends Thread {

    // data variables
    private BluetoothClient m_bluetoothClient = null;
    private boolean m_running = true;
    private Handler m_uiHandler = null;
    private byte m_clientID = -1;
    // Variables that keep track of the clients current song its streaming to the server
    private long m_currentPosition; // how much data we've sent so far
    private long m_maxSongLength; // how much data this song is
    private byte[] m_currentSongByteArray;
    private boolean m_currentSongDone;
    private boolean m_sendNextChunk = false;

    public StreamingThread(Handler uiHandler, BluetoothClient clientRef, byte[] songData, byte clientID) {
        m_uiHandler = uiHandler;
        m_bluetoothClient = clientRef;
        m_currentPosition = 0;
        m_maxSongLength = songData.length;
        // allocate and copy the song data buffer
        if(m_currentSongByteArray == null) {
            m_currentSongByteArray = new byte[songData.length];
        }
        System.arraycopy(songData, 0, m_currentSongByteArray, 0, songData.length);
        m_currentSongDone = false;
        m_clientID = clientID;
    }

    @Override
    public void run() {

        m_sendNextChunk = true;
        while(m_running || !m_currentSongDone) {
            if(m_sendNextChunk) {
                streamNextChunk();
                m_sendNextChunk = false;
            }
        }
    }

    /**
     * Stop the thread
     * NOTE: Should call interrupt later and check the status of that
     * NOTE: https://stackoverflow.com/questions/8505707/android-best-and-safe-way-to-stop-thread
     */
    public void cancel() {
        m_running = false;
    }

    private void streamNextChunk() {
        if(m_bluetoothClient != null) {
            // figure out the chunk size
            long chunkSize = 0;
            if((m_currentPosition + BTUtils.SONG_CHUNK_SIZE) <= m_maxSongLength) {
                chunkSize = BTUtils.SONG_CHUNK_SIZE;
                m_currentSongDone = false;
            }
            else {
                chunkSize = m_maxSongLength - m_currentPosition;
                m_currentSongDone = true;
            }
            if(chunkSize > 0) {
                // send the next chunk of data
                byte[] nextChunk = new byte[(int)chunkSize];
                System.arraycopy(m_currentSongByteArray, (int)m_currentPosition, nextChunk, 0, (int)chunkSize);
                // send this new byte array
                byte[] newMessage = MessageBuilder.buildSongData(m_clientID, nextChunk, m_currentSongDone);
                m_bluetoothClient.sendDataToServer(newMessage, false);
                // adjust our position
                m_currentPosition += chunkSize;
                // call the handler here to update the progess bar UI on the main thread
                if(!m_currentSongDone) {
                    Message msg = m_uiHandler.obtainMessage(BTMessages.MESSAGE_UPDATE_STREAM_PROGRESS);
                    Bundle msgData = new Bundle();
                    msgData.putLong(BTMessages.STREAM_PROGRESS, m_currentPosition);
                    msg.setData(msgData);
                    m_uiHandler.sendMessage(msg);
                }
                else {
                    // Clear the progress bar on the UI thread so since we are done
                    m_uiHandler.obtainMessage(BTMessages.MESSAGE_UPDATE_STREAM_END).sendToTarget();
                }
            }
        }
    }

    public void signalStream() {
        m_sendNextChunk = true;
    }
}
