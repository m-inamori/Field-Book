package com.fieldbook.tracker.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.fieldbook.tracker.R;
import com.h6ah4i.android.preference.NumberPickerPreferenceCompat;
import com.h6ah4i.android.preference.NumberPickerPreferenceDialogFragmentCompat;

public class ThemePreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    PreferenceManager prefMgr;
    Context context;
    private Preference restoreTheme;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName("Settings");

        setPreferencesFromResource(R.xml.preferences_theme, rootKey);

        ((PreferencesActivity) this.getActivity()).getSupportActionBar().setTitle(getString(R.string.preferences_appearance_theme_title));

        restoreTheme = findPreference("RESTORE_DEFAULT_THEME");

        restoreTheme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {

                SharedPreferences.Editor ed = getContext().getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS).edit();
                ed.putInt("SAVED_DATA_COLOR", Color.parseColor("#d50000"));
                ed.apply();

                getParentFragmentManager()
                        .beginTransaction()
                        .detach(ThemePreferencesFragment.this)
                        .attach(ThemePreferencesFragment.this)
                        .commit();
                return true;
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Occurs before the on create function. We get the context this way.
        ThemePreferencesFragment.this.context = context;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}