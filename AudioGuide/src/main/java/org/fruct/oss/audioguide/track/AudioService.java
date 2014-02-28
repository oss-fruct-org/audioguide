package org.fruct.oss.audioguide.track;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;

import org.fruct.oss.audioguide.LocationReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioService extends Service implements TrackManager.Listener, DistanceTracker.Listener {
	private final static Logger log = LoggerFactory.getLogger(AudioService.class);

	private DistanceTracker distanceTracker;
	private LocationReceiver locationReceiver;
	private TrackManager trackManager;

	private boolean isStarted = false;

    public AudioService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
		return binder;
    }

	@Override
	public void onCreate() {
		super.onCreate();
		log.info("AudioService onCreate");

		locationReceiver = new LocationReceiver(this);
		trackManager = TrackManager.getInstance();

		trackManager.addListener(this);

		distanceTracker = new DistanceTracker(trackManager, locationReceiver);

		distanceTracker.addListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		trackManager.removeListener(this);
		stopTracking();

		log.info("AudioService onDestroy");
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
		log.debug("pointInRange");
	}

	@Override
	public void pointOutRange(Point point) {
		log.debug("pointOutRange");
	}

	public class AudioServiceBinder extends Binder {
		public AudioService getService() {
			return AudioService.this;
		}
	}
}
