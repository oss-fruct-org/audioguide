package org.fruct.oss.audioguide.track;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.BuildConfig;
import org.fruct.oss.audioguide.util.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	private final static Logger log = LoggerFactory.getLogger(AudioService.class);

	public static final String ACTION_PLAY = "org.fruct.oss.audioguide.ACTION_PLAY";
	public static final String ACTION_STOP = "org.fruct.oss.audioguide.ACTION_STOP";
	public static final String ACTION_CACHE = "org.fruct.oss.audioguide.ACTION_CACHE";

	public static final String ACTION_WATCH_POINTS = "org.fruct.oss.audioguide.ACTION_WATCH_POINTS";
	public static final String ACTION_SEND_STATE = "org.fruct.oss.audioguide.ACTION_SEND_STATE";

	public static final String BC_START_WATCH_POINTS = "org.fruct.oss.audioguide.BC_START_WATCH_POINTS";
	public static final String BC_STOP_SERVICE = "org.fruct.oss.audioguide.BC_STOP_SERVICE";

	private Downloader downloader;

	private BroadcastReceiver inReceiver;
	private BroadcastReceiver outReceiver;

	private MediaPlayer player;
	private Uri currentUri;

	public AudioService() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log.info("AudioService onStartCommand");
		if (intent == null || intent.getAction() == null)
			return START_STICKY;

		String action = intent.getAction();
		if (action.equals(ACTION_PLAY)) {
			Uri uri = intent.getData();
			startAudioTrack(uri);
		} else if (action.equals(ACTION_STOP)) {
			Uri uri = intent.getData();
			stopAudioTrack(uri);
		} else if (action.equals(ACTION_WATCH_POINTS)) {
			watchPoints();
		} else if (action.equals(ACTION_SEND_STATE)) {
			sendState();
		} else if (action.equals(ACTION_CACHE)) {
			Uri uri = intent.getData();
			downloader.insertUri(uri);
		}

		return START_STICKY;
	}

	private void sendState() {
		if (inReceiver != null) {
			assert outReceiver != null;

			LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BC_START_WATCH_POINTS));
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.info("AudioService onCreate");

		downloader = new Downloader(this, "audio-service-stored");
	}

	private void watchPoints() {
		inReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				pointInRange(TrackingService.getPointFromIntent(intent));
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(inReceiver, new IntentFilter(TrackingService.BC_ACTION_POINT_IN_RANGE));

		outReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				pointOutRange(TrackingService.getPointFromIntent(intent));
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(outReceiver, new IntentFilter(TrackingService.BC_ACTION_POINT_OUT_RANGE));

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BC_START_WATCH_POINTS));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		log.info("AudioService onDestroy");

		if (player != null) {
			player.stop();
		}

		LocalBroadcastManager.getInstance(this).unregisterReceiver(inReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(outReceiver);

		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BC_STOP_SERVICE));

		downloader = null;
	}

	private void startAudioTrack(Uri uri) {
		if (player != null && player.isPlaying()) {
			player.stop();
			player = null;
			currentUri = null;
		}

		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			// Try to use cached uri
			uri = downloader.getUri(uri);
			player.setDataSource(this, uri);
		} catch (IOException e) {
			log.warn("Cannot set data source for player with url = '{}'", uri);
			return;
		}

		currentUri = uri;
		player.setOnCompletionListener(this);
		player.setOnPreparedListener(this);
		player.setOnErrorListener(this);
		player.prepareAsync();
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		log.trace("Playing uri {}", currentUri);
		mediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		assert player == mediaPlayer;
		player.release();
		player = null;
		currentUri = null;
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		log.warn("Player error with uri" + currentUri + " " + what + " " + extra);

		player = null;
		currentUri = null;

		return false;
	}

	private void stopAudioTrack(Uri uri) {
		if (currentUri != null && (uri == null || currentUri.equals(uri)) && player.isPlaying()) {
			player.stop();
		}
	}

	private void pointInRange(Point point) {
		log.debug("pointInRange");

		String audioUrl = point.getAudioUrl();
		if (audioUrl == null || audioUrl.isEmpty())
			return;

		startAudioTrack(Uri.parse(audioUrl));
	}

	private void pointOutRange(Point point) {
		log.debug("pointOutRange");

		String audioUrl = point.getAudioUrl();
		if (audioUrl == null || audioUrl.isEmpty())
			return;

		stopAudioTrack(Uri.parse(audioUrl));
	}

	@Deprecated
	public static boolean isRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (AudioService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
