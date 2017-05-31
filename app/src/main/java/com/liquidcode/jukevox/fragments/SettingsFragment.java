package com.liquidcode.jukevox.fragments;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.Switch;

import com.liquidcode.jukevox.JukevoxMain;
import com.liquidcode.jukevox.R;

import java.util.Map;

/**
 * SettingsFragment.java
 * Saves/loads the settings screen and values
 * Created by mikev on 5/28/2017.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sharedPreferences;

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

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // we want to watch the preference values' changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Map<String, ?> preferencesMap = sharedPreferences.getAll();
        // iterate through the preference entries and update their summary if they are an instance of EditTextPreference
        for (Map.Entry<String, ?> preferenceEntry : preferencesMap.entrySet()) {
                updateSummary(findPreference(preferenceEntry.getKey()));
        }
    }

    @Override
    public void onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        // tell the main activity that we might have updated setting options
        ((JukevoxMain)getActivity()).loadUserOptions();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // get the preference that has been changed
        updateSummary(findPreference(key));
    }

    private void updateSummary(Preference preference) {
        if(preference != null) {
            if (preference instanceof EditTextPreference) {
                // set the EditTextPreference's summary value to its current text
                ((EditTextPreference) preference).setSummary(((EditTextPreference) preference).getText());
            } else if (preference instanceof SwitchPreference) {
                ((SwitchPreference) preference).setChecked(((SwitchPreference) preference).isChecked());
            }
        }
    }

}
