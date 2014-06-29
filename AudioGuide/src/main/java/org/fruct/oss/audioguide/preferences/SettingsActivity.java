package org.fruct.oss.audioguide.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.fruct.oss.audioguide.R;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String PREF_RANGE = "pref_range";
	public static final String PREF_WAKE = "pref_wake";
	public static final String PREF_LOAD_RADIUS = "pref_load_radius";


	private SliderPreference rangePreference;
	private SliderPreference loadRadiusPreference;
	private SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		rangePreference = (SliderPreference) findPreference(PREF_RANGE);
		loadRadiusPreference = ((SliderPreference) findPreference(PREF_LOAD_RADIUS));
	}

	@Override
	protected void onResume() {
		super.onResume();

		assert getPreferenceScreen() != null;
		pref = getPreferenceScreen().getSharedPreferences();

		updateRangeSummary();
		updateLoadRadiusSummary();

		pref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		pref.unregisterOnSharedPreferenceChangeListener(this);

		super.onPause();
	}

	private void updateRangeSummary() {
		int value = pref.getInt(PREF_RANGE, 50);
		rangePreference.setSummary(getResources().getQuantityString(R.plurals.pref_seek_bar_summary,
				value, value));
	}

	private void updateLoadRadiusSummary() {
		int value = pref.getInt(PREF_LOAD_RADIUS, 1000);
		loadRadiusPreference.setSummary(getResources().getQuantityString(R.plurals.pref_load_radius_summary,
				value, value));
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		if (s.equals(PREF_RANGE)) {
			updateRangeSummary();
		} else if (s.equals(PREF_LOAD_RADIUS)) {
			updateLoadRadiusSummary();
		}
	}
}
