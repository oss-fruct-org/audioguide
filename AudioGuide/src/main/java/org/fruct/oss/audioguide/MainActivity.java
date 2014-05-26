package org.fruct.oss.audioguide;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.TextView;

import org.fruct.oss.audioguide.fragments.CommonFragment;
import org.fruct.oss.audioguide.fragments.GetsFragment;
import org.fruct.oss.audioguide.fragments.MapFragment;
import org.fruct.oss.audioguide.fragments.NavigateFragment;
import org.fruct.oss.audioguide.fragments.TrackFragment;
import org.fruct.oss.audioguide.fragments.edit.EditTrackFragment;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
public class MainActivity extends ActionBarActivity
		implements NavigationDrawerFragment.NavigationDrawerCallbacks, MultiPanel,
		TestFragment.OnFragmentInteractionListener {
	private final static Logger log = LoggerFactory.getLogger(MainActivity.class);

	private static final String STATE_STACK_SIZE = "stack-size";
	private static final String STATE_STACK = "stack-fragment";
	private static final String STATE_CURRENT_FRAGMENT = "current-fragment";

	private static final String TAG_PANEL_FRAGMENT = "panel-fragment";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private ArrayList<FragmentStorage> fragmentStack = new ArrayList<FragmentStorage>();
	private Fragment currentFragment;

	private FragmentManager fragmentManager;
	private boolean suppressDrawerItemSelect = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log.trace("MainActivity onCreate");
		super.onCreate(savedInstanceState);

		fragmentManager = getSupportFragmentManager();

		if (fragmentManager.findFragmentByTag("common-fragment") == null)
			fragmentManager.beginTransaction().add(CommonFragment.newInstance(), "common-fragment").commit();

		if (fragmentManager.getFragments() != null) {
			for (Fragment f : fragmentManager.getFragments()) {
				log.debug("FRAGMENT: {}", f.getClass().getName());
			}
		}

		if (savedInstanceState != null) {
			suppressDrawerItemSelect = true;

			fragmentStack = savedInstanceState.getParcelableArrayList(STATE_STACK);
			currentFragment = fragmentManager.getFragment(savedInstanceState, STATE_CURRENT_FRAGMENT);

			for (FragmentStorage storage : fragmentStack)
				storage.setFragmentManager(fragmentManager);
		}

		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				fragmentManager.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		if (savedInstanceState != null) {
			updateUpButton();
		}
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

		//FragmentTransaction trans = fragmentManager.beginTransaction();
		//removeFragments(trans);
		//trans.commit();
		//fragmentManager.executePendingTransactions();
	}

	@Override
	public void onNavigationDrawerItemSelected(int position, Bundle fragmentParameters) {
		/*FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction trans = fragmentManager.beginTransaction();
		try {
			if (fragmentManager.getFragments() != null) {
				for (Fragment fragment : fragmentManager.getFragments()) {
					if (fragment != null && fragment.getTag() != null && fragment.getTag().equals(TAG_PANEL_FRAGMENT))
						trans.remove(fragment);
				}
			}
		} finally {
			trans.commit();
		}*/

		if (suppressDrawerItemSelect) {
			suppressDrawerItemSelect = false;
			return;
		}


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
		}

		if (fragmentParameters != null && fragment != null) {
			fragment.setArguments(fragmentParameters);
		}

		fragmentStack.clear();
		currentFragment = fragment;

		fragmentManager.beginTransaction()
				.replace(R.id.panel1, fragment, TAG_PANEL_FRAGMENT)
				.commit();

		updateUpButton();
	}

	private void updateUpButton() {
		if (mNavigationDrawerFragment == null)
			return;

		if (fragmentStack.size() <= 0) {
			mNavigationDrawerFragment.setUpEnabled(true);
		} else {
			mNavigationDrawerFragment.setUpEnabled(false);
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
		}
	}

	@Override
	public void onBackPressed() {
		if (fragmentStack.size() > 0)
			popFragment();
		else
			super.onBackPressed();
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();

		if (fragmentStack.isEmpty())
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		else
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);


		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onSupportNavigateUp() {
		if (fragmentStack.size() > 0)
			popFragment();

		return super.onSupportNavigateUp();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
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
		FragmentTransaction trans = fragmentManager.beginTransaction();

		FragmentStorage oldStorage = new FragmentStorage(currentFragment).setFragmentManager(fragmentManager);
		fragmentStack.add(oldStorage);
		oldStorage.storeFragment();

		currentFragment = fragment;
		trans.replace(R.id.panel1, fragment, TAG_PANEL_FRAGMENT);
		trans.commit();

		updateUpButton();
	}

	@Override
	public void replaceFragment(Fragment fragment, Fragment firstFragment) {
		pushFragment(fragment);

		/*int size = fragmentStack.size();

		FragmentStorage lastFragmentStorage = fragmentStack.get(size - 1);
		if (!lastFragmentStorage.isStored() && lastFragmentStorage.getFragment() == firstFragment) {
			pushFragment(fragment);
			return;
		}

		int indexOfLastKeepedFragment = size - 1;

		for (int i = size - 1; i >= 0; i--) {
			FragmentStorage storage = fragmentStack.get(i);
			if (!storage.isStored()) {
				if (storage.getFragment() == firstFragment) {
					indexOfLastKeepedFragment = i;
					break;
				}
			} else {
				throw new UnsupportedOperationException(
						"Shifting until off-screen fragment not supported yet");
			}
		}

		FragmentTransaction trans = fragmentManager.beginTransaction();
		removeFragments(trans);

		for (int i = size - 1; i > indexOfLastKeepedFragment; i--) {
			fragmentStack.remove(i);
		}

		fragmentStack.add(new FragmentStorage(fragment).setFragmentManager(fragmentManager));
		trans.add(R.id.panel1, fragment, TAG_PANEL_FRAGMENT);
		trans.commit();
		updateUpButton();*/
	}

	@Override
	public void popFragment() {
		FragmentTransaction trans = fragmentManager.beginTransaction();
		trans.remove(currentFragment);

		Fragment newFragment = fragmentStack.get(fragmentStack.size() - 1).getFragment();
		fragmentStack.remove(fragmentStack.size() - 1);

		currentFragment = newFragment;
		trans.replace(R.id.panel1, newFragment, TAG_PANEL_FRAGMENT);
		trans.commit();

		updateUpButton();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_STACK_SIZE, fragmentStack.size());
		outState.putParcelableArrayList(STATE_STACK, fragmentStack);
		fragmentManager.putFragment(outState, STATE_CURRENT_FRAGMENT, currentFragment);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onFragmentInteraction(Uri uri) {

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.debug("MainActivity onActivityResult {}, {}", requestCode, resultCode);
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

	private static class FragmentStorage implements Parcelable {
		private Fragment fragment;
		private Fragment.SavedState state;
		private Class<? extends Fragment> aClass;
		private FragmentManager fragmentManager;

		public FragmentStorage(Fragment fragment) {
			this.fragment = fragment;
		}

		public FragmentStorage setFragmentManager(FragmentManager fragmentManager) {
			this.fragmentManager = fragmentManager;
			return this;
		}

		public void storeFragment() {
			if (fragment == null)
				return;

			state = fragmentManager.saveFragmentInstanceState(fragment);
			aClass = fragment.getClass();
			fragment = null;
		}

		public boolean isStored() {
			return fragment == null;
		}

		public Fragment getFragment() {
			if (fragment != null)
				return fragment;

			try {
				fragment = aClass.newInstance();
				fragment.setInitialSavedState(state);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			return fragment;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int i) {
			parcel.writeParcelable(state, i);
			parcel.writeSerializable(aClass);
		}

		@SuppressWarnings("unchecked")
		public static final Creator<FragmentStorage> CREATOR = new Creator<FragmentStorage>() {
			@Override
			public FragmentStorage createFromParcel(Parcel parcel) {
				FragmentStorage  fs = new FragmentStorage(null);
				fs.state = parcel.readParcelable(null);
				fs.aClass = (Class<? extends Fragment>) parcel.readSerializable();
				return fs;
			}

			@Override
			public FragmentStorage[] newArray(int i) {
				return new FragmentStorage[i];
			}
		};
	}
}
