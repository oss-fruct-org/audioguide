package org.fruct.oss.audioguide.fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.fruct.oss.audioguide.MainActivity;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.SynchronizerService;
import org.fruct.oss.audioguide.config.Config;
import org.fruct.oss.audioguide.dialogs.EditPointDialog;
import org.fruct.oss.audioguide.dialogs.SelectTrackDialog;
import org.fruct.oss.audioguide.events.PointInRangeEvent;
import org.fruct.oss.audioguide.overlays.EditOverlay;
import org.fruct.oss.audioguide.overlays.MyPositionOverlay;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.LocationEvent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
import org.fruct.oss.audioguide.track.tasks.PointsTask;
import org.fruct.oss.audioguide.util.EventReceiver;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import static android.view.ViewGroup.LayoutParams;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MapFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static Logger log = LoggerFactory.getLogger(MapFragment.class);

	private final static String PREF_LATITUDE = "pref-latitude";
	private final static String PREF_LONGITUDE = "pref-longitude";
	private final static String PREF_ZOOM = "pref-zoom";
	public static final String ARG_POINT = "point";

	private MapView mapView;
	private TrackManager trackManager;
	private TrackingService trackingService;
	private TrackingServiceConnection serviceConnection = new TrackingServiceConnection();
	private SharedPreferences pref;

	private MyPositionOverlay myPositionOverlay;

	private ViewGroup bottomToolbar;

	private List<EditOverlay> trackOverlays = new ArrayList<EditOverlay>();

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment MapFragment.
	 */
	public static MapFragment newInstance() {
		return new MapFragment();
	}
	public MapFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section2),
				ActionBar.NAVIGATION_MODE_STANDARD, null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		log.trace("MapFragment onCreate");

		super.onCreate(savedInstanceState);

		trackManager = DefaultTrackManager.getInstance();
		setHasOptionsMenu(true);

		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.map_menu, menu);
		inflater.inflate(R.menu.categories_filter, menu);

		if (Config.isEditLocked()) {
			menu.findItem(R.id.action_add).setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_place:
			mockLocation();
			break;
		case R.id.action_find_me:
			if (myPositionOverlay != null && myPositionOverlay.getLocation() != null) {
				GeoPoint newMapCenter = new GeoPoint(myPositionOverlay.getLocation());
				if (mapView.getZoomLevel() < 15) {
					mapView.getController().setZoom(15);
				}

				mapView.getController().animateTo(newMapCenter);
			} else {
				Toast.makeText(getActivity(), R.string.warn_no_providers, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.action_search:
			startSearchingPoints();
			break;
		case R.id.action_stop_guide:
			if (pref.contains(TrackManager.PREF_TRACK_MODE)) {
				trackManager.activateTrackMode(null);
				updatePointsOverlay();
			} else {
				SelectTrackDialog dialog = SelectTrackDialog.newInstance();
				dialog.setListener(activateTrackListener);

				FragmentTransaction trans = getFragmentManager().beginTransaction();
				trans.addToBackStack("select-track-dialog");
				dialog.show(trans, "select-track-dialog");
			}
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void startSearchingPoints() {
		Toast.makeText(getActivity(), "Searching near points...", Toast.LENGTH_LONG).show();

		if (myPositionOverlay.getLocation() != null) {
			SynchronizerService.startSyncPoints(getActivity());
		} else {
			Toast.makeText(getActivity(), R.string.warn_no_providers, Toast.LENGTH_SHORT).show();
		}
	}

	private void mockLocation() {
		if (trackingService != null) {
			IGeoPoint mapCenter = mapView.getMapCenter();
			trackingService.mockLocation(mapCenter.getLatitude(), mapCenter.getLongitude());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		log.debug("MapFragment.onCreateView");

		// Inflate the layout for this fragment
		final View view = inflater.inflate(R.layout.fragment_map, container, false);
		assert view != null;

		createMapView(view);


		final GeoPoint initialMapCenter;
		final int initialZoomLevel;

		if (savedInstanceState != null) {
			initialMapCenter = new GeoPoint(savedInstanceState.getInt("map-center-lat"),
					savedInstanceState.getInt("map-center-lon"));
			initialZoomLevel = savedInstanceState.getInt("zoom");
		} else if (getArguments() != null) {
			Point centerPoint = getArguments().getParcelable(ARG_POINT);
			initialMapCenter = new GeoPoint(centerPoint.getLatE6(), centerPoint.getLonE6());
			initialZoomLevel = 17;
		} else {
			initialZoomLevel = pref.getInt(PREF_ZOOM, 15);
			initialMapCenter = new GeoPoint(pref.getFloat(PREF_LATITUDE, 61.7833f),
					pref.getFloat(PREF_LONGITUDE, 34.35f));
		}

		createClickHandlerOverlay();
		createCenterOverlay();
		updatePointsOverlay();
		createMyPositionOverlay();

		final ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				//noinspection deprecation
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				mapView.getController().setZoom(initialZoomLevel);
				mapView.getController().setCenter(initialMapCenter);
			}
		});

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		log.trace("MapFragment onStart");

		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);

		EventBus.getDefault().register(this);
	}


	@EventReceiver
	public void onEventMainThread(PointInRangeEvent event) {
		PointDetailFragment detailsFragment = (PointDetailFragment) getFragmentManager().findFragmentByTag("details-fragment");
		if (detailsFragment != null) {
			getFragmentManager().popBackStack("details-fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}

		detailsFragment = PointDetailFragment.newInstance(event.getPoint(), true);
		getFragmentManager().beginTransaction()
				.addToBackStack("details-fragment")
				.add(R.id.panel_details, detailsFragment, "details-fragment")
				.commit();
	}

	@EventReceiver
	public void onEventMainThread(LocationEvent event) {
		if (myPositionOverlay != null) {
			myPositionOverlay.setLocation(event.getLocation());
			mapView.invalidate();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		/*if (getArguments() != null) {
			Point point = getArguments().getParcelable(ARG_POINT);
			centerOn(new GeoPoint(point.getLatE6(), point.getLonE6()), 17);
		}*/
	}

	@Override
	public void onStop() {
		pref.edit()
				.putInt(PREF_ZOOM, mapView.getZoomLevel())
				.putFloat(PREF_LATITUDE, (float) mapView.getMapCenter().getLatitude())
				.putFloat(PREF_LONGITUDE, (float) mapView.getMapCenter().getLongitude()).apply();

		EventBus.getDefault().unregister(this);

		getActivity().unbindService(serviceConnection);

		super.onStop();
	}

	@Override
	public void onDestroy() {
		log.trace("MapFragment onDestroy");

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.unregisterOnSharedPreferenceChangeListener(this);

		for (EditOverlay trackOverlay : trackOverlays) {
			trackOverlay.close();
		}

		mapView.getTileProvider().clearTileCache();

		trackManager = null;
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		log.debug("MapFragment onSaveInstanceState");
		super.onSaveInstanceState(outState);

		IGeoPoint screenPos = mapView.getMapCenter();
		int zoom = mapView.getZoomLevel();

		outState.putInt("map-center-lat", screenPos.getLatitudeE6());
		outState.putInt("map-center-lon", screenPos.getLongitudeE6());
		outState.putInt("zoom", zoom);
	}

	private void createMapView(View view) {
		final Context context = getActivity();
		final ViewGroup layout = (ViewGroup) view.findViewById(R.id.map_layout);
		final ResourceProxyImpl proxy = new ResourceProxyImpl(this.getActivity().getApplicationContext());

		/*final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(context.getApplicationContext());


		final OnlineTileSourceBase tileSource = TileSourceFactory.MAPQUESTOSM;

		final TileWriter tileWriter = new TileWriter();
		final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(registerReceiver, tileSource);

		final NetworkAvailabliltyCheck networkAvailabilityCheck = new NetworkAvailabliltyCheck(context);
		final MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, tileWriter, networkAvailabilityCheck);

		final MapTileProviderArray tileProviderArray = new MapTileProviderArray(tileSource, registerReceiver,
				new MapTileModuleProviderBase[] { fileSystemProvider, downloaderProvider });
*/


		mapView = new MapView(context, 256, proxy);
		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mapView.setMultiTouchControls(true);

		layout.addView(mapView);

		setHardwareAccelerationOff();
	}

	private void createCenterOverlay() {
		Overlay overlay = new Overlay(getActivity()) {
			Paint paint = new Paint();
			{
				paint.setColor(Color.GRAY);
				paint.setStrokeWidth(2);
				paint.setStyle(Paint.Style.FILL);
				paint.setAntiAlias(true);
			}

			@Override
			protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
				if (shadow)
					return;

				Projection proj = mapView.getProjection();
				android.graphics.Point mapCenter = proj.toPixels(mapView.getMapCenter(), null);
				canvas.drawCircle(mapCenter.x, mapCenter.y, 5, paint);
			}
		};

		mapView.getOverlays().add(overlay);
	}

	private void updatePointsOverlay() {
		// Clear all previous data
		for (EditOverlay trackOverlay : trackOverlays) {
			mapView.getOverlays().remove(trackOverlay);
		}

		trackOverlays.clear();

		CursorHolder activePoints;

		// active track can be null after cleaning database
		// unlikely on non-debug use
		Track activeTrack = null;
		String activeTrackName = pref.getString(TrackManager.PREF_TRACK_MODE, null);
		if (activeTrackName != null)
			activeTrack = trackManager.getTrackByName(activeTrackName);

		if (activeTrack != null) {
			Toast.makeText(getActivity(), R.string.str_tracks_mode, Toast.LENGTH_LONG).show();
			activePoints = trackManager.loadPoints(activeTrack);
		} else {
			Toast.makeText(getActivity(), R.string.str_points_mode, Toast.LENGTH_LONG).show();
			activePoints = trackManager.loadLocalPoints();
		}

		EditOverlay pointsOverlay = new EditOverlay(getActivity(), activePoints, 1,
				mapView, activeTrack != null);

		pointsOverlay.setListener(trackOverlayListener);
		pointsOverlay.setMarkerIndex(1);

		trackOverlays.add(pointsOverlay);
		mapView.getOverlays().add(pointsOverlay);

		mapView.invalidate();
	}

	private void createMyPositionOverlay() {
		myPositionOverlay = new MyPositionOverlay(getActivity(), mapView);
		mapView.getOverlays().add(myPositionOverlay);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		myPositionOverlay.setRange(pref.getInt(SettingsActivity.PREF_RANGE, 50));
	}

	private void createClickHandlerOverlay() {
		Overlay clickHandlerOverlay = new Overlay(getActivity()) {
			@Override
			protected void draw(Canvas c, MapView mapView, boolean shadow) {
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
				if (bottomToolbar != null) {
					Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_down);
					assert anim != null;
					bottomToolbar.startAnimation(anim);
					bottomToolbar.setVisibility(View.GONE);
					bottomToolbar = null;
				}

				return false;
			}
		};

		mapView.getOverlays().add(clickHandlerOverlay);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		log.debug("MapFragment onActivityResult {}, {}", requestCode, resultCode);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		if (s.equals(SettingsActivity.PREF_RANGE)) {
			if (myPositionOverlay != null) {
				myPositionOverlay.setRange(sharedPreferences.getInt(s, 50));
			}
		}
	}

	private class TrackingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			trackingService = ((TrackingService.TrackingServiceBinder) iBinder).getService();
			trackingService.sendLastLocation();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			trackingService = null;
		}
	}

	private SelectTrackDialog.Listener activateTrackListener = new SelectTrackDialog.Listener() {
		@Override
		public void trackSelected(Track track) {
			trackManager.activateTrackMode(track);
			updatePointsOverlay();
		}
	};

	private EditOverlay.Listener trackOverlayListener = new EditOverlay.Listener() {
		@Override
		public void pointMoved(Point point, IGeoPoint geoPoint) {
		}

		@Override
		public void pointPressed(Point point) {
			log.debug("Simple point pressed");

			PointDetailFragment detailsFragment = PointDetailFragment.newInstance(point, true);
			getActivity().getSupportFragmentManager().beginTransaction()
					.addToBackStack("details-fragment")
					.replace(R.id.panel_details, detailsFragment, "details-fragment")
					.commit();
		}

		@Override
		public void pointLongPressed(Point point) {
		}
	};
}
