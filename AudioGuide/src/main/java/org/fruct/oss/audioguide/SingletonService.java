package org.fruct.oss.audioguide;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		handler.postDelayed(stopRunnable, STOP_DELAY);
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		handler.removeCallbacks(stopRunnable);
	}

	private Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			stopSelf();
		}
	};

	private Binder binder = new Binder();
	private class Binder extends android.os.Binder {
		public SingletonService getService() {
			return SingletonService.this;
		}
	}
}
