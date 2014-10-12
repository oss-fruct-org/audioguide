package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for handling points auto-refresh
 */
public class Refresher {
	private static final Logger log = LoggerFactory.getLogger(Refresher.class);

	/**
	 * Points will be refreshed when distance to previous refresh point will be more than (loadRadius * RADIUS_FACTOR)
	 */
	public static final double REFRESH_RADIUS_FACTOR = 0.3;

	/**
	 * Time after that points will be refreshed
	 */
	public static final long REFRESH_TIME_DELAY = 3600 * 10 * 1000; // 10 hours

	private final SharedPreferences pref;

	private static final String PREF_LATITUDE = "org.fruct.oss.audioguide.track.Refresher.LATITUDE";
	private static final String PREF_LONGITUDE	= "org.fruct.oss.audioguide.track.Refresher.LONGITUDE";
	private static final String PREF_TIMESTAMP = "org.fruct.oss.audioguide.track.Refresher.PREF_TIMESTAMP";

	private final TrackManager trackManager;
	private final Database database;

	private float lastLatitude;
	private float lastLongitude;
	private long lastTimestamp;

	private boolean hasPreviousLocation;

	private float loadRadius = -1;

	public Refresher(Context context, Database database, TrackManager trackManager) {
		pref = PreferenceManager.getDefaultSharedPreferences(context);

		this.trackManager = trackManager;
		this.database = database;

		if (pref.contains(PREF_LONGITUDE)) {
			lastLatitude = pref.getFloat(PREF_LATITUDE, -1);
			lastLongitude = pref.getFloat(PREF_LONGITUDE, -1);
			lastTimestamp = pref.getLong(PREF_TIMESTAMP, -1);
			hasPreviousLocation = true;
		}
	}

	public void refresh(Location location) {
		log.info("Auto-refreshing points");
		hasPreviousLocation = true;
		lastLatitude = (float) location.getLatitude();
		lastLongitude = (float) location.getLongitude();
		lastTimestamp = location.getTime();

		pref.edit().putFloat(PREF_LATITUDE, lastLatitude)
				.putFloat(PREF_LONGITUDE, lastLongitude)
				.putLong(PREF_TIMESTAMP, lastTimestamp)
				.apply();

		trackManager.requestTracksInRadius();
		trackManager.requestPointsInRadius(lastLatitude, lastLongitude, false);

	}

	public void updateUserLocation(Location location) {
		if (loadRadius < 0) {
			return;
		}

		if (!hasPreviousLocation) {
			refresh(location);
			return;
		}

		long timeDiff = location.getTime() - lastTimestamp;
		if (timeDiff > REFRESH_TIME_DELAY) {
			refresh(location);
			return;
		}

		float[] ret = new float[1];
		Location.distanceBetween(location.getLatitude(), location.getLongitude(), lastLatitude, lastLongitude, ret);

		if (ret[0] > loadRadius * REFRESH_RADIUS_FACTOR) {
			log.debug("Refreshing because large distance diff {} > {}", ret[0], loadRadius * REFRESH_RADIUS_FACTOR);
			refresh(location);
		}
	}

	public void updateLoadRadius(float loadRadius) {
		this.loadRadius = loadRadius;
	}
}
