package com.liquidcode.jukevox.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.liquidcode.jukevox.R;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.services.MediaPlayerService;
import com.liquidcode.jukevox.util.MessageIntents;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * MediaPlayerFragment
 * Notes: This class is in charge of the MediaPlayer UI and all the interations between
 *          the user and the MediaPlayer class.
 * Created by mikev on 11/12/2016.
 */
public class MediaPlayerFragment extends Fragment {
    // request code for broadcasts
    private final int NOTIF_REQUEST_CODE = 123123;
    // tag for this fragment
    private String m_fragmentTag = null;
    // List of songs that we want to play
    private ArrayList<Song> m_songList = null;
    private Context m_context = null;
    private String m_artistName = null;
    // the current song index in the list of songs
    private int m_startSongIndex = -1;
    // buttons for the media player view
    private Button m_playButton = null;
    private SeekBar m_seekBar = null;
    // textview for the songs duration
    private TextView m_durationText = null;
    // textview for the song name
    TextView m_songText = null;
    // imageview for the album art
    private ImageView m_albumArt = null;
    // URI location for the album artwork
    private Uri m_albumArtURI = null;
    // our service connection to bind
    private ServiceConnection m_serviceConnection = null;
    private boolean m_boundToService = false; // are we bound
    // our service we received from binding to the MediaPlayerService
    private MediaPlayerService m_mediaPlayerService = null;
    // our broadcast receiver
    private BroadcastReceiver m_receiver = null;
    // our current seekbar location
    private long m_currentSeekLocation = 0;
    // variable that keeps track of whether we are paused or not
    private boolean m_isPaused = false;
    // the intent filters we can use to register our receiver
    IntentFilter prevFilter = new IntentFilter(MessageIntents.PREV_SONG_ACTION);
    IntentFilter nextFilter = new IntentFilter(MessageIntents.NEXT_SONG_ACTION);
    IntentFilter toggleFilter = new IntentFilter(MessageIntents.TOGGLE_PLAY_ACTION);
    IntentFilter quitPlayFilter = new IntentFilter(MessageIntents.QUIT_PLAY_ACTION);
    // our broadcast receivers

