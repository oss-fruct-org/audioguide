package org.fruct.oss.audioguide.track;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.fruct.oss.audioguide.files.DefaultFileManager;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
					doSynchronizationStepPoints();
					doSynchronizationStepTracks();
					scheduleStep();
				} catch (InterruptedException e) {
					quitSafely();
				} catch (Exception ex) {
					log.error("Synchronization error: ", ex);
					scheduleStep();
				}
			}
		}, 5000);
	}

	private void doSynchronizationStepTracks() throws InterruptedException, IOException, GetsException {
		log.info("Synchronizing tracks");

		// TODO: cursors non exception-safe
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

		database.clearTrackUpdates();
	}

	private void doSynchronizationStepPoints() throws IOException, GetsException, InterruptedException {
		log.info("Synchronizing points");

		Cursor cursor = database.loadUpdatedPoints();
		try {
			while (cursor.moveToNext()) {
				Point point = new Point(cursor);
				ensurePointFilesUploaded(point);

				if (point.getTime() == null) {
					// Insert new point
					point.createTime();
					storageBackend.insertPoint(point.getCategoryId(), point);
				} else {
					storageBackend.updatePoint(point);
				}

				database.insertPoint(point);
				database.clearPointUpdate(cursor.getLong(cursor.getColumnIndex("_id")));
			}
		} finally  {
			cursor.close();
		}

	}

	private void ensurePointFilesUploaded(Point point) throws IOException, GetsException {
		if (point.hasAudio()) {
			Uri remoteAudioUri = ensurePointFileUploaded(Uri.parse(point.getAudioUrl()));
			if (remoteAudioUri != null) {
				point.setAudioUrl(remoteAudioUri.toString());
				database.insertPoint(point);
			}
		}

		if (point.hasPhoto()) {
			Uri remotePhotoUri = ensurePointFileUploaded(Uri.parse(point.getPhotoUrl()));
			if (remotePhotoUri != null) {
				point.setPhotoUrl(remotePhotoUri.toString());
				database.insertPoint(point);
			}
		}
	}

	private Uri ensurePointFileUploaded(Uri uri) throws IOException, GetsException {
		FileManager fm = DefaultFileManager.getInstance();
		if (!fm.isLocal(uri))
			return null;

		return fm.uploadLocalFile(uri);
	}
}
