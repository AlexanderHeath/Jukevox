package com.liquidcode.jukevox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.liquidcode.jukevox.fragments.ClientFragment;
import com.liquidcode.jukevox.fragments.ClientJoinedFragment;
import com.liquidcode.jukevox.fragments.ServerFragment;
import com.liquidcode.jukevox.fragments.HostClientSelectFragment;
import com.liquidcode.jukevox.fragments.LibraryFragment;
import com.liquidcode.jukevox.fragments.SettingsFragment;
import com.liquidcode.jukevox.fragments.SongFragment;
import com.liquidcode.jukevox.musicobjects.Song;
import com.liquidcode.jukevox.networking.Client.BluetoothClient;

import java.util.ArrayList;

public class JukevoxMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SongFragment.OnSongSelectedListener {

    private static final String TAG = "JukevoxMain";
    private static final int NUM_PAGES = 2;
    // view pager
    private ViewPager m_pager = null;
    // pager adapter
    private PagerAdapter m_pagerAdapter = null;
    // variables for user options
    String m_userName;
    String m_roomName;
    boolean m_connectionType;
    // instances of our potential fragments
    private LibraryFragment m_libraryFragment = null;
    private HostClientSelectFragment m_hostClientSelect = null;
    private ServerFragment m_serverFragment = null;
    private final String SERVERFRAG_TAG = "serverfrag";
    private ClientFragment m_clientFragment = null;
    private final String CLIENTFRAG_TAG = "clientfrag";
    private SongFragment m_songFragment = null;
    private final String SONGFRAG_TAG = "songfrag";
    private ClientJoinedFragment m_clientJoinedFragment = null;
    private final String JOINEDFRAG_TAG = "clientjoined";
    private SettingsFragment m_settings = null;
    // request code for our sign in
    private final int RC_SIGN_IN = 0;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jukevox_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // set up the view pager
        m_pager = (ViewPager) findViewById(R.id.main_pager);
        m_pagerAdapter = new JukevoxPagerAdapter(getSupportFragmentManager());
        m_pager.setPageTransformer(false, new ZoomOutPageTransformer());
        m_pager.setAdapter(m_pagerAdapter);

        // setup the drawer layout
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        // we are at the library (home) screen
        if (m_pager.getCurrentItem() == 0) {
            // check if the settings menu is up and kill it if it is
            if(m_settings != null) {
                m_settings = null;
            }
            // if the SongFragment is open backstack is popped (home screen)
            // if the SongFragment is NOT there closes app.
            super.onBackPressed();
        } else {
                if(m_settings != null) {
                    // the settings menu is up lets close it and kill the object
                    super.onBackPressed();
                    m_settings = null;
                }
                else {
                    // Otherwise, select the previous step.
                    m_pager.setCurrentItem(m_pager.getCurrentItem() - 1);
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.jukevox_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_disconnect:
                // close the client or server fragment if we can
                closeRoomFragments();
                return true;
            case R.id.action_signin:
                signIn();
                return true;
            case R.id.action_settings:
                createSettingsFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    /**
     * Prompts the user to sign in
     */
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(client);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            updateAccountUI(acct);
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            //updateUI(true);
        }
    }

    private void updateAccountUI(GoogleSignInAccount acct) {
        if(acct != null) {
            // get the name and email textviews
            TextView accountName = (TextView)findViewById(R.id.account_name);
            TextView accountEmail = (TextView)findViewById(R.id.account_email);
            ImageView accountImage = (ImageView)findViewById(R.id.account_image);
            if(accountName != null && accountEmail != null && accountImage != null) {
                updateUserName(acct.getDisplayName());
                accountEmail.setText(acct.getEmail());
                accountImage.setImageURI(acct.getPhotoUrl());
            }
        }
    }

