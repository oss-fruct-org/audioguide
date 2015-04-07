package org.fruct.oss.audioguide.fragments;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.SingletonService;
import org.fruct.oss.audioguide.dialogs.CategoriesDialog;
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
	private boolean isStateSaved;

	private Context context;

	private ServiceConnection singletonServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	public static CommonFragment newInstance() {
		return new CommonFragment();
	}
	public CommonFragment() {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		isStateSaved = true;
		outState.putBoolean("isTrackingActive", isTrackingActive);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log.debug("CommonFragment.onCreate");
		// It seems that using Activity's context is dangerous with setRetainInstance
		context = App.getContext();

		setHasOptionsMenu(true);
		setRetainInstance(true);

		LocalBroadcastManager.getInstance(context).registerReceiver(errorReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String message = intent.getStringExtra(BC_ARG_MESSAGE);
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		}, new IntentFilter(CommonFragment.BC_ERROR_MESSAGE));

		App.getContext().bindService(new Intent(context, TrackingService.class),
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

		if (savedInstanceState != null) {
			isTrackingActive = savedInstanceState.getBoolean("isTrackingActive");
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		isStateSaved = false;
		context.bindService(new Intent(context, SingletonService.class), singletonServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();
		context.unbindService(singletonServiceConnection);
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(errorReceiver);

		if (trackingServiceConnection != null) {
			context.unbindService(trackingServiceConnection);
			trackingServiceConnection = null;
		}

		super.onDestroy();
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
				context.startService(new Intent(TrackingService.ACTION_STOP_TRACKING,
						null, context, TrackingService.class));
			} else {
				context.startService(new Intent(TrackingService.ACTION_START_TRACKING,
						null, context, TrackingService.class));
			}

			isTrackingActive = !isTrackingActive;
			updateMenuIcon(isTrackingActive);
			return true;

		case R.id.action_filter:
			CategoriesDialog dialog = CategoriesDialog.newInstance();
			dialog.show(getFragmentManager(), "categories-dialog");
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon(boolean isWatchActive) {
		if (navigateAction != null) {
			if (isWatchActive) {
				navigateAction.setTitle("Unfollow");
				navigateAction.setIcon(R.drawable.ic_action_inv_stop);
			} else {
				navigateAction.setTitle("Follow");
				navigateAction.setIcon(R.drawable.ic_action_inv_play);
			}
		}
	}
}
