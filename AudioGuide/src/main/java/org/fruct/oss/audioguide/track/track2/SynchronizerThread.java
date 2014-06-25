package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;

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
				points.add(new Point(cursor2));
			}
			cursor2.close();

			storageBackend.updateTrack(track, points);
		}
		cursor.close();
		database.clearUpdates();
		scheduleStep();
	}
}
