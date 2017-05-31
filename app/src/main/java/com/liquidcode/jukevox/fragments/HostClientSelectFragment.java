package com.liquidcode.jukevox.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;

/**
 * Created by mikev on 5/11/2017.
 */

public class HostClientSelectFragment extends Fragment {
    private static final String TAG = "HostClientFragment";
    // ViewGroup to pass around
    private ViewGroup m_rootView = null;
    // instance for the server room
    private ServerFragment m_serverFragment = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        m_rootView = (ViewGroup) inflater.inflate(
                R.layout.host_client_select_layout, container, false);

        if(m_rootView != null) {
            initializeButtons();
        }
        return m_rootView;
    }

    private void initializeButtons() {
        // get the server start button
        Button startServerButton = (Button)m_rootView.findViewById(R.id.startServerButton);
        // get the join server button
        Button joinServerButton = (Button)m_rootView.findViewById(R.id.joinServerButton);
        if(startServerButton != null && joinServerButton != null) {

            // set onClicklistener for startServer
            startServerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //CreateRoomNameAlert();
                    ((JukevoxMain)getActivity()).createServerRoomFragment();
                }
            });

            // set onClickListener for joinServer
            joinServerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CreateClientFragment();
                }
            });
        }
        else {
            Log.e(TAG, "Could not setup selection buttons!");
        }
    }

    /**
     * Prompts the user to create a name for the room and starts the intent
     */
//    private void CreateRoomNameAlert() {
//        final EditText input = new EditText(getActivity());
//        input.setSingleLine(true);
//        input.setText(m_roomName);
//        // ask the user for room name
//        new AlertDialog.Builder(getActivity())
//                .setTitle("JukeVox")
//                .setMessage("Enter room name:")
//                .setView(input)
//                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        String result = input.getText().toString().trim();
//                        if (!result.equals("")) {
//                            m_roomName = input.getText().toString();
//                            ((JukevoxMain)getActivity()).createServerRoomFragment(m_roomName);
//                        }
//                    }
//                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int whichButton) {
//
//            }
//        }).show();
//    }

    /**
     * Creates the client room fragment
     */
    private void CreateClientFragment() {
        ((JukevoxMain)getActivity()).createJoinRoomFragment();
    }
}
