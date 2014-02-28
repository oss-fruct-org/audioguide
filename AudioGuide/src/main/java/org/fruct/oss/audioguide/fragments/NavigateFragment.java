package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.track.AudioService;
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

	private AudioService audioService;

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

		audioServiceConnection = new AudioServiceConnection();
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

		getActivity().bindService(new Intent(getActivity(), AudioService.class),
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
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_navigate:
			if (audioService != null) {
				if (audioService.isTrackingStarted())
					audioService.stopTracking();
				else
					audioService.startTracking();

				updateMenuIcon();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon() {
		if (audioService.isTrackingStarted()) {
			navigateAction.setTitle("Unfollow");
		} else {
			navigateAction.setTitle("Follow");
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

	private class AudioServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			audioService = ((AudioService.AudioServiceBinder) iBinder).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			audioService = null;
		}
	}
}
