package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	public static String BC_ACTION_START_PLAY = "org.fruct.oss.audioguide.AudioPlayer.START_PLAY";
	public static String BC_ACTION_STOP_PLAY = "org.fruct.oss.audioguide.AudioPlayer.STOP_PLAY";
	public static String BC_ACTION_POSITION = "org.fruct.oss.audioguide.AudioPlayer.POSITION";

	private final static Logger log = LoggerFactory.getLogger(AudioPlayer.class);
	private final Context context;

	private final MediaPlayer player;

	private FileInputStream currentInputStream;
	private Uri currentUri;
	private Point currentPoint;

	private DiskCache diskCache;

	AudioPlayer(Context context) {
		this.context = context;

		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		diskCache = ImageLoader.getInstance().getDiskCache();
	}

	public void close() {
		player.release();
	}

	public void startAudioTrack(Point point) {
		if (currentInputStream != null || !point.hasAudio()) {
			return;
		}

		Uri uri = Uri.parse(point.getAudioUrl());
		FileInputStream inputStream;

		try {
			// Try to use cached uri
			File localFile = diskCache.get(point.getAudioUrl());
			if (localFile != null && localFile.exists()) {
				inputStream = new FileInputStream(localFile);
				player.setDataSource(inputStream.getFD());
			} else {
				return;
			}
		} catch (IOException e) {
			log.warn("Cannot set data source for player with url = '{}'", uri);
			return;
		}

		currentInputStream = inputStream;
		currentUri = uri;
		currentPoint = point;
		player.setOnCompletionListener(this);
		player.setOnPreparedListener(this);
		player.setOnErrorListener(this);
		player.prepareAsync();
	}

	public void stopAudioTrack() {
		if (currentInputStream != null) {
			player.stop();
			player.reset();

			Utils.sclose(currentInputStream);
			currentUri = null;
			currentInputStream = null;
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
		}
	}

	public boolean isPlaying(Uri uri) {
		return (uri == null || uri.equals(currentUri));
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		log.trace("Playing uri {}", currentUri);
		mediaPlayer.start();

		Intent intent = new Intent(BC_ACTION_START_PLAY);
		intent.putExtra("duration", mediaPlayer.getDuration());
		intent.putExtra("point", currentPoint);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

		handlerInputStream = currentInputStream;
		handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(positionUpdater, 1000);
	}

	private FileInputStream handlerInputStream;
	private Handler handler;
	private Runnable positionUpdater = new Runnable() {
		@Override
		public void run() {
			if (currentInputStream != handlerInputStream) {
				return;
			}

			Intent intent = new Intent(BC_ACTION_POSITION);
			intent.putExtra("position", player.getCurrentPosition());
			intent.putExtra("point", currentPoint);

			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
			handler.postDelayed(positionUpdater, 1000);
		}
	};

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		player.reset();

		Utils.sclose(currentInputStream);
		currentUri = null;
		currentInputStream = null;
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		log.warn("Player error with uri " + currentUri + " " + what + " " + extra);

		player.reset();
		Utils.sclose(currentInputStream);
		currentInputStream = null;
		currentUri = null;

		return false;
	}

	public void pause() {
		if (player.isPlaying())
			player.pause();
	}

	public void unpause() {
		if (currentInputStream != null)
			player.start();
	}

	public void seek(int position) {
		if (currentInputStream != null)
			player.seekTo(position);
	}
}
