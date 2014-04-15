package org.fruct.oss.audioguide.track;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.LocationReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackingService extends Service implements TrackManager.Listener, DistanceTracker.Listener, LocationReceiver.Listener {
	private final static Logger log = LoggerFactory.getLogger(TrackingService.class);

	public static final String BC_ACTION_POINT_IN_RANGE = "BC_ACTION_POINT_IN_RANGE";
	public static final String BC_ACTION_POINT_OUT_RANGE = "BC_ACTION_POINT_IOUTN_RANGE";
	public static final String BC_ACTION_NEW_LOCATION = "BC_ACTION_NEW_LOCATION";

	public static final String ARG_POINT = "ARG_POINT";
	public static final String ARG_LOCATION = "ARG_LOCATION";

	private DistanceTracker distanceTracker;
	private TrackManager trackManager;
	private LocationReceiver locationReceiver;

	public TrackingService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private void startLocationTrack() {
		new Thread() {
			@Override
			public void run() {
				double[][] arr
						= {
						{61.788009,34.356433},
						{61.787410,34.354308},
						{61.786719,34.352334},
						{61.786100,34.350339},
						{61.784941,34.346605},
						{61.780592,34.352454}};

				for (double[] point : arr) {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						return;
					}

					mockLocation(point[0], point[1]);
				}

			}
		}.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.info("TrackingService onCreate");

		locationReceiver = new LocationReceiver(this);
		trackManager = TrackManager.getInstance();

		trackManager.addListener(this);

		distanceTracker = new DistanceTracker(trackManager, locationReceiver);
		distanceTracker.setRadius(50);
		distanceTracker.addListener(this);
		distanceTracker.start();

		locationReceiver.addListener(this);

		// Insert points into distanceTracker
		tracksUpdated();

		//startLocationTrack();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackManager.removeListener(this);
		distanceTracker.stop();

		log.info("TrackingService onDestroy");
	}

	private TrackingServiceBinder binder = new TrackingServiceBinder();

	@Override
	public void tracksUpdated() {
		distanceTracker.setTracks(trackManager.getActiveTracks());
	}

	@Override
	public void pointsUpdated(Track track) {
	}


	@Override
	public void pointInRange(Point point) {
		Intent intent = new Intent(BC_ACTION_POINT_IN_RANGE);
		intent.putExtra(ARG_POINT, point);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public void pointOutRange(Point point) {
		Intent intent = new Intent(BC_ACTION_POINT_OUT_RANGE);
		intent.putExtra(ARG_POINT, point);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}


	public List<Point> getPointsInRange() {
		return distanceTracker.getPointsInRange();
	}

	public static Point getPointFromIntent(Intent intent) {
		return intent.getParcelableExtra(ARG_POINT);
	}

	public void mockLocation(final double latitude, final double longitude) {
		Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Location location = new Location("mock-provider");
				location.setAccuracy(0);
				location.setLatitude(latitude);
				location.setLongitude(longitude);
				location.setTime(System.currentTimeMillis());

				locationReceiver.mockLocation(location);
			}
		}, 4000);

	}

	@Override
	public void newLocation(Location location) {
		Intent intent = new Intent(BC_ACTION_NEW_LOCATION);
		intent.putExtra(ARG_LOCATION, location);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public void sendLastLocation() {
		locationReceiver.sendLastLocation();
	}

	public class TrackingServiceBinder extends Binder {
		public TrackingService getService() {
			return TrackingService.this;
		}
	}
}
