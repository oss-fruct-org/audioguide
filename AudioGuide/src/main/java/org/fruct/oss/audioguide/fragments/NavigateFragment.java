package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link org.fruct.oss.audioguide.MultiPanel}
 * interface.
 */
public class NavigateFragment extends ListFragment implements TrackManager.Listener, LocationReceiver.Listener {
	private final static Logger log = LoggerFactory.getLogger(NavigateFragment.class);

	private TrackManager trackManager;
	private MultiPanel multiPanel;

	private TrackAdapter trackAdapter;

	private LocationReceiver locationReceiver;

    public static NavigateFragment newInstance() {
		return new NavigateFragment();
    }

    public NavigateFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();
		trackManager.addListener(this);
		trackManager.loadRemoteTracks();

		// Setup location receiver
		locationReceiver = new LocationReceiver(getActivity());
		locationReceiver.setListener(this);

		setHasOptionsMenu(true);
    }

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackManager.removeListener(this);
		trackManager = null;
	}

	@Override
	public void onStart() {
		super.onStart();

		locationReceiver.start();
	}

	@Override
	public void onStop() {
		super.onStop();

		locationReceiver.stop();
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            multiPanel = (MultiPanel) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement MultiPanel");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
		multiPanel = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.navigate, menu);
	}

	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
		Track track = trackAdapter.getItem(position);
		multiPanel.replaceFragment(PointFragment.newInstance(track), this);
    }

	private void updateTracksAdapter() {
		trackAdapter = new TrackAdapter(getActivity(), R.layout.list_track_item, trackManager.getActiveTracks());
		setListAdapter(trackAdapter);
	}

	@Override
	public void tracksUpdated() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateTracksAdapter();
			}
		});
	}

	@Override
	public void trackUpdated(Track track) {
	}

	@Override
	public void pointsUpdated(Track track) {
	}

	@Override
	public void newLocation(Location location) {
		log.debug("New location {}", location);
	}
}
