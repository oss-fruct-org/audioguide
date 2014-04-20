package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackModelAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackFragment extends ListFragment {
	private final static Logger log = LoggerFactory.getLogger(TrackFragment.class);

	public static final String STATE_SPINNER_STATE = "spinner-state";

	private MultiPanel multiPanel;
	private TrackManager trackManager;
	private int selectedSpinnerItem = 0;

	private TrackModelAdapter trackAdapter;

	public static TrackFragment newInstance() {
		return new TrackFragment();
	}

	public TrackFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();

		trackAdapter = new TrackModelAdapter(getActivity(), R.layout.list_track_item, trackManager.getTracksModel());

		setListAdapter(trackAdapter);

		setHasOptionsMenu(true);

		if (savedInstanceState != null) {
			selectedSpinnerItem = savedInstanceState.getInt(STATE_SPINNER_STATE);
		}
	}


	@Override
	public void onResume() {
		super.onResume();

		setupSpinner();
	}

	@Override
	public void onPause() {
		super.onPause();

		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	private void setupSpinner() {
		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();


		ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(),
				R.array.track_spinner_array,
				android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int i, long l) {
				switch (i) {
				case 0: // All tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return true;
						}
					});
					break;

				case 1: // Private tracks
					/*trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return true;
						}
					});*/
					break;

				case 2: // Public tracks
					/*trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return true;
						}
					});*/
					break;

				case 3: // Local tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return track.isLocal();
						}
					});
					break;
				case 4: // Active tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return track.isActive();
						}
					});
					break;
				default:
					return false;
				}

				return true;
			}
		});
		actionBar.setSelectedNavigationItem(selectedSpinnerItem);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onDestroy() {
		trackAdapter.close();
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			multiPanel = (MultiPanel) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement MultiFragment");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		multiPanel = null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Track track = trackAdapter.getItem(position);
		trackClicked(track);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			trackManager.refresh();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();

		outState.putInt(STATE_SPINNER_STATE, actionBar.getSelectedNavigationIndex());
	}

	public void trackClicked(Track track) {
		multiPanel.replaceFragment(TrackDetailFragment.newInstance(track), this);
	}
}
