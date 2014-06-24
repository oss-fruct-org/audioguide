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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.dialogs.EditPointDialog;
import org.fruct.oss.audioguide.dialogs.SelectTrackDialog;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.overlays.EditOverlay;
import org.fruct.oss.audioguide.overlays.MyPositionOverlay;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackingService;
import org.fruct.oss.audioguide.track.track2.DefaultTrackManager;
import org.fruct.oss.audioguide.track.track2.TrackManager;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MapFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static Logger log = LoggerFactory.getLogger(MapFragment.class);

	private MapView mapView;
	private TrackManager trackManager;
	private TrackingService trackingService;
	private TrackingServiceConnection serviceConnection = new TrackingServiceConnection();

	private MyPositionOverlay myPositionOverlay;
	private BroadcastReceiver locationReceiver;

	private ViewGroup bottomToolbar;
	private MultiPanel multiPanel;

	private Point selectedPoint;

	private List<EditOverlay> trackOverlays = new ArrayList<EditOverlay>();

	private EditOverlay editOverlay;
	private Track editTrack;

	private Model<Point> searchingPoints;

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
	public void onCreate(Bundle savedInstanceState) {
		log.trace("MapFragment onCreate");

		super.onCreate(savedInstanceState);

		trackManager = DefaultTrackManager.getInstance();
		setHasOptionsMenu(true);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.map_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_place:
			mockLocation();
			break;
		case R.id.action_add:
			startAddingPoint();
			break;
		case R.id.action_find_me:
			if (myPositionOverlay != null) {
				GeoPoint newMapCenter = new GeoPoint(myPositionOverlay.getLocation());
				if (mapView.getZoomLevel() < 15) {
					mapView.getController().setZoom(15);
				}

				mapView.getController().animateTo(newMapCenter);
			}
			break;
		case R.id.action_search:
			startSearchingPoints();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startSearchingPoints() {
		Toast.makeText(getActivity(), "Searching near points...", Toast.LENGTH_LONG).show();

		trackManager.requestPointsInRadius((float) myPositionOverlay.getLocation().getLatitude(),
				(float) myPositionOverlay.getLocation().getLatitude(),
				1000, true);
	}

	private void startAddingPoint() {
		//if (editTrack != null) {
			EditPointDialog dialog = EditPointDialog.newInstance(null);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
		//}
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
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		assert view != null;

		createMapView(view);

		createClickHandlerOverlay();

		createCenterOverlay();
		updatePointsOverlay();
		createMyPositionOverlay();

		if (savedInstanceState != null) {
			GeoPoint mapCenter = new GeoPoint(savedInstanceState.getInt("map-center-lat"),
					savedInstanceState.getInt("map-center-lon"));
			int zoom = savedInstanceState.getInt("zoom");

			mapView.getController().setZoom(zoom);
			mapView.getController().setCenter(mapCenter);
		} else {
			mapView.getController().setZoom(15);
			mapView.getController().setCenter(new GeoPoint(61.783333, 34.35));
		}

		for (Overlay overlay : mapView.getOverlays()) {
			log.debug("OVERLAY: {}", overlay.getClass().getName());
		}

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		log.trace("MapFragment onStart");


		getActivity().bindService(new Intent(getActivity(), TrackingService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Location location = intent.getParcelableExtra(TrackingService.ARG_LOCATION);
				if (myPositionOverlay != null) {
					myPositionOverlay.setLocation(location);
					mapView.invalidate();
				}
			}
		}, new IntentFilter(TrackingService.BC_ACTION_NEW_LOCATION));
	}

	@Override
	public void onResume() {
		super.onResume();
		log.trace("MapFragment onResume");

		if (getArguments() != null) {
			Point point = getArguments().getParcelable("point");
			centerOn(new GeoPoint(point.getLatE6(), point.getLonE6()), 17);
		}
	}

	@Override
	public void onStop() {
		log.trace("MapFragment onStop");
		super.onStop();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locationReceiver);
		getActivity().unbindService(serviceConnection);
	}

	@Override
	public void onDestroy() {
		log.trace("MapFragment onDestroy");
		super.onDestroy();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.unregisterOnSharedPreferenceChangeListener(this);

		for (EditOverlay trackOverlay : trackOverlays) {
			trackOverlay.close();
		}

		trackManager = null;
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

				MapView.Projection proj = mapView.getProjection();
				android.graphics.Point mapCenter = proj.toMapPixels(mapView.getMapCenter(), null);
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
		editOverlay = null;
		editTrack = null;

		// Free points
		EditOverlay freePointsOverlay = new EditOverlay(getActivity(),
				trackManager.loadLocalPoints(), trackManager.loadRelations(),
				1);

		freePointsOverlay.setListener(trackOverlayListener);
		freePointsOverlay.setMarkerIndex(1);
		trackOverlays.add(freePointsOverlay);
		mapView.getOverlays().add(freePointsOverlay);

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

	public void centerOn(GeoPoint geoPoint, int zoom) {
		if (mapView != null) {
			mapView.getController().setZoom(zoom);
			mapView.getController().animateTo(geoPoint);
		}
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

	private SelectTrackDialog.Listener addPointToTrackListener = new SelectTrackDialog.Listener() {
		@Override
		public void trackSelected(Track track) {
			trackManager.insertToTrack(track, selectedPoint);
			selectedPoint = null;
		}
	};

	private EditPointDialog.Listener editDialogListener = new EditPointDialog.Listener() {
		@Override
		public void pointCreated(Point point) {
			log.debug("Point created callback");

			IGeoPoint mapCenter = mapView.getMapCenter();
			point.setCoordinates(mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6());

			trackManager.insertPoint(point);
		}

		@Override
		public void pointUpdated(Point point) {
			log.debug("Point updated callback");
			throw new UnsupportedOperationException();
		}
	};

	private EditOverlay.Listener trackOverlayListener = new EditOverlay.Listener() {
		@Override
		public void pointMoved(Point point, IGeoPoint geoPoint) {
			assert false;
		}

		@Override
		public void pointPressed(Point point) {
			log.debug("Simple point pressed");

			/*DetailsOverlay detailsOverlay = new DetailsOverlay(getActivity(), mapView, point);
			mapView.getOverlays().add(detailsOverlay);
			mapView.invalidate();*/

			/*View detailContainer = getActivity().findViewById(R.id.panel_details);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					getView().getMeasuredWidth() - Utils.getDP(24),
					getView().getMeasuredHeight() - Utils.getDP(24));
			params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

			detailContainer.setLayoutParams(params);*/

			PointDetailFragment detailsFragment = PointDetailFragment.newInstance(point, true);
			getActivity().getSupportFragmentManager().beginTransaction()
					.replace(R.id.panel_details, detailsFragment, "details-fragment")
					.commit();

			//PointDetailFragment frag = PointDetailFragment.newInstance(point);
			//multiPanel.replaceFragment(frag, MapFragment.this);
		}

		@Override
		public void pointLongPressed(final Point point) {
			//Toast.makeText(getActivity(), "Long press detected", Toast.LENGTH_SHORT).show();
			/*EditPointDialog dialog = EditPointDialog.newInstance(point);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");*/

			PopupMenu menu = new PopupMenu(getActivity(), getView().findViewById(R.id.map_anchor));
			final MenuItem item = menu.getMenu().add("Add to track");
			menu.show();

			menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					if (menuItem == item) {
						SelectTrackDialog dialog = SelectTrackDialog.newInstance();
						dialog.setListener(addPointToTrackListener);
						selectedPoint = point;
						dialog.show(getFragmentManager(), "select-track-dialog");
						return true;
					}

					return false;
				}
			});
		}
	};

	private EditOverlay.Listener editOverlayListener = new EditOverlay.Listener() {
		@Override
		public void pointMoved(Point point, IGeoPoint geoPoint) {
			point.setCoordinates(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
			//trackManager.storePoint(editTrack, point);
			throw new UnsupportedOperationException();
		}

		@Override
		public void pointPressed(Point point) {
			log.debug("Editable point pressed");

			EditPointDialog dialog = EditPointDialog.newInstance(point);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
		}

		@Override
		public void pointLongPressed(Point p) {
		}
	};
}
