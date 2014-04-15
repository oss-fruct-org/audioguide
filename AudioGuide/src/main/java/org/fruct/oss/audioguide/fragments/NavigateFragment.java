package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.adapters.TrackModelAdapter;
import org.fruct.oss.audioguide.track.TrackingService;
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
public class NavigateFragment extends ListFragment {
	private final static Logger log = LoggerFactory.getLogger(NavigateFragment.class);

	private TrackManager trackManager;
	private MultiPanel multiPanel;

	private TrackModelAdapter trackAdapter;
	private ServiceConnection serviceConnection;

	private TrackingService trackingService;

	public static NavigateFragment newInstance() {
		return new NavigateFragment();
	}

	public NavigateFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();

		trackAdapter = new TrackModelAdapter(getActivity(),
				R.layout.list_track_item,
				trackManager.getActiveTracksModel());
		setListAdapter(trackAdapter);

		serviceConnection = new TrackingServiceConnection();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackAdapter.close();
		trackManager = null;
	}

	@Override
	public void onStart() {
		super.onStart();

		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();

		getActivity().unbindService(serviceConnection);
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		Track track = trackAdapter.getItem(position);
		multiPanel.replaceFragment(PointFragment.newInstance(track), this);
	}

	private class TrackingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			trackingService = ((TrackingService.TrackingServiceBinder) iBinder).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			trackingService = null;
		}
	}
}
