package org.fruct.oss.audioguide.preferences;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import junit.framework.AssertionFailedError;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.SingletonService;
import org.fruct.oss.audioguide.SynchronizerService;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final Logger log = LoggerFactory.getLogger(SettingsActivity.class);

	public static final String PREF_RANGE = "pref_range";
	public static final String PREF_WAKE = "pref_wake";
	public static final String PREF_LOAD_RADIUS = "pref_load_radius";
	public static final String PREF_CLEAN_POINTS = "pref_clean_points";

	public static final String PREF_WARN_NETWORK_DISABLED = "pref_warn_network_disabled";
	public static final String PREF_WARN_PROVIDERS_DISABLED = "pref_warn_providers_disabled";

	private SliderPreference rangePreference;
	private SliderPreference loadRadiusPreference;
	private SharedPreferences pref;
	private Preference cleanPointsPreference;

	private ServiceConnection singletonServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		rangePreference = (SliderPreference) findPreference(PREF_RANGE);
		loadRadiusPreference = ((SliderPreference) findPreference(PREF_LOAD_RADIUS));
		cleanPointsPreference = (Preference) findPreference(PREF_CLEAN_POINTS);

		cleanPointsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				log.debug("Clean points clicked");
				Toast.makeText(SettingsActivity.this, "Cleaning points...", Toast.LENGTH_LONG).show();
				SynchronizerService.startClean(SettingsActivity.this);
				return true;
			}
		});

		if (Build.VERSION.SDK_INT >= 11) {
			//getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		bindService(new Intent(this, SingletonService.class), singletonServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
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

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unbindService(singletonServiceConnection);
	}

	private void updateRangeSummary() {
		int value = pref.getInt(PREF_RANGE, 50);
		rangePreference.setSummary(getResources().getQuantityString(R.plurals.pref_seek_bar_summary,
				value, value));
	}

	private void updateLoadRadiusSummary() {
		int value = pref.getInt(PREF_LOAD_RADIUS, 500);
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
