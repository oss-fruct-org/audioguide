package org.fruct.oss.audioguide.track;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.MainActivity;
import org.fruct.oss.audioguide.R;
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

	public static final String ACTION_START_TRACKING = "org.fruct.oss.audioguide.TrackingService.ACTION_START_TRACKING";
	public static final String ACTION_STOP_TRACKING = "org.fruct.oss.audioguide.TrackingService.ACTION_STOP_TRACKING";

	public static final String ACTION_PLAY = "org.fruct.oss.audioguide.TrackingService.ACTION_PLAY";
	public static final String ACTION_STOP = "org.fruct.oss.audioguide.TrackingService.ACTION_STOP";

	public static final String ACTION_BACKGROUND = "org.fruct.oss.audioguide.TrackingService.ACTION_BACKGROUND";
	public static final String ACTION_FOREGROUND = "org.fruct.oss.audioguide.TrackingService.ACTION_FOREGROUND";

	public static final String PREF_IS_TRACKING_MODE = "pref-tracking-service-is-tracking-mode";
	public static final String PREF_IS_BACKGROUND_MODE = "pref-tracking-service-is-background-mode";

	private static int NOTIFICATION_ID = 0;

	private boolean isTrackingMode = false;
	private boolean isBackgroundMode = false;

	private SharedPreferences pref;

	private DistanceTracker distanceTracker;
	private LocationReceiver locationReceiver;
	private AlarmManager alarmManager;
	private PendingIntent pendingIntent;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private AudioPlayer audioPlayer;

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
		if (intent == null) {
			return START_STICKY;
		}

		String action = intent.getAction();
		if (action != null) {
			if (action.equals(ACTION_WAKE)) {
				log.debug("ACTION_WAKE triggered");

				// This normally shouldn't be executed, only after application crash
				if (!isTrackingMode) {
					log.debug("Disabling wake alarm");
					alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					alarmManager.cancel(createWakePendingIntent());

					if (wakeLock != null && wakeLock.isHeld()) {
						wakeLock.release();
					}

					stopSelf();
				}

				if (wakeLock != null && wakeLock.isHeld()) {
					wakeLock.acquire(30000);
				}
			} else if (action.equals(ACTION_PLAY)) {
				audioPlayer.startAudioTrack(intent.getData());
			} else if (action.equals(ACTION_STOP)) {
				audioPlayer.stopAudioTrack();
			} else if (action.equals(ACTION_START_TRACKING)) {
				startTracking();
			} else if (action.equals(ACTION_STOP_TRACKING)) {
				stopTracking();
			} else if (action.equals(ACTION_BACKGROUND)) {
				goBackground();
			} else if (action.equals(ACTION_FOREGROUND)) {
				goForeground();
			}
		}

		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		pref = PreferenceManager.getDefaultSharedPreferences(this);

		isTrackingMode = pref.getBoolean(PREF_IS_TRACKING_MODE, false);
		isBackgroundMode = pref.getBoolean(PREF_IS_BACKGROUND_MODE, false);

		log.info("TrackingService onCreate");

		audioPlayer = new AudioPlayer(this);

		locationReceiver = new LocationReceiver(this);
		TrackManager trackManager = TrackManager.getInstance();

		distanceTracker = new DistanceTracker(trackManager, locationReceiver);

		distanceTracker.setRadius(pref.getInt(SettingsActivity.PREF_RANGE, 50));
		distanceTracker.addListener(this);
		distanceTracker.start();

		locationReceiver.addListener(this);

		pref.registerOnSharedPreferenceChangeListener(this);
		//startLocationTrack();
	}

	private void startTracking() {
		pref.edit().putBoolean(PREF_IS_TRACKING_MODE, true).apply();
		isTrackingMode = true;
		acquireWakeLock();
	}

	private void stopTracking() {
		pref.edit().putBoolean(PREF_IS_TRACKING_MODE, false).apply();
		isTrackingMode = false;

		audioPlayer.stopAudioTrack();

		releaseWakeLock();

		if (isBackgroundMode) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.cancel(NOTIFICATION_ID);

			stopSelf();
		}
	}

	private void goBackground() {
		if (!pref.getBoolean(SettingsActivity.PREF_WAKE, true) || !isTrackingMode) {
			stopSelf();
			log.debug("TrackingService stopSelf");
		} else {
			log.debug("TrackingService goBackground");
			isBackgroundMode = true;
			pref.edit().putBoolean(PREF_IS_BACKGROUND_MODE, true).apply();

			showNotification(null);
		}
	}

	private void goForeground() {
		log.debug("TrackingService goForeground");
		isBackgroundMode = false;
		pref.edit().putBoolean(PREF_IS_BACKGROUND_MODE, false).apply();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(NOTIFICATION_ID);
	}

	private void showNotification(String text) {
		if (!isBackgroundMode)
			return;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Audio Guide")
				.setContentText(text != null ? text : "Audio Guide in background. Click to open")
				.setOngoing(true);

		// Open app action
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		// Stop tracking action
		Intent stopIntent = new Intent(ACTION_STOP_TRACKING, null, this, TrackingService.class);
		PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(R.drawable.ic_action_volume_muted, "Stop", stopPendingIntent);

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		distanceTracker.stop();
		distanceTracker.removeListener(this);
		audioPlayer.stopAudioTrack();

		log.info("TrackingService onDestroy");
		releaseWakeLock();
	}

	private void acquireWakeLock() {
		if (!pref.getBoolean(SettingsActivity.PREF_WAKE, true)) {
			return;
		}

		// Schedule periodical service wake
		if (pendingIntent == null) {
			pendingIntent = createWakePendingIntent();
			alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, 10000, pendingIntent);
		}

		if (powerManager == null) {
			powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "audio-guide-wake-lock");
			wakeLock.setReferenceCounted(false);
			wakeLock.acquire(30000);
		}

	}

	private PendingIntent createWakePendingIntent() {
		Intent wakeIntent = new Intent(ACTION_WAKE, null, this, TrackingService.class);
		return PendingIntent.getService(this, 0, wakeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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

		if (isTrackingMode && point.hasAudio()) {
			audioPlayer.startAudioTrack(Uri.parse(point.getAudioUrl()));
		}

		showNotification(point.getName());
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

		if (point.hasAudio() && audioPlayer.isPlaying(Uri.parse(point.getAudioUrl()))) {
			audioPlayer.stopAudioTrack();
		}

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
				if (isTrackingMode) {
					acquireWakeLock();
				}
			} else {
				if (isTrackingMode) {
					releaseWakeLock();
				}
			}
		}
	}

	public boolean isTrackingActive() {
		return isTrackingMode;
	}

	public class TrackingServiceBinder extends Binder {
		public TrackingService getService() {
			return TrackingService.this;
		}
	}
}
