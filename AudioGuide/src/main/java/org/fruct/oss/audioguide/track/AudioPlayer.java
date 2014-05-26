package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.fruct.oss.audioguide.files.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AudioPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	public static String BC_ACTION_START_PLAY = "org.fruct.oss.audioguide.AudioPlayer.START_PLAY";
	public static String BC_ACTION_STOP_PLAY = "org.fruct.oss.audioguide.AudioPlayer.STOP_PLAY";

	private final static Logger log = LoggerFactory.getLogger(AudioPlayer.class);
	private final Context context;

	private MediaPlayer player;
	private Uri currentUri;

	private FileManager fileManager;

	public AudioPlayer(Context context) {
		this.context = context;

		fileManager = FileManager.getInstance();
	}

	public void startAudioTrack(Uri uri) {
		if (player != null) {
			return;
		}

		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			// Try to use cached uri
			Uri uriToPlay = fileManager.getAudioUri(uri);
			player.setDataSource(context, uriToPlay);
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

	public void stopAudioTrack() {
		if (player != null) {
			player.stop();
			player = null;
			currentUri = null;
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
		}
	}

	public boolean isPlaying(Uri uri) {
		return player != null && (uri == null || uri.equals(currentUri));
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		log.trace("Playing uri {}", currentUri);
		mediaPlayer.start();

		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_START_PLAY));
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		if (player != null) {
			player.release();
			player = null;
			currentUri = null;
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BC_ACTION_STOP_PLAY));
		}
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
		log.warn("Player error with uri " + currentUri + " " + what + " " + extra);

		player = null;
		currentUri = null;

		return false;
	}
}
