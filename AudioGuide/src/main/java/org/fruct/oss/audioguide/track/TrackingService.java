package org.fruct.oss.audioguide.track;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.LocationReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackingService extends Service implements TrackManager.Listener, DistanceTracker.Listener {
	private final static Logger log = LoggerFactory.getLogger(TrackingService.class);

	public static final String BC_ACTION_POINT_IN_RANGE = "BC_ACTION_POINT_IN_RANGE";
	public static final String BC_ACTION_POINT_OUT_RANGE = "BC_ACTION_POINT_IOUTN_RANGE";

	public static final String ARG_POINT = "ARG_POINT";

	private DistanceTracker distanceTracker;
	private TrackManager trackManager;
	private LocationReceiver locationReceiver;

	public TrackingService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
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

		// Insert points into distanceTracker
		tracksUpdated();
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
	public void trackUpdated(Track track) {
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

	public void mockLocation(double latitude, double longitude) {
		Location location = new Location("mock-provider");
		location.setAccuracy(0);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		location.setTime(System.currentTimeMillis());

		locationReceiver.mockLocation(location);
	}

	public class TrackingServiceBinder extends Binder {
		public TrackingService getService() {
			return TrackingService.this;
		}
	}
}
