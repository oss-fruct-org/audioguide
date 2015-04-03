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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.NavigationDrawerFragment;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.PointCursorAdapter;
import org.fruct.oss.audioguide.events.DataUpdatedEvent;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackListener;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
import org.fruct.oss.audioguide.track.tasks.StoreTrackTask;
import org.fruct.oss.audioguide.util.EventReceiver;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.event.EventBus;

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

	private ImageView trackImageView;

	private Track track;
	private BroadcastReceiver inReceiver;
	private BroadcastReceiver outReceiver;

	private PointCursorAdapter pointAdapter;
	private CursorHolder cursorHolder;

	private TrackingService trackingService;

	private TrackingServiceConnection serviceConnection = new TrackingServiceConnection();
	private ViewGroup headerView;

	private StoreTrackTask storeTrackTask;

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

		trackManager = DefaultTrackManager.getInstance();

		Bundle arguments = getArguments();
		if (arguments != null) {
			track = arguments.getParcelable(ARG_TRACK);
		}

		if (savedInstanceState != null) {
			track = savedInstanceState.getParcelable(STATE_TRACK);
		}

		pointAdapter = new PointCursorAdapter(getActivity(), false);
		cursorHolder = trackManager.loadPoints(track);
		cursorHolder.attachToAdapter(pointAdapter);

		setListAdapter(pointAdapter);

		setHasOptionsMenu(true);
		setupRangeReceiver();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_list_view, container, false);
		final ListView listView = (ListView) view.findViewById(android.R.id.list);

		headerView = (ViewGroup) inflater.inflate(R.layout.fragment_track_detail_header, listView, false);
		trackImageView = (ImageView) headerView.findViewById(R.id.image_view);

		view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				int imageWidth = view.getMeasuredWidth();
				tryUpdateImage(imageWidth);
			}
		});

		setupTrack(track);

		listView.addHeaderView(headerView);
		return view;
	}

	private void setupTrack(final Track track) {
		TextView titleTextView = (TextView) headerView.findViewById(R.id.text_title);
		titleTextView.setText(track.getHumanReadableName());

		TextView descriptionTextView = (TextView) headerView.findViewById(R.id.text_description);
		if (Utils.isNullOrEmpty(track.getDescription())) {
			descriptionTextView.setVisibility(View.GONE);
		} else {
			descriptionTextView.setText(track.getDescription());
		}

		ImageButton imageButton = (ImageButton) headerView.findViewById(R.id.button);
		imageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (track.isLocal()) {
					startTrack();
				} else {
					saveTrack(true);
				}
			}
		});

		imageButton.setImageResource(track.isLocal() ? R.drawable.ic_action_place : R.drawable.ic_action_save);
	}

	private void startTrack() {
		if (pointAdapter.getCount() == 0) {
			return;
		}

		Point firstPoint = pointAdapter.getPoint(0);

		Bundle args = new Bundle();
		args.putParcelable(MapFragment.ARG_POINT, firstPoint);

		trackManager.activateTrackMode(track);
		NavigationDrawerFragment frag =
				(NavigationDrawerFragment)
						getActivity().getSupportFragmentManager()
								.findFragmentById(R.id.navigation_drawer);

		frag.selectItem(1, args);
	}

	private void saveTrack(boolean local) {
		if (storeTrackTask != null) {
			storeTrackTask.cancel(true);
		}

		storeTrackTask = new StoreTrackTask(track, local);
		storeTrackTask.execute();
	}

	private void tryUpdateImage(int imageWidth) {
		if (track.hasPhoto()) {
			String remoteUrl = track.getPhotoUrl();
			trackImageView.setAdjustViewBounds(true);
			ImageLoader.getInstance().displayImage(remoteUrl, trackImageView);
		} else {
			trackImageView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);

		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		getActivity().unbindService(serviceConnection);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (storeTrackTask != null) {
			storeTrackTask.cancel(true);
		}

		cursorHolder.close();
		trackManager = null;
		pointAdapter.close();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(inReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(outReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.points_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			saveTrack(false);

			break;

		case R.id.action_save:
			saveTrack(true);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setupRangeReceiver() {
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

		// Ignore header click
		if (position != 0) {
			Point point = pointAdapter.getPoint(position - 1);
			multiPanel.replaceFragment(PointDetailFragment.newInstance(point, false), this);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_TRACK, track);
	}

	@EventReceiver
	public void onEventMainThread(DataUpdatedEvent event) {
		track = trackManager.getTrackByName(track.getName());
		setupTrack(track);
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
