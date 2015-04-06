package org.fruct.oss.audioguide;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingletonService extends Service {
	private static final Logger log = LoggerFactory.getLogger(SingletonService.class);

	public static final int STOP_DELAY = 10000;
	private Handler handler;

	public SingletonService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.trace("onCreate");


		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		log.trace("onDestroy");

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log.trace("onStartCommand {} {}", intent, flags);
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		log.trace("onUnbind");
		handler.postDelayed(stopRunnable, STOP_DELAY);
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		log.trace("onRebind");

		handler.removeCallbacks(stopRunnable);
	}

	private Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			log.trace("stopSelf");
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