    public MediaPlayerFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // note: register in onResume() and unregister in onPause()
    @Override
    public void onResume() {
        super.onResume();
        // register our receivers
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(m_receiver, prevFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(m_receiver, nextFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(m_receiver, toggleFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(m_receiver, quitPlayFilter);
        // if we have a valid service connection start the song
        if(m_serviceConnection != null) {
            StartSong();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // unregister from our broadcasts
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(m_receiver);
        // unbind from the service
        //Toast.makeText(getActivity(), "UnBound From Server", Toast.LENGTH_SHORT).show();
        //getActivity().unbindService(m_serviceConnection);
    }

    private void initBroadcastReceiver() {
        if(m_receiver == null) {
            m_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // check our action and delegate the correct response
                    switch(intent.getAction()) {
                        case MessageIntents.PREV_SONG_ACTION:
                            Toast.makeText(getActivity(), "Previous song intent", Toast.LENGTH_SHORT).show();
                            updateUIComponents();
                            break;
                        case MessageIntents.NEXT_SONG_ACTION:
                            Toast.makeText(getActivity(), "Next song intent", Toast.LENGTH_SHORT).show();
                            updateUIComponents();
                            break;
                        case MessageIntents.TOGGLE_PLAY_ACTION:
                            Toast.makeText(getActivity(), "Toggle Play intent", Toast.LENGTH_SHORT).show();
                            updateUIComponents();
                            break;
                        case MessageIntents.QUIT_PLAY_ACTION:
                            Toast.makeText(getActivity(), "Quit intent", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_player, container, false);
        if(!m_songList.isEmpty()) {
            // set the artist textview
            TextView artistText = (TextView) view.findViewById(R.id.player_Artist);
            if (artistText != null) {
                artistText.setText(m_artistName);
            }
            // set the song textview
            m_songText = (TextView)view.findViewById(R.id.player_Song);
            if(m_songText != null && m_startSongIndex != -1) {
                m_songText.setText(m_songList.get(m_startSongIndex).title);
            }
            // set the album image
            m_albumArt = (ImageView)view.findViewById(R.id.player_Album);
            setAlbumArtToFit();
            // set the buttons
            setupButtons(view);
            // create the media player service
            Intent mediaPlayerIntent = new Intent(getActivity(), MediaPlayerService.class);
            if(m_serviceConnection == null) {
                m_serviceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        m_boundToService = true;
                        MediaPlayerService.MediaBinder binder = (MediaPlayerService.MediaBinder)iBinder;
                        m_mediaPlayerService = binder.getMediaPlayerService();
                        // set our current song index so the media player knows where to start
                        m_mediaPlayerService.setCurrentSongIndex(m_startSongIndex);
                        // init media player
                        m_mediaPlayerService.initMediaPlayer(m_songList);
                        StartSong();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                        m_boundToService = false;
                    }
                };
            }
            getActivity().bindService(mediaPlayerIntent, m_serviceConnection, Context.BIND_AUTO_CREATE);
        }
        return view;
    }

    public void initMediaPlayer(ArrayList<Song> songList, String artistName, int songIndex, Uri albumArtUri, Context context) {
        m_songList = songList;
        m_context = context;
        m_artistName = artistName;
        m_startSongIndex = songIndex;
        m_albumArtURI = albumArtUri;
    }

    /**
     * private boolean StartSong(long id)
     *  Starts a song by passing in the id from the Song class
     * @return boolean if start was successful, false if not
     */
    private boolean StartSong()
    {
        if(m_mediaPlayerService != null) {
            updateUIComponents();
            // start the mediaplayerservice playback
            m_mediaPlayerService.stopSong();
            m_mediaPlayerService.startSong(m_isPaused);
            return true;
        }
        return false;
    }

    private void updateSeekBarState(long progress) {
        // duration of current song
        long songduration = m_mediaPlayerService.getCurrentSong().duration;
        // turn progess into percent
        float percent = ((float)progress * .01f);
        m_currentSeekLocation = (long)(percent * m_mediaPlayerService.getCurrentSong().duration);
        final long minute = (m_currentSeekLocation / (1000 * 60)) % 60;
        final long second = (m_currentSeekLocation / 1000) % 60;
        long durationSeconds = (songduration / 1000) % 60;
        long durationMinutes = (songduration / (1000 * 60)) % 60;
        m_durationText.setText(String.format(Locale.ENGLISH, "%02d:%02d / %02d:%02d", minute, second, durationMinutes, durationSeconds));
        m_seekBar.setProgress((int)progress);
    }

    private void setupButtons(View v)
    {
        Button previousButton = (Button)v.findViewById(R.id.player_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PreviousSong();
            }
        });
        Button nextButton = (Button)v.findViewById(R.id.player_next);
        nextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                NextSong();
            }
        });
        m_playButton = (Button)v.findViewById(R.id.player_play);
        m_playButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TogglePlayButton();
            }
        });

        m_seekBar = (SeekBar)v.findViewById(R.id.player_seekbar);
        m_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                m_mediaPlayerService.getMediaPlayer().seekTo((int)m_currentSeekLocation);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                if(m_mediaPlayerService != null) {
                    updateSeekBarState(progress);
                }
            }
        });
        // update duration text
        m_durationText = (TextView)v.findViewById(R.id.player_durationText);
    }

    // helper functions for skipping songs and pause/play
    private void PreviousSong()
    {
        // stop the current song
        if(m_boundToService) {
            m_mediaPlayerService.previousSong(m_isPaused);
        }
    }

    private void NextSong()
    {
        // stop the current song
        if(m_boundToService) {
            m_mediaPlayerService.nextSong(m_isPaused);
        }
    }

    /**
     * Updates essential UI components when states change
     */
    private void updateUIComponents() {
        // update the seekbar state back to 0
        updateSeekBarState(0);
        // set the play button state
        updatePlayButtonUI();
        m_songText.setText(m_mediaPlayerService.getCurrentSong().title);
    }

    private void TogglePlayButton()
    {
        if(m_mediaPlayerService.getMediaPlayer().isPlaying())
        {
            m_playButton.setText(getResources().getString(R.string.PlayString));
            m_mediaPlayerService.pauseMediaPlayback();
            m_isPaused = true;
        }
        else
        {
            m_playButton.setText(getResources().getString(R.string.PauseString));
            m_mediaPlayerService.continueMediaPlayback();
            m_isPaused = false;
        }
    }

    private void updatePlayButtonUI()
    {
        if(m_isPaused)
        {
            m_playButton.setText(getResources().getString(R.string.PlayString));
        }
        else
        {
            m_playButton.setText(getResources().getString(R.string.PauseString));
        }
    }

    private void setAlbumArtToFit()
    {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(
                    m_context.getContentResolver(), m_albumArtURI);
        } catch (FileNotFoundException e) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bitmap != null) {
            // Get current dimensions AND the desired bounding box
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int bounding = dpToPx(250);

            // Determine how much to scale: the dimension requiring less scaling is
            // closer to the its side. This way the image always stays inside your
            // bounding box AND either x/y axis touches it.
            float xScale = ((float) bounding) / width;
            float yScale = ((float) bounding) / height;
            float scale = (xScale <= yScale) ? xScale : yScale;

            // Create a matrix for the scaling and add the scaling data
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            // Create a new bitmap and convert it to a format understood by the ImageView
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            width = scaledBitmap.getWidth(); // re-use
            height = scaledBitmap.getHeight(); // re-use

            // Apply the scaled bitmap
            m_albumArt.setImageBitmap(scaledBitmap);

            // Now change ImageView's dimensions to match the scaled image
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) m_albumArt.getLayoutParams();
            params.width = width;
            params.height = height;
            m_albumArt.setLayoutParams(params);
        }
    }

    private int dpToPx(int dp)
    {
        float density = m_context.getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }
}
