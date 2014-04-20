package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.AddPointFragment;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.PointModelAdapter;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link org.fruct.oss.audioguide.MultiPanel}
 * interface.
 */
public class PointFragment extends ListFragment {
	private final static Logger log = LoggerFactory.getLogger(PointFragment.class);

	private static final String STATE_TRACK = "track";

	public static final String ARG_TRACK = "arg_point";

	private MultiPanel multiPanel;
	private TrackManager trackManager;

	private Track track;
	private BroadcastReceiver inReceiver;
	private BroadcastReceiver outReceiver;
	private PointModelAdapter pointAdapter;
	private TrackingService trackingService;

	private TrackingServiceConnection serviceConnection = new TrackingServiceConnection();

	public static PointFragment newInstance(Track track) {
		Bundle args = new Bundle(1);
		args.putParcelable(ARG_TRACK, track);
		PointFragment fragment = new PointFragment();
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public PointFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();

		Bundle arguments = getArguments();
		if (arguments != null) {
			track = arguments.getParcelable(ARG_TRACK);
		}

		if (savedInstanceState != null) {
			track = savedInstanceState.getParcelable(STATE_TRACK);
		}

		pointAdapter = new PointModelAdapter(getActivity(),
				R.layout.list_point_item,
				trackManager.getPointsModel(track));
		setListAdapter(pointAdapter);

		setHasOptionsMenu(true);
		setupAudioReceiver();
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
	public void onDestroy() {
		super.onDestroy();
		trackManager = null;
		pointAdapter.close();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(inReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(outReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.points_menu, menu);
		inflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			log.debug("Action add selected");
			AddPointFragment dialogFragment = new AddPointFragment(track);
			dialogFragment.show(getFragmentManager(), "add-point-dialog");
			break;

		case R.id.action_refresh:
			log.debug("Action refresh selected from PointFragment");
			trackManager.refreshPoints(track);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setupAudioReceiver() {
		inReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				pointInRange(TrackingService.getPointFromIntent(intent));
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(inReceiver, new IntentFilter(TrackingService.BC_ACTION_POINT_IN_RANGE));

		outReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				pointOutRange(TrackingService.getPointFromIntent(intent));
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(outReceiver, new IntentFilter(TrackingService.BC_ACTION_POINT_OUT_RANGE));
	}

	private void pointInRange(Point point) {
		if (pointAdapter != null)
			pointAdapter.addHighlightedItem(point);
	}

	private void pointOutRange(Point point) {
		if (pointAdapter != null)
			pointAdapter.removeHighlightedItem(point);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			multiPanel = (MultiPanel) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		multiPanel = null;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Point point = pointAdapter.getItem(position);
		multiPanel.replaceFragment(PointDetailFragment.newInstance(point), this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_TRACK, track);
	}

	private class TrackingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			trackingService = ((TrackingService.TrackingServiceBinder) iBinder).getService();
			List<Point> pointsInRange = trackingService.getPointsInRange();
			pointAdapter.setHighlightedItems(pointsInRange);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			trackingService = null;
		}
	}
}
