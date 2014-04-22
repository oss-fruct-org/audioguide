package org.fruct.oss.audioguide.track;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.sonyericsson.illumination.IlluminationIntent;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackingService extends Service implements DistanceTracker.Listener, LocationReceiver.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
	private final static Logger log = LoggerFactory.getLogger(TrackingService.class);

	public static final String BC_ACTION_POINT_IN_RANGE = "BC_ACTION_POINT_IN_RANGE";
	public static final String BC_ACTION_POINT_OUT_RANGE = "BC_ACTION_POINT_IOUTN_RANGE";
	public static final String BC_ACTION_NEW_LOCATION = "BC_ACTION_NEW_LOCATION";

	public static final String ARG_POINT = "ARG_POINT";
	public static final String ARG_LOCATION = "ARG_LOCATION";

	public static final String ACTION_WAKE = "org.fruct.oss.audioguide.TrackingService.ACTION_WAKE";
	public static final String ACTION_START = "org.fruct.oss.audioguide.TrackingService.ACTION_START";

	public static final String ACTION_ACQUIRE_WAKE_LOCK = "org.fruct.oss.audioguide.TrackingService.ACTION_ACQUIRE_WAKE_LOCK";
	public static final String ACTION_RELEASE_WAKE_LOCK = "org.fruct.oss.audioguide.TrackingService.ACTION_RELEASE_WAKE_LOCK";

	private boolean isWakeMode = false;
	
	private DistanceTracker distanceTracker;
	private LocationReceiver locationReceiver;
	private AlarmManager alarmManager;
	private PendingIntent pendingIntent;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;

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
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(ACTION_WAKE)) {
				log.debug("ACTION_WAKE triggered");

				if (wakeLock != null && wakeLock.isHeld()) {
					wakeLock.acquire(30000);
				}
			} else if (action.equals(ACTION_ACQUIRE_WAKE_LOCK)) {
				acquireWakeLock();
				isWakeMode = true;
			} else if (action.equals(ACTION_RELEASE_WAKE_LOCK)) {
				releaseWakeLock();
				isWakeMode = false;
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		log.info("TrackingService onCreate");

		locationReceiver = new LocationReceiver(this);
		TrackManager trackManager = TrackManager.getInstance();

		distanceTracker = new DistanceTracker(trackManager, locationReceiver);
		distanceTracker.setRadius(50);
		distanceTracker.addListener(this);
		distanceTracker.start();

		locationReceiver.addListener(this);

		pref.registerOnSharedPreferenceChangeListener(this);
		//startLocationTrack();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);

		distanceTracker.stop();
		distanceTracker.removeListener(this);

		log.info("TrackingService onDestroy");
		releaseWakeLock();
	}

	private void acquireWakeLock() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!pref.getBoolean(SettingsActivity.PREF_WAKE, true)) {
			return;
		}

		// Schedule periodical service wake
		Intent wakeIntent = new Intent(ACTION_WAKE, null, this, TrackingService.class);
		pendingIntent = PendingIntent.getService(this, 0, wakeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, 10000, pendingIntent);

		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "audio-guide-wake-lock");
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire(30000);
	}

	private void releaseWakeLock() {
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
			pendingIntent = null;
		}

		if (powerManager != null) {
			wakeLock.release();
			log.debug("Wake lock state after release: {}", wakeLock.isHeld() ? "Still acquired!" : "Released");
			powerManager = null;
		}
	}

	private TrackingServiceBinder binder = new TrackingServiceBinder();

	@Override
	public void pointInRange(Point point) {
		Intent intent = new Intent(BC_ACTION_POINT_IN_RANGE);
		intent.putExtra(ARG_POINT, point);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
/*
		Intent ledIntent = new Intent(IlluminationIntent.ACTION_START_LED);
		ledIntent.putExtra(IlluminationIntent.EXTRA_LED_ID, IlluminationIntent.VALUE_BUTTON_RGB);
		ledIntent.putExtra(IlluminationIntent.EXTRA_LED_COLOR, 0xff00ff00);
		ledIntent.putExtra(IlluminationIntent.EXTRA_PACKAGE_NAME, "org.fruct.oss.audioguide");
		startService(ledIntent);*/
	}

	@Override
	public void pointOutRange(Point point) {
		Intent intent = new Intent(BC_ACTION_POINT_OUT_RANGE);
		intent.putExtra(ARG_POINT, point);

		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
/*
		Intent ledIntent = new Intent(IlluminationIntent.ACTION_STOP_LED);
		ledIntent.putExtra(IlluminationIntent.EXTRA_PACKAGE_NAME, "org.fruct.oss.audioguide");
		ledIntent.putExtra(IlluminationIntent.EXTRA_LED_ID, IlluminationIntent.VALUE_BUTTON_RGB);
		startService(ledIntent);*/
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
				location.setBearing((float) (Math.random() * 360));

				locationReceiver.mockLocation(location);
			}
		}, 2000);

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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		if (s.equals(SettingsActivity.PREF_RANGE)) {
			int newRange = sharedPreferences.getInt(s, 50);
			if (distanceTracker != null)
				distanceTracker.setRadius(newRange);
		} else if (s.equals(SettingsActivity.PREF_WAKE)) {
			if (sharedPreferences.getBoolean(s, true)) {
				if (isWakeMode) {
					acquireWakeLock();
				}
			} else {
				if (!isWakeMode) {
					releaseWakeLock();
				}
			}
		}
	}

	public class TrackingServiceBinder extends Binder {
		public TrackingService getService() {
			return TrackingService.this;
		}
	}
}
