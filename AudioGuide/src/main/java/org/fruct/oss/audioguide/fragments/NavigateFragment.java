package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.track.AudioService;
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
public class NavigateFragment extends ListFragment implements TrackManager.Listener {
	private final static Logger log = LoggerFactory.getLogger(NavigateFragment.class);

	private TrackManager trackManager;
	private MultiPanel multiPanel;

	private TrackAdapter trackAdapter;
	private ServiceConnection audioServiceConnection;

	private TrackingService trackingService;

	private MenuItem navigateAction;

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

		setHasOptionsMenu(true);

		audioServiceConnection = new TrackingServiceConnection();
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

		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				audioServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();

		getActivity().unbindService(audioServiceConnection);
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
		navigateAction = menu.findItem(R.id.action_navigate);

		updateMenuIcon();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_navigate:
			if (AudioService.isRunning(getActivity()))
				getActivity().stopService(new Intent(getActivity(), AudioService.class));
			else
				getActivity().startService(new Intent(getActivity(), AudioService.class));

			updateMenuIcon();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon() {
		if (AudioService.isRunning(getActivity())) {
			navigateAction.setTitle("Unfollow");
			navigateAction.setIcon(R.drawable.ic_action_volume_muted);
		} else {
			navigateAction.setTitle("Follow");
			navigateAction.setIcon(R.drawable.ic_action_volume_on);
		}
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

	private class TrackingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			trackingService = ((TrackingService.TrackingServiceBinder) iBinder).getService();
			trackingService.startTracking();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			trackingService = null;
		}
	}
}
