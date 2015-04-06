package org.fruct.oss.audioguide;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

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
	public static final String ACTION_INIT = "org.fruct.oss.audioguide.SynchronizerService.ACTION_INIT";
	public static final String ACTION_SYNC_POINTS = "org.fruct.oss.audioguide.SynchronizerService.ACTION_SYNC_POINTS";
	public static final String ACTION_SYNC_TRACKS = "org.fruct.oss.audioguide.SynchronizerService.ACTION_SYNC_TRACKS";

	private ExecutorService executor;

	private Database database;

	private int tasksCount;

	private boolean isPendingInitialization;

	private CategoriesTask categoriesTask;
	private PointsTask pointsTask;
	private TracksTask tracksTask;

	private Location location;

	public SynchronizerService() {
	}

	private static Intent createStartIntent(Context context, String action) {
		return new Intent(action, null, context, SynchronizerService.class);
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

	@Override
	public void onCreate() {
		super.onCreate();

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
		}

		return super.onStartCommand(intent, flags, startId);
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
