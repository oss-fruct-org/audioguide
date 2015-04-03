package org.fruct.oss.audioguide.files2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.IoUtils;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.events.AudioDownloadFinished;
import org.fruct.oss.audioguide.track.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

public class AudioDownloadService extends Service {
	private static final int MAX_THREADS = 3;

	private static final Logger log = LoggerFactory.getLogger(AudioDownloadService.class);

	public final static String ACTION_DOWNLOAD = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD";
	public final static String ARG_POINTS = "org.fruct.oss.audioguide.AudioDownloadService.ACTION_DOWNLOAD.ARG_POINTS";

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
			List<Point> points = intent.getParcelableArrayListExtra(ARG_POINTS);
			addDownloadPoints(points);
			scheduleTask();
			break;
		}

		return START_NOT_STICKY;
	}

	private void scheduleTask() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!queue.isEmpty()) {
					processOneUrl();
				} else {
					stopForeground(true);
				}
			}
		});

	}

	private void processOneUrl() {
		synchronized (executorService) {
			if (workersCount == MAX_THREADS) {
				return;
			}

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

	private void downloadFile(String fileUrl) throws IOException {
		log.info("Downloading audio url " + fileUrl);

		URL url = new URL(fileUrl);

		HttpURLConnection conn = null;
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

		InputStream inputStream = conn.getInputStream();
		boolean result = cache.save(fileUrl, inputStream, new IoUtils.CopyListener() {
			@Override
			public boolean onBytesCopied(int current, int total) {
				return true;
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

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
