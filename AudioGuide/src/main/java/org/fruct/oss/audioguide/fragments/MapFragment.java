package org.fruct.oss.audioguide.fragments;


import android.annotation.TargetApi;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.overlays.MyPositionOverlay;
import org.fruct.oss.audioguide.track.AudioService;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.TrackingService;
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

import static android.view.ViewGroup.LayoutParams;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MapFragment extends Fragment implements TrackManager.Listener {
	private final static Logger log = LoggerFactory.getLogger(MapFragment.class);

	private MapView mapView;
	private ItemizedIconOverlay<OverlayItem> trackOverlay;
	private TrackManager trackManager;
	private TrackingService trackingService;
	private TrackingServiceConnection serviceConnection = new TrackingServiceConnection();

	private MyPositionOverlay myPositionOverlay;
	private BroadcastReceiver locationReceiver;

	private ViewGroup bottomToolbar;

	private Point selectedPoint;

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
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();
		trackManager.addListener(this);
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
		}
		return super.onOptionsItemSelected(item);
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
		createCenterOverlay();
		updatePointsOverlay();
		createMyPositionOverlay();

		bottomToolbar = (ViewGroup) view.findViewById(R.id.map_toolbar);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

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
	public void onStop() {
		super.onStop();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locationReceiver);
		getActivity().unbindService(serviceConnection);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackManager.removeListener(this);
		trackManager = null;
	}

	private void setupBottomPanel() {
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
		if (trackOverlay != null)
			mapView.getOverlays().remove(trackOverlay);

		ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();

		List<Track> tracks = trackManager.getActiveTracks();
		for (Track track : tracks) {
			List<Point> points = trackManager.getPoints(track);

			for (Point point : points) {
				OverlayItem overlayItem = new PointOverlayItem(point);
				overlayItems.add(overlayItem);
			}
		}

		trackOverlay = new ItemizedIconOverlay<OverlayItem>(getActivity(), overlayItems, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem item) {
				log.debug("onItemSingleTapUp");

				MapFragment.this.selectedPoint = ((PointOverlayItem) item).getAGPoint();

				setupBottomPanel();

				Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_up);
				assert anim != null;
				bottomToolbar.startAnimation(anim);
				bottomToolbar.setVisibility(View.VISIBLE);
				return false;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem item) {
				return false;
			}
		});

		mapView.getOverlays().add(trackOverlay);
	}

	private void createMyPositionOverlay() {
		myPositionOverlay = new MyPositionOverlay(getActivity(), mapView);
		mapView.getOverlays().add(myPositionOverlay);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	// TrackManager.Listener methods
	@Override
	public void tracksUpdated() {

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
			trackingService.sendLastLocation();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			trackingService = null;
		}
	}

	private class PointOverlayItem extends OverlayItem {
		private final Point point;

		public PointOverlayItem(Point point) {
			super(point.getName(), point.getDescription(),
					new GeoPoint(point.getLatE6(), point.getLonE6()));

			this.point = point;
		}

		public Point getAGPoint() {
			return point;
		}
	}
}