    private void updateUserName(String username) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navView = (NavigationView) drawer.findViewById(R.id.nav_view);
        TextView accountName = (TextView)navView.getHeaderView(0).findViewById(R.id.account_name);
        if(accountName != null) {
            accountName.setText(username);
        }
    }

    private void createSettingsFragment() {
        // don't allow to instances of the settings menu to appear.
        if(m_settings == null) {
            m_settings = new SettingsFragment();
            // add settings fragment
            if (m_pager.getCurrentItem() == 0) {
                getFragmentManager().beginTransaction()
                        //.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit,
                        //        R.animator.fragment_enter, R.animator.fragment_exit)
                        .replace(R.id.libraryContainer, m_settings, "settingsfrag_tag")
                        .addToBackStack(null)
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        //.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit,
                        //        R.animator.fragment_enter, R.animator.fragment_exit)
                        .replace(R.id.hostingContainer, m_settings, "settingsfrag_tag")
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_library) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_start_room) {
            Toast.makeText(this, "Starting Room...", Toast.LENGTH_SHORT).show();
            //CreateRoomNameAlert(); // ask the user to create a room name and start the fragment if we do
        } else if (id == R.id.nav_join_room) {
            Toast.makeText(this, "Joining Room...", Toast.LENGTH_SHORT).show();
            //CreateClientIntent();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("JukevoxMain Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserOptions();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    /**
     * Polls the SharedPreferences and loads the user options
     */
    public void loadUserOptions() {
        // load the users options (username, room name, connection type)
        // from the shared preferences if they exist
        SharedPreferences useroptions = PreferenceManager.getDefaultSharedPreferences(this);
        m_userName = useroptions.getString("edit_displayname_preference", "Default");
        m_roomName = useroptions.getString("edit_servername_preference", "DefaultServer");
        m_connectionType = useroptions.getBoolean("switch_connectiontype_preference", false);
        // update our username
        updateUserName(m_userName);
    }

    @Override
    public void onSongSelected(String artist, Song songData) {
        if(m_clientJoinedFragment != null && m_clientJoinedFragment.isConnectedToRoom()) {
            // send the song info to the ClientJoinedFragment
            m_clientJoinedFragment.beginSongStreaming(artist, songData);
        }
    }

    /**
     * PagerAdapter for our library and room fragments
     */
    private class JukevoxPagerAdapter extends FragmentStatePagerAdapter {

        private JukevoxPagerAdapter(FragmentManager fm) {
            super(fm);
            m_libraryFragment = new LibraryFragment();
            m_hostClientSelect = new HostClientSelectFragment();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // library fragment
                    return m_libraryFragment;
                case 1: // HostClientFragment
                    return m_hostClientSelect;
                default:
                    return m_libraryFragment;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: // library fragment
                    return "Library";
                case 1: // HostClientFragment
                    return "Start / Join";
                default:
                    return "Home";
            }
        }
    }

    /**
     * ZoomOut PagerAdapter effect when swiping between children
     */
    private class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    /**
     * Creates the server room fragment
     */
    public void createServerRoomFragment() {
        // if fragment is null create it
        if(m_serverFragment == null) {
            m_serverFragment = new ServerFragment();
        }
        Bundle args = new Bundle();
        args.putString("roomName", m_roomName);
        m_serverFragment.setArguments(args);
        m_serverFragment.initializeRoom();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.hostingContainer, m_serverFragment, SERVERFRAG_TAG)
                //.addToBackStack(null)
                .commit();
    }

    /**
     * Creates the join room fragment
     * Lists all available rooms to join
     */
    public void createJoinRoomFragment() {
        if(m_clientFragment == null) {
            m_clientFragment = new ClientFragment();
        }
        // set the clients username
        m_clientFragment.updateClientUsername(m_userName);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.hostingContainer, m_clientFragment, CLIENTFRAG_TAG)
                //.addToBackStack(null)
                .commit();
    }

    /**
     * Creates the ClientJoinedFragment that will hold the ConnectedThread
     * This fragment will be in charge of sending updates and other info based on User interactions
     * @param btc - the bluetooth client that we made a successful connection with
     */
    public void createClientJoinedRoomFragment(BluetoothClient btc) {
        if(m_clientJoinedFragment == null) {
            m_clientJoinedFragment = new ClientJoinedFragment();
        }
        // set the bluetoothclient
        m_clientJoinedFragment.initializeClient(m_userName, btc);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.hostingContainer, m_clientJoinedFragment, JOINEDFRAG_TAG)
                .commit();
    }

    /**
     * Creates the songlist fragment and replaces our library with it
     */
    public void createSongListFragment(ArrayList<Song> songlist, String album, String artist, Uri albumUri) {
        if(m_songFragment == null) {
            m_songFragment = new SongFragment();
        }
        // set the album info before we go creating the views
        m_songFragment.initSongInfo(songlist, album, artist, albumUri, getApplicationContext());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.libraryContainer, m_songFragment, SONGFRAG_TAG)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Closes all the room fragments and cleans up threads
     */
    public void closeRoomFragments() {
        // try to find the server fragment and remove it
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ServerFragment sf = (ServerFragment)getSupportFragmentManager().findFragmentByTag(SERVERFRAG_TAG);
        if(sf != null) {
            ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                    .remove(sf)
                    .commit();
        }

        // try to find the client fragment and remove it
        ft = getSupportFragmentManager().beginTransaction();
        ClientJoinedFragment cjf = (ClientJoinedFragment) getSupportFragmentManager().findFragmentByTag(JOINEDFRAG_TAG);
        if(cjf != null) {
            ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                    .remove(cjf)
                    .commit();
        }

        // try to find the client fragment and remove it
        ft = getSupportFragmentManager().beginTransaction();
        ClientFragment cf = (ClientFragment)getSupportFragmentManager().findFragmentByTag(CLIENTFRAG_TAG);
        if(cf != null) {
            ft.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                    .remove(cf)
                    .commit();
        }

        if(sf == null && cf == null && cjf == null) {
            Toast.makeText(getApplicationContext(), "Not Hosting or Connected to a room!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Removes the BluetoothServerFragent.
     * This is called when there is an error in the initialization of the fragment
     */
    public void removeCurrentFragment() {
        closeRoomFragments();
    }
}
