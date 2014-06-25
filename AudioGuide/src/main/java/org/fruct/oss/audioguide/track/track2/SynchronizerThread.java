package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.track2.Database;
import org.fruct.oss.audioguide.track.track2.StorageBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SynchronizerThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(SynchronizerThread.class);

	private final Database database;
	private final StorageBackend storageBackend;

	SynchronizerThread(Database database, StorageBackend storageBackend) {
		this.database = database;
		this.storageBackend = storageBackend;
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				doSynchronizationStep();
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	private void doSynchronizationStep() throws InterruptedException {
		Thread.sleep(1000);

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
	}
}
