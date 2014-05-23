package org.fruct.oss.audioguide.fragments;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFragment extends Fragment {
	private final static Logger log = LoggerFactory.getLogger(CommonFragment.class);

	public static final String BC_ERROR_MESSAGE = "org.fruct.oss.audioguide.CommonFragment.BC_ERROR_MESSAGE";
	public static final String BC_ARG_MESSAGE = "message";

	private MenuItem navigateAction;
	private boolean isTrackingActive;

	private BroadcastReceiver errorReceiver;
	private ServiceConnection trackingServiceConnection;
	private TrackingService trackingService;

	public static CommonFragment newInstance() {
		return new CommonFragment();
	}
	public CommonFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log.debug("onCreate");

		setHasOptionsMenu(true);
		setRetainInstance(true);

		getActivity().startService(new Intent(TrackingService.ACTION_FOREGROUND, null,
				getActivity(), TrackingService.class));

		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				trackingServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
				log.debug("CommonFragment onServiceConnected");
				trackingService = ((TrackingService.TrackingServiceBinder) iBinder).getService();
				isTrackingActive = trackingService.isTrackingActive();
				updateMenuIcon(isTrackingActive);
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
				trackingService = null;
			}
		}, Context.BIND_AUTO_CREATE);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(errorReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String message = intent.getStringExtra(BC_ARG_MESSAGE);
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			}
		}, new IntentFilter(CommonFragment.BC_ERROR_MESSAGE));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (trackingService != null) {
			getActivity().unbindService(trackingServiceConnection);
		}

		getActivity().startService(new Intent(TrackingService.ACTION_BACKGROUND,
				null, getActivity(), TrackingService.class));

		trackingServiceConnection = null;
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(errorReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.navigate, menu);
		navigateAction = menu.findItem(R.id.action_navigate);
		updateMenuIcon(isTrackingActive);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_navigate:
			if (isTrackingActive) {
				getActivity().startService(new Intent(TrackingService.ACTION_STOP_TRACKING,
						null, getActivity(), TrackingService.class));
			} else {
				getActivity().startService(new Intent(TrackingService.ACTION_START_TRACKING,
						null, getActivity(), TrackingService.class));
			}

			isTrackingActive = !isTrackingActive;
			updateMenuIcon(isTrackingActive);
			return true;

		/*case R.id.action_refresh:
			TrackManager.getInstance().refresh();
			return true;*/
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon(boolean isWatchActive) {
		if (navigateAction != null) {
			if (isWatchActive) {
				navigateAction.setTitle("Unfollow");
				navigateAction.setIcon(R.drawable.ic_action_volume_muted);
			} else {
				navigateAction.setTitle("Follow");
				navigateAction.setIcon(R.drawable.ic_action_volume_on);
			}
		}
	}
}
