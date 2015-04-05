package org.fruct.oss.audioguide.files2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.events.AudioDownloadFinished;
import org.fruct.oss.audioguide.events.AudioDownloadProgress;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.IntervalCopyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;

public class AudioDownloadService extends Service {
	private static final int MAX_THREADS = 1;

	private static final Logger log = LoggerFactory.getLogger(AudioDownloadService.class);

	public final static String ACTION_KEEP_PERSISTENT = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_KEEP_PERSISTENT";

	public final static String ACTION_DOWNLOAD = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD";
	public final static String ARG_POINTS = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD.ARG_POINTS";
	public final static String ARG_URLS = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD.ARG_URLS";
	public final static String ARG_POINT = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD.ARG_POINT";

	private final Deque<String> queue = new ArrayDeque<>();

	private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);
	private final Handler handler = new Handler(Looper.getMainLooper());
	private int workersCount = 0;

	private DiskCache cache;

	public AudioDownloadService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();

		cache = ImageLoader.getInstance().getDiskCache();
		workersCount = 0;
	}

	@Override
	public void onDestroy() {
		executorService.shutdownNow();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return START_NOT_STICKY;
		}

		switch (intent.getAction()) {
		case ACTION_DOWNLOAD:
			if (intent.getExtras().containsKey(ARG_POINTS)) {
				List<Point> points = intent.getParcelableArrayListExtra(ARG_POINTS);
				addDownloadPoints(points);
			} else if (intent.getExtras().containsKey(ARG_POINT)) {
				Point point = intent.getParcelableExtra(ARG_POINT);
				addDownloadPoint(point);
			}

			scheduleTask();
			break;

		case ACTION_KEEP_PERSISTENT:
			if (intent.getExtras().containsKey(ARG_URLS)) {
				List<String> urls = intent.getStringArrayListExtra(ARG_URLS);

				addDownloadUrls(urls);
				ensurePersistent(urls);

				scheduleTask();
			}
			break;
		}

		return START_NOT_STICKY;
	}

	private void ensurePersistent(final List<String> urls) {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				App.getInstance().getCache().setPersistentUrls(urls);
			}
		});
	}


	private void scheduleTask() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!queue.isEmpty()) {
					while(processQueue())
						;
				} else {
					stopForeground(true);
				}
			}
		});

	}

	private boolean processQueue() {
		synchronized (executorService) {
			if (workersCount == MAX_THREADS) {
				return false;
			}

			if (queue.isEmpty())
				return false;

			if (workersCount == 0) {
				startForeground(2, buildNotification());
			}

			workersCount++;
		}

		final String url = queue.removeLast();

		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					asyncProcessOneUrl(url);
				} finally {
					synchronized (executorService) {
						workersCount--;
					}
					scheduleTask();
				}
			}
		});

		return true;
	}

	private Notification buildNotification() {
		Intent intent = new Intent(this, getClass());
		PendingIntent contentIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher)
				.setOngoing(true)
				.setContentTitle("Download audio")
				.setContentText("AudioGuide downloading audio...")
				.setContentIntent(contentIntent);
		return builder.build();
	}

	private void asyncProcessOneUrl(String url) {
		// Check url already loaded
		File existingFile = cache.get(url);
		if (existingFile == null || !existingFile.exists()) {
			try {
				downloadFile(url);
			} catch (IOException e) {
				// Download failed but don't return url to queue.
			}
		} else {
			log.debug("Skipping audio download for url " + url);
		}
	}

	private void downloadFile(final String fileUrl) throws IOException {
		log.info("Downloading audio url " + fileUrl);

		URL url = new URL(fileUrl);

		HttpURLConnection conn;
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoInput(true);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(15000);
		conn.setRequestMethod("GET");
		conn.connect();

		int code = conn.getResponseCode();
		if (code != 200) {
			throw new IOException("Server returned " + code);
		}

		InputStream inputStream = new ContentLengthInputStream(conn.getInputStream(), conn.getContentLength());
		boolean result = cache.save(fileUrl, inputStream, new IntervalCopyListener(100000) {
			@Override
			public void onProgress(int current, int total) {
				log.debug("Progress " + fileUrl + " " + current + " " + total);
				// TODO: check if EventBus can process same event every time, and move this event to field
				EventBus.getDefault().post(new AudioDownloadProgress(fileUrl, total, current));
			}
		});

		if (result) {
			EventBus.getDefault().post(new AudioDownloadFinished(fileUrl));
		} else {
			throw new IOException("Something went wrong while saving file to cache");
		}
	}

	private void addDownloadPoints(List<Point> points) {
		for (Point point : points) {
			addDownloadPoint(point);
		}
	}

	private void addDownloadPoint(Point point) {
		if (point.hasAudio()) {
			while(queue.remove(point.getAudioUrl()))
				;

			queue.add(point.getAudioUrl());
		}
	}

	private void addDownloadUrls(List<String> urls) {
		for (String url : urls) {
				while(queue.remove(url))
					;

				queue.add(url);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
