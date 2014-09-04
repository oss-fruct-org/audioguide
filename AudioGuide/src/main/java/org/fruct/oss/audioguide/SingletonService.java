package org.fruct.oss.audioguide;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.fruct.oss.audioguide.files.DefaultFileManager;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonService extends Service {
	private static final Logger log = LoggerFactory.getLogger(SingletonService.class);

	public static final int STOP_DELAY = 10000;
	private TrackManager trackManager;
	private FileManager fileManager;

	public static final String ACTION_START = "org.fruct.oss.audioguide.SingletonService.START";
	public static final String ACTION_STOP = "org.fruct.oss.audioguide.SingletonService.STOP";

	private Handler handler;

	public SingletonService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.trace("onCreate");

		trackManager = DefaultTrackManager.getInstance();
		fileManager = DefaultFileManager.getInstance();

		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		log.trace("onDestroy");

		trackManager.close();
		fileManager.close();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return START_STICKY;

		if (intent.getAction() == null)
			return START_STICKY;

		if (intent.getAction().equals(ACTION_START)) {
			handler.removeCallbacks(stopper);
		} else if (intent.getAction().equals(ACTION_STOP)) {
			handler.postDelayed(stopper, STOP_DELAY);
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Runnable stopper = new Runnable() {
		@Override
		public void run() {
			stopSelf();
		}
	};
}
