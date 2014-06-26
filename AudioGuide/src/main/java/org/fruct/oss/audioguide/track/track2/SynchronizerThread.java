package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.fruct.oss.audioguide.files.files2.DefaultFileManager;
import org.fruct.oss.audioguide.files.files2.FileManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SynchronizerThread extends HandlerThread {
	private static final Logger log = LoggerFactory.getLogger(SynchronizerThread.class);

	private final Database database;
	private final StorageBackend storageBackend;
	private Handler handler;

	SynchronizerThread(Database database, StorageBackend storageBackend) {
		super("Synchronizer thread");

		this.database = database;
		this.storageBackend = storageBackend;
	}

	void initializeHandler() {
		handler = new Handler(getLooper());
		scheduleStep();
	}

	private void scheduleStep() {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					doSynchronizationStep();
				} catch (InterruptedException e) {
					quitSafely();
				}
			}
		}, 1000);
	}

	private void doSynchronizationStep() throws InterruptedException {
		Cursor cursor = database.loadUpdatedTracks();
		while (cursor.moveToNext()) {
			Track track = new Track(cursor);

			Cursor cursor2 = database.loadPointsCursor(track);
			List<Point> points = new ArrayList<Point>();
			while (cursor2.moveToNext()) {
				Point point = new Point(cursor2);
				ensurePointFilesUploaded(point);
				points.add(point);
			}
			cursor2.close();

			storageBackend.updateTrack(track, points);
		}
		cursor.close();
		database.clearUpdates();
		scheduleStep();
	}

	private void ensurePointFilesUploaded(Point point) {
		Point oldPoint = new Point(point);

		if (point.hasAudio()) {
			Uri remoteAudioUri = ensurePointFileUploaded(Uri.parse(point.getAudioUrl()));
			if (remoteAudioUri != null) {
				point.setAudioUrl(remoteAudioUri.toString());
				database.insertPoint(point, oldPoint);
			}
		}

		if (point.hasPhoto()) {
			Uri remotePhotoUri = ensurePointFileUploaded(Uri.parse(point.getPhotoUrl()));
			if (remotePhotoUri != null) {
				point.setPhotoUrl(remotePhotoUri.toString());
				database.insertPoint(point, oldPoint);
			}
		}
	}

	private Uri ensurePointFileUploaded(Uri uri) {
		FileManager fm = DefaultFileManager.getInstance();
		if (!fm.isLocal(uri))
			return null;

		return fm.uploadLocalFile(uri);
	}
}
