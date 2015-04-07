package org.fruct.oss.audioguide;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.fruct.oss.audioguide.track.Database;
import org.fruct.oss.audioguide.track.LocationEvent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.gets.Category;
import org.fruct.oss.audioguide.track.tasks.CategoriesTask;
import org.fruct.oss.audioguide.track.tasks.PointsTask;
import org.fruct.oss.audioguide.track.tasks.TracksTask;
import org.fruct.oss.audioguide.util.EventReceiver;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;

public class SynchronizerService extends Service {
	public static long SYNC_DELTA_TIME = 3600 * 1000;

	private static final String PREF_LAST_LAT = "pref_last_sync_lat";
	private static final String PREF_LAST_LON = "pref_last_sync_lon";
	private static final String PREF_LAST_TIME = "pref_last_sync_time";

	public static final String ACTION_INIT = "org.fruct.oss.audioguide.SynchronizerService.ACTION_INIT";
	public static final String ACTION_SYNC_POINTS = "org.fruct.oss.audioguide.SynchronizerService.ACTION_SYNC_POINTS";
	public static final String ACTION_SYNC_TRACKS = "org.fruct.oss.audioguide.SynchronizerService.ACTION_SYNC_TRACKS";
	public static final String ACTION_CLEAN = "org.fruct.oss.audioguide.SynchronizerService.ACTION_CLEAN";
	public static final String ACTION_SYNC_BY_DISTANCE = "org.fruct.oss.audioguide.SynchronizerService.ACTION_SYNC_BY_DISTANCE";

	private SharedPreferences pref;
	private ExecutorService executor;

	private Database database;

	private int tasksCount;

	private boolean isPendingInitialization;

	private CategoriesTask categoriesTask;
	private PointsTask pointsTask;
	private TracksTask tracksTask;

	private Location location;

	private transient float[] out = new float[1];

	public SynchronizerService() {
	}

	private static Intent createStartIntent(Context context, String action) {
		return new Intent(action, null, context, SynchronizerService.class);
	}

	public static void startSyncByDistance(Context context) {
		Intent intent = createStartIntent(context, ACTION_SYNC_BY_DISTANCE);
		context.startService(intent);
	}

	public static void startInit(Context context) {
		Intent intent = createStartIntent(context, ACTION_INIT);
		context.startService(intent);
	}

	public static void startSyncPoints(Context context) {
		Intent intent = createStartIntent(context, ACTION_SYNC_POINTS);
		context.startService(intent);
	}

	public static void startSyncTracks(Context context) {
		Intent intent = createStartIntent(context, ACTION_SYNC_TRACKS);
		context.startService(intent);
	}

	public static void startClean(Context context) {
		Intent intent = createStartIntent(context, ACTION_CLEAN);
		context.startService(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		pref = PreferenceManager.getDefaultSharedPreferences(this);
		executor = Executors.newSingleThreadExecutor();
		database = App.getInstance().getDatabase();
		tasksCount = 0;

		EventBus.getDefault().registerSticky(this);
	}

	@Override
	public void onDestroy() {
		EventBus.getDefault().unregister(this);

		executor.shutdownNow();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return START_NOT_STICKY;
		}

		switch (intent.getAction()) {
		case ACTION_INIT:
			init();
			break;

		case ACTION_SYNC_POINTS:
			synchronizePoints();
			break;

		case ACTION_SYNC_TRACKS:
			synchronizeTracks();
			break;

		case ACTION_CLEAN:
			clean();
			break;

		case ACTION_SYNC_BY_DISTANCE:
			syncByDistance();
			break;
		}

		return START_NOT_STICKY;
	}

	private void syncByDistance() {
		if (location == null) {
			return;
		}

		long lastSyncTime = pref.getLong(PREF_LAST_TIME, 0);
		if (System.currentTimeMillis() - lastSyncTime > SYNC_DELTA_TIME) {
			synchronizeAll();
			return;
		}

		double loadRadiusM = pref.getFloat(SettingsActivity.PREF_LOAD_RADIUS, 500) * 1000;
		double lastSyncLat = pref.getFloat(PREF_LAST_LAT, 0);
		double lastSyncLon = pref.getFloat(PREF_LAST_LON, 0);
		Location.distanceBetween(location.getLatitude(), location.getLongitude(),
				lastSyncLat, lastSyncLon, out);

		if (out[0] > loadRadiusM) {
			synchronizeAll();
		}
	}

	private void clean() {
		if (location == null) {
			return;
		}

		final int radius = pref.getInt(SettingsActivity.PREF_LOAD_RADIUS, 500);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				database.cleanupPoints(location, radius * 1000);
			}
		});
	}

	@EventReceiver
	public void onEventMainThread(LocationEvent locationEvent) {
		location = locationEvent.getLocation();

		if (isPendingInitialization) {
			init();
		}
	}

	private void init() {
		if (!database.isFirstRun()) {
			return;
		}

		if (location == null) {
			isPendingInitialization = true;
			return;
		}

		isPendingInitialization = false;
		synchronizeAll();
	}

	private void synchronizeAll() {
		synchronizeCategories();
		synchronizeTracks();
		synchronizePoints();


	}

	private void synchronizePoints() {
		if (pointsTask != null) {
			pointsTask.cancel(true);
		}

		if (location == null) {
			return;
		}

		pointsTask = new PointsTask(location, this) {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				onTaskStarted();
			}

			@Override
			protected void onPostExecute(List<Point> points) {
				super.onPostExecute(points);
				onTaskEnded();
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				onTaskEnded();
			}
		};

		execute(pointsTask);
	}

	private void synchronizeTracks() {
		if (tracksTask != null) {
			tracksTask.cancel(true);
		}

		if (location == null) {
			return;
		}

		tracksTask = new TracksTask(location, this) {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				onTaskStarted();
			}

			@Override
			protected void onPostExecute(List<Track> tracks) {
				super.onPostExecute(tracks);
				onTaskEnded();
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				onTaskEnded();
			}
		};

		execute(tracksTask);
	}

	private void synchronizeCategories() {
		if (categoriesTask != null) {
			categoriesTask.cancel(true);
		}

		if (location == null) {
			return;
		}

		categoriesTask = new CategoriesTask() {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				onTaskStarted();
			}

			@Override
			protected void onPostExecute(List<Category> categories) {
				super.onPostExecute(categories);
				onTaskEnded();
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();
				onTaskEnded();
			}
		};

		execute(categoriesTask);
	}

	@SafeVarargs
	private final <Params, Progress, Result> void execute(AsyncTask<Params, Progress, Result> task,
														  Params... params) {
		if (Build.VERSION.SDK_INT > 11) {
			task.executeOnExecutor(executor, params);
		} else {
			task.execute(params);
		}
	}

	private void onTaskStarted() {
		if (tasksCount == 0) {
			startForeground(4, buildNotification());
		}

		tasksCount++;
	}

	private void onTaskEnded() {
		tasksCount--;

		if (tasksCount == 0) {
			stopForeground(true);
		}

		pref.edit().putLong(PREF_LAST_TIME, System.currentTimeMillis())
				.putFloat(PREF_LAST_LAT, (float) location.getLatitude())
				.putFloat(PREF_LAST_LON, (float) location.getLongitude())
				.apply();
	}

	private Notification buildNotification() {
		Intent intent = new Intent(this, getClass());
		PendingIntent contentIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_action_refresh)
				.setOngoing(true)
				.setContentTitle("Synchronizing data")
				.setContentText("AudioGuide synchronizes data...")
				.setContentIntent(contentIntent);
		return builder.build();
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
