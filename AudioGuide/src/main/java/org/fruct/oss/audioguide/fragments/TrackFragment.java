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
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackModelAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackFragment extends ListFragment {
	private final static Logger log = LoggerFactory.getLogger(TrackFragment.class);

	private MultiPanel multiPanel;
	private TrackManager trackManager;

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

	public void trackClicked(Track track) {
		multiPanel.replaceFragment(TrackDetailFragment.newInstance(track), this);
	}
}
