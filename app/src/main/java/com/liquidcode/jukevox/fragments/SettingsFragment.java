package com.liquidcode.jukevox.fragments;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import com.liquidcode.jukevox.R;

/**
 * Created by mikev on 5/28/2017.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
        }
    }

}
