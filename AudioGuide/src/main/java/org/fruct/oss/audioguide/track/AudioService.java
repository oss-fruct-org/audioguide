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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	private final static Logger log = LoggerFactory.getLogger(AudioService.class);

	private BroadcastReceiver inReceiver;
	private BroadcastReceiver outReceiver;

	private String currentAudioUrl;
	private MediaPlayer player;

	public AudioService() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.info("AudioService onCreate");

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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		log.info("AudioService onDestroy");

		if (player != null) {
			if (player.isPlaying()) {
				player.stop();
			}
		}

		LocalBroadcastManager.getInstance(this).unregisterReceiver(inReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(outReceiver);
	}

	private void startAudioTrack(String url) {
		if (currentAudioUrl != null) {
			log.info("Trying play another audio track while {} already playing", url);
			return;
		}

		currentAudioUrl = url;
		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			player.setDataSource(this, Uri.parse(url));
		} catch (IOException e) {
			log.warn("Cannot set data source for player with url = '{}'", url);
			return;
		}

		player.setOnCompletionListener(this);
		player.setOnPreparedListener(this);
		player.setOnErrorListener(this);
		player.prepareAsync();
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		log.info("Starting playing track {}", currentAudioUrl);
		mediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		mediaPlayer.release();
		currentAudioUrl = null;
		player = null;
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		log.warn("Player error with url" + currentAudioUrl + " " + what + " " + extra);
		return false;
	}

	private void stopAudioTrack(String url) {
		if (currentAudioUrl != null && currentAudioUrl.equals(url) && player.isPlaying()) {
			currentAudioUrl = null;
			player.stop();
		}
	}

	private void pointInRange(Point point) {
		log.debug("pointInRange");

		String audioUrl = point.getAudioUrl();
		if (audioUrl == null || audioUrl.isEmpty())
			return;

		startAudioTrack(audioUrl);
	}

	private void pointOutRange(Point point) {
		log.debug("pointOutRange");

		String audioUrl = point.getAudioUrl();
		if (audioUrl == null || audioUrl.isEmpty())
			return;

		stopAudioTrack(audioUrl);
	}

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
