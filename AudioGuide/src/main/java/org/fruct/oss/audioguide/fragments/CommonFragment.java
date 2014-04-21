package org.fruct.oss.audioguide.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.AudioService;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFragment extends Fragment {
	private final static Logger log = LoggerFactory.getLogger(CommonFragment.class);

	public static final String BC_ERROR_MESSAGE = "org.fruct.oss.audioguide.CommonFragment.BC_ERROR_MESSAGE";
	public static final String BC_ARG_MESSAGE = "message";

	private MenuItem navigateAction;
	private boolean isWatchActive;
	private BroadcastReceiver watchReceiver;
	private BroadcastReceiver audioStopReceiver;
	private BroadcastReceiver errorReceiver;

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

		getActivity().startService(new Intent(getActivity(), TrackingService.class));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(watchReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				isWatchActive = true;
				updateMenuIcon(true);
			}
		}, new IntentFilter(AudioService.BC_START_WATCH_POINTS));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(audioStopReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				isWatchActive = false;
				updateMenuIcon(false);
			}
		}, new IntentFilter(AudioService.BC_STOP_SERVICE));

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

		getActivity().stopService(new Intent(getActivity(), TrackingService.class));
		getActivity().stopService(new Intent(getActivity(), AudioService.class));

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(watchReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(audioStopReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(errorReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.navigate, menu);
		navigateAction = menu.findItem(R.id.action_navigate);
		updateMenuIcon(isWatchActive);

		getActivity().startService(new Intent(AudioService.ACTION_SEND_STATE, null,
				getActivity(), AudioService.class));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_navigate:
			if (isWatchActive)
				getActivity().stopService(new Intent(getActivity(), AudioService.class));
			else
				getActivity().startService(new Intent(AudioService.ACTION_WATCH_POINTS, null,
						getActivity(), AudioService.class));

			return true;

		/*case R.id.action_refresh:
			TrackManager.getInstance().refresh();
			return true;*/
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon(boolean isWatchActive) {
		if (isWatchActive) {
			navigateAction.setTitle("Unfollow");
			navigateAction.setIcon(R.drawable.ic_action_volume_muted);
		} else {
			navigateAction.setTitle("Follow");
			navigateAction.setIcon(R.drawable.ic_action_volume_on);
		}

		this.isWatchActive = isWatchActive;
	}
}
