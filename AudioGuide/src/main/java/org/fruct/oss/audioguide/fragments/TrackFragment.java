package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.adapters.TrackModelAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackFragment extends ListFragment implements TrackManager.Listener {
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
		trackManager.addListener(this);

		trackAdapter = new TrackModelAdapter(getActivity(), R.layout.list_track_item, trackManager.getTracksModel());
		setListAdapter(trackAdapter);
	}

	@Override
	public void onDestroy() {
		trackManager.removeListener(this);
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
	public void tracksUpdated() {
		//final List<Track> tracks = trackManager.getTracks();
		//log.debug("Tracks updated. Size = {}", tracks.size());

		/*getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				trackAdapter.clear();
				for (Track track : tracks)
					trackAdapter.add(track);
			}
		});*/
	}

	@Override
	public void pointsUpdated(Track track) {

	}

	public void trackClicked(Track track) {
		multiPanel.replaceFragment(TrackDetailFragment.newInstance(track), this);
	}
}
