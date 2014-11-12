package org.fruct.oss.audioguide;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.fruct.oss.audioguide.config.Config;
import org.fruct.oss.audioguide.dialogs.WarnDialog;
import org.fruct.oss.audioguide.fragments.AboutFragment;
import org.fruct.oss.audioguide.fragments.CommonFragment;
import org.fruct.oss.audioguide.fragments.GetsFragment;
import org.fruct.oss.audioguide.fragments.MapFragment;
import org.fruct.oss.audioguide.fragments.PanelFragment;
import org.fruct.oss.audioguide.fragments.TrackFragment;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.fruct.oss.audioguide.track.AudioPlayer;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class MainActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks, MultiPanel,
		TestFragment.OnFragmentInteractionListener {
	private final static Logger log = LoggerFactory.getLogger(MainActivity.class);

	private static final String TAG_PANEL_FRAGMENT = "panel-fragment";

	public static final String STATE_BACK_STACK_COUNT = "state-back-stack-count";
	public static final String STATE_NETWORK_WARN_SHOWN = "state-STATE_NETWORK_WARN_SHOWN";
	public static final String STATE_PROVIDERS_WARN_SHOWN = "state-STATE_PROVIDERS_WARN_SHOWN";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private FragmentManager fragmentManager;
	private BroadcastReceiver startAudioReceiver;
	private int backStackCount;

	private boolean networkToastShown;
	private boolean providersToastShown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log.trace("MainActivity onCreate");
		super.onCreate(savedInstanceState);

		Config.checkEditLocked(this);

		fragmentManager = getSupportFragmentManager();

		setContentView(R.layout.activity_main);

		if (fragmentManager.findFragmentByTag("common-fragment") == null)
			fragmentManager.beginTransaction().add(CommonFragment.newInstance(), "common-fragment").commit();

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				fragmentManager.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		if (savedInstanceState != null) {
			backStackCount = savedInstanceState.getInt(STATE_BACK_STACK_COUNT);
			networkToastShown = savedInstanceState.getBoolean(STATE_NETWORK_WARN_SHOWN);
			providersToastShown = savedInstanceState.getBoolean(STATE_PROVIDERS_WARN_SHOWN);
			updateUpButton();
		}

		setupBottomPanel();

		startService(new Intent(this, SingletonService.class));
	}

	private void setupBottomPanel() {
		LocalBroadcastManager.getInstance(this).registerReceiver(startAudioReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final int duration = intent.getIntExtra("duration", 0);
				final Point point = intent.getParcelableExtra("point");

				PanelFragment panelFragment = (PanelFragment) getSupportFragmentManager().findFragmentByTag("bottom-panel-fragment");

				if (panelFragment == null || panelFragment.isRemoving()) {
					panelFragment = PanelFragment.newInstance(null, duration, point);
					fragmentManager.beginTransaction()
							.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
							.replace(R.id.panel_container, panelFragment, "bottom-panel-fragment").commit();
				} else {
					panelFragment.setCurrentPoint(point);
					panelFragment.startPlaying(duration);
				}
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_START_PLAY));

	}

	private void unsetupBottomPanel() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(startAudioReceiver);
	}

	@Override
	protected void onStart() {
		super.onStart();
		log.trace("MainActivity onStart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		log.trace("MainActivity onResume");

		checkNetworkAvailable();
		checkProvidersEnabled();
	}

	@Override
	protected void onPause() {
		super.onPause();
		log.trace("MainActivity onPause");
	}

	@Override
	protected void onStop() {
		super.onStop();
		log.trace("MainActivity onStop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		log.trace("MainActivity onDestroy");
		unsetupBottomPanel();
	}

	@Override
	public void onNavigationDrawerItemSelected(int position, Bundle fragmentParameters) {
		Fragment fragment = null;
		switch (position) {
		case 0:
			fragment = TrackFragment.newInstance();
			break;
		case 1:
			fragment = MapFragment.newInstance();
			break;
		case 2:
			fragment = GetsFragment.newInstance();
			break;
		case 3:
			fragment = AboutFragment.newInstance();
			break;
		}

		if (fragmentParameters != null && fragment != null) {
			fragment.setArguments(fragmentParameters);
		}

		fragmentManager.popBackStack(TAG_PANEL_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.addToBackStack(TAG_PANEL_FRAGMENT);
		fragmentTransaction.replace(R.id.panel1, fragment, "content_fragment");
		fragmentTransaction.commit();
		backStackCount = 1;

		updateUpButton();
	}

	private void updateUpButton() {
		if (mNavigationDrawerFragment == null)
			return;

		if (backStackCount > 1) {
			mNavigationDrawerFragment.setUpEnabled(false);
		} else {
			mNavigationDrawerFragment.setUpEnabled(true);
		}
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		case 4:
			mTitle = getString(R.string.title_section4);
			break;
		}
	}

	@Override
	public void onBackPressed() {
		int count = fragmentManager.getBackStackEntryCount();
		String name;
		do {
			FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(--count);
			fragmentManager.popBackStack();
			name = entry.getName();
		} while (name == null);
		backStackCount--;

		if (name.equals(TAG_PANEL_FRAGMENT)) {
			finish();
		} else {
			updateUpButton();
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();

		/*if (fragmentStack.isEmpty() && currentFragment instanceof TrackFragment)
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		else
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);*/


		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return super.onSupportNavigateUp();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.

			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void pushFragment(Fragment fragment) {
		fragmentManager.beginTransaction()
				.addToBackStack("fragment-transaction-" + fragment.hashCode())
				.replace(R.id.panel1, fragment)
				.commit();

		backStackCount++;
		updateUpButton();
	}

	@Override
	public void replaceFragment(Fragment fragment, Fragment firstFragment) {
		pushFragment(fragment);
	}

	@Override
	public void popFragment() {
		onBackPressed();

		updateUpButton();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_BACK_STACK_COUNT, backStackCount);
		outState.putBoolean(STATE_NETWORK_WARN_SHOWN, networkToastShown);
		outState.putBoolean(STATE_PROVIDERS_WARN_SHOWN, providersToastShown);
	}

	@Override
	public void onFragmentInteraction(Uri uri) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.debug("MainActivity onActivityResult {}, {}", requestCode, resultCode);
	}

	private void checkNetworkAvailable() {
		boolean networkActive = Utils.checkNetworkAvailability(this);

		if (!networkToastShown && !networkActive) {
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);

			Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
					PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

			if (!pref.getBoolean(SettingsActivity.PREF_WARN_NETWORK_DISABLED, false)) {
				WarnDialog dialog = WarnDialog.newInstance(R.string.warn_no_network,
						R.string.str_configure,
						R.string.str_ignore,
						SettingsActivity.PREF_WARN_NETWORK_DISABLED, pendingIntent);
				dialog.show(getSupportFragmentManager(), "network-dialog");
			} else {
				Toast toast = Toast.makeText(this,
						R.string.warn_no_network, Toast.LENGTH_SHORT);
				toast.show();
			}

			networkToastShown = true;
		}
	}

	private void checkProvidersEnabled() {
		LocationManager locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Check if all providers disabled and show warning
		if (!providersToastShown
				&& !locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
				&& !locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
					PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);

			if (!pref.getBoolean(SettingsActivity.PREF_WARN_PROVIDERS_DISABLED, false)) {
				WarnDialog dialog = WarnDialog.newInstance(R.string.warn_no_providers,
						R.string.str_configure,
						R.string.str_ignore,
						SettingsActivity.PREF_WARN_PROVIDERS_DISABLED,
						pendingIntent);

				dialog.show(getSupportFragmentManager(), "providers-dialog");
			} else {
				Toast toast = Toast.makeText(this,
						R.string.warn_no_providers, Toast.LENGTH_SHORT);
				toast.show();
			}
			providersToastShown = true;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section
		 * number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			TextView textView = (TextView) rootView.findViewById(R.id.section_label);
			textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(
					getArguments().getInt(ARG_SECTION_NUMBER));
		}
	}
}
