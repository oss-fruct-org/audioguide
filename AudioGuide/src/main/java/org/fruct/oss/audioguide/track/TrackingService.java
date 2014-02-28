package org.fruct.oss.audioguide.track;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.LocationReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingService extends Service implements TrackManager.Listener, DistanceTracker.Listener {
	private final static Logger log = LoggerFactory.getLogger(TrackingService.class);

	public static final String BC_ACTION_POINT_IN_RANGE = "BC_ACTION_POINT_IN_RANGE";
	public static final String BC_ACTION_POINT_OUT_RANGE = "BC_ACTION_POINT_IOUTN_RANGE";

	public static final String ARG_POINT = "ARG_POINT";

	private DistanceTracker distanceTracker;
	private LocationReceiver locationReceiver;
	private TrackManager trackManager;

	private boolean isStarted = false;

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
		distanceTracker.setRadius(500);
		distanceTracker.addListener(this);

		// Insert points into distanceTracker
		tracksUpdated();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackManager.removeListener(this);
		stopTracking();

		log.info("TrackingService onDestroy");
	}

	public boolean isTrackingStarted() {
		return isStarted;
	}

	public void startTracking() {
		if (!isStarted) {
			isStarted = true;
			distanceTracker.start();
		}
	}

	public void stopTracking() {
		if (isStarted) {
			distanceTracker.stop();
			isStarted = false;
		}
	}

	private AudioServiceBinder binder = new AudioServiceBinder();

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

	public class AudioServiceBinder extends Binder {
		public TrackingService getService() {
			return TrackingService.this;
		}
	}
}
