package org.fruct.oss.audioguide.fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.fragments.edit.EditPointDialog;
import org.fruct.oss.audioguide.overlays.EditOverlay;
import org.fruct.oss.audioguide.overlays.MyPositionOverlay;
import org.fruct.oss.audioguide.track.AudioService;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.view.ViewGroup.LayoutParams;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MapFragment extends Fragment {
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

		trackManager = TrackManager.getInstance();
		setHasOptionsMenu(true);
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
		}
		return super.onOptionsItemSelected(item);
	}

	private void startAddingPoint() {
		if (editTrack != null) {
			EditPointDialog dialog = new EditPointDialog(null);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
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
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		assert view != null;

		createMapView(view);

		createClickHandlerOverlay();

		createCenterOverlay();
		updatePointsOverlay();
		createMyPositionOverlay();
		createEditOverlay();

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


	private void setupBottomPanel() {
		bottomToolbar = (ViewGroup) getView().findViewById(R.id.map_toolbar);

		final Button buttonPlay = (Button) bottomToolbar.findViewById(R.id.button_play);
		final Button buttonDetails = (Button) bottomToolbar.findViewById(R.id.button_details);
		final Button buttonStop = (Button) bottomToolbar.findViewById(R.id.button_stop);

		buttonPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (selectedPoint == null)
					return;

				Intent intent = new Intent(AudioService.ACTION_PLAY,
						Uri.parse(selectedPoint.getAudioUrl()),
						getActivity(), AudioService.class);
				getActivity().startService(intent);

				buttonPlay.setVisibility(View.GONE);
				buttonStop.setVisibility(View.VISIBLE);
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(AudioService.ACTION_STOP,
						null,
						getActivity(), AudioService.class);
				getActivity().startService(intent);

				buttonPlay.setVisibility(View.VISIBLE);
				buttonStop.setVisibility(View.GONE);
			}
		});

		if (selectedPoint.hasAudio()) {
			buttonPlay.setVisibility(View.VISIBLE);
		} else {
			buttonPlay.setVisibility(View.GONE);
		}

		buttonStop.setVisibility(View.GONE);
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
		mapView.getController().setZoom(15);
		mapView.getController().setCenter(new GeoPoint(61.783333, 34.35));

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

		Track globalEditTrack = trackManager.getEditingTrack();
		for (Track track : trackManager.getActiveTracks()) {

			int hashR = (int) (Utils.longHash(track.getName() + "'") % 50);
			int hashG = (int) (Utils.longHash(track.getName() + "4") % 50);
			int hashB = (int) (Utils.longHash(track.getName() + "3") % 50);

			EditOverlay trackOverlay = new EditOverlay(getActivity(),
					trackManager.getPointsModel(track),
					Utils.color(hashR, hashG, 255 - hashB) + 0xff000000);
			trackOverlays.add(trackOverlay);

			if (globalEditTrack == track) {
				// Save track that user edits
				editOverlay = trackOverlay;
				editOverlay.setColor(0xffaaccee);
				editTrack = track;
				trackOverlay.setEditable(true);
				trackOverlay.setListener(editOverlayListener);
			} else {
				trackOverlay.setListener(trackOverlayListener);
			}

			mapView.getOverlays().add(trackOverlay);
		}

		mapView.invalidate();
	}

	private void createMyPositionOverlay() {
		myPositionOverlay = new MyPositionOverlay(getActivity(), mapView);
		mapView.getOverlays().add(myPositionOverlay);
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

	private void createEditOverlay() {
		/*Track globalEditTrack = trackManager.getEditingTrack();

		if (globalEditTrack != null) {
			editTrack = globalEditTrack;

			Model<Point> pointsModel = trackManager.getPointsModel(editTrack);
			editOverlay = new EditOverlay<Point>(getActivity(), pointsModel);

			for (Point point : pointsModel) {
				editOverlay.addPoint(new GeoPoint(point.getLatE6(), point.getLonE6()), point);
			}

			editOverlay.setListener(editOverlayListener);
			mapView.getOverlays().add(editOverlay);
		}*/
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

	private EditPointDialog.Listener editDialogListener = new EditPointDialog.Listener() {
		@Override
		public void pointCreated(Point point) {
			log.debug("Point created callback");

			IGeoPoint mapCenter = mapView.getMapCenter();
			point.setCoordinates(mapCenter.getLatitudeE6(), mapCenter.getLongitudeE6());
			editOverlay.addPoint(AUtils.copyGeoPoint(mapCenter), point);
			trackManager.storePoint(editTrack, point);
			mapView.invalidate();
		}

		@Override
		public void pointUpdated(Point point) {
			log.debug("Point updated callback");
			trackManager.storePoint(editTrack, point);
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

			PointDetailFragment frag = PointDetailFragment.newInstance(point);
			multiPanel.replaceFragment(frag, MapFragment.this);
		}
	};

	private EditOverlay.Listener editOverlayListener = new EditOverlay.Listener() {
		@Override
		public void pointMoved(Point point, IGeoPoint geoPoint) {
			point.setCoordinates(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
			trackManager.storePoint(editTrack, point);
		}

		@Override
		public void pointPressed(Point point) {
			log.debug("Editable point pressed");

			EditPointDialog dialog = new EditPointDialog(point);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
		}
	};

}
