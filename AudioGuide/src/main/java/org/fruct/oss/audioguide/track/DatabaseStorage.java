package org.fruct.oss.audioguide.track;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.BuildConfig;
import org.fruct.oss.audioguide.util.AUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseStorage implements ILocalStorage {
	private final static Logger log = LoggerFactory.getLogger(DatabaseStorage.class);

	public static final String DB_NAME = "tracksdb";
	public static final int DB_VERSION = 11; // published 11
	public static final String CREATE_TRACKS_SQL = "CREATE TABLE tracks " +
			"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
			"name TEXT," +
			"description TEXT," +
			"hname TEXT," +
			"url TEXT," +
			"active INTEGER," +
			"private INTEGER);";

	public static final String CREATE_POINTS_SQL = "CREATE TABLE points " +
			"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
			"name TEXT," +
			"description TEXT," +
			"lat INTEGER," +
			"lon INTEGER," +
			"audioUrl TEXT," +
			"photoUrl TEXT," +
			"trackId INTEGER," +
			"FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE);";

	public static final String[] SELECT_TRACK_COLUMNS = {
			"id", "name", "description", "url", "active", "hname", "private"
	};

	public static final String[] SELECT_POINT_COLUMNS = {
			"id", "name", "description", "lat", "lon", "audioUrl", "trackId", "photoUrl"
	};

	private final Context context;

	private TrackDatabaseHelper helper;
	private SQLiteDatabase db;

	private ArrayList<Track> tracks;

	private long freeTrackId;

	public DatabaseStorage(Context context) {
		this.context = context;
	}

	@Override
	public void storeLocalTrack(Track track) {
		ContentValues cv = new ContentValues(6);
		cv.put("name", track.getName());
		cv.put("description", track.getDescription());
		cv.put("url", track.getUrl());
		cv.put("active", track.isActive());
		cv.put("private", track.isPrivate());
		cv.put("hname", track.getHname());

		if (tracks.contains(track)) {
			tracks.remove(track);
			tracks.add(track);

			db.update("tracks", cv, "id=?", new String[] {String.valueOf(track.getLocalId())});
		} else {
			long localId = db.insert("tracks", null, cv);
			log.debug("Track {} inserted " + track.getName(), localId);
			track.setLocalId(localId);
			track.setLocal(true);
			tracks.add(track);
		}
	}

	@Override
	public void updatePoint(Track track, Point point) {
		ContentValues cv = new ContentValues(7);
		cv.put("name", point.getName());
		cv.put("description", point.getDescription());
		cv.put("lat", point.getLatE6());
		cv.put("lon", point.getLonE6());
		cv.put("audioUrl", point.getAudioUrl());
		cv.put("photoUrl", point.getPhotoUrl());

		long trackId = track != null ? track.getLocalId() : freeTrackId;
		cv.put("trackId", trackId);

		int count = db.update("points", cv, "points.trackId=? and points.name=?",
				new String[]{String.valueOf(trackId), point.getName()});
		if (count < 1) {
			db.insert("points", null, cv);
		}
	}

	private void storeLocalPoint(Track track, Point point) {
		ContentValues cv = new ContentValues(7);
		cv.put("name", point.getName());
		cv.put("description", point.getDescription());
		cv.put("lat", point.getLatE6());
		cv.put("lon", point.getLonE6());
		cv.put("audioUrl", point.getAudioUrl());
		cv.put("photoUrl", point.getPhotoUrl());
		cv.put("trackId", track.getLocalId());

		db.insert("points", null, cv);
	}

	@Override
	public void storeLocalPoints(Track track, List<Point> points) {
		db.beginTransaction();

		db.delete("points", "trackId=?", new String[] { String.valueOf(track.getLocalId()) });
		try {
			for (Point point : points)
				storeLocalPoint(track, point);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@SuppressLint("Assert")
	@Override
	public void initialize() {
		helper = new TrackDatabaseHelper(context);
		db = helper.getWritableDatabase();

		// Setup free (fake) track
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String freeTrackName = pref.getString("free-track-name", null);
		if (freeTrackName == null) {
			pref.edit().putString("free-track-name", freeTrackName = UUID.randomUUID().toString()).apply();
		}

		Cursor freeTrackCursor = db.query("tracks", new String[]{"id"},
				"name=?", new String[]{freeTrackName},
				null, null, null);

		assert freeTrackCursor.getCount() <= 1;

		if (freeTrackCursor.getCount() == 0) {
			ContentValues cv = new ContentValues();
			cv.put("name", freeTrackName);
			freeTrackId = db.insert("tracks", null, cv);
		} else {
			freeTrackCursor.moveToFirst();
			freeTrackId = freeTrackCursor.getInt(0);
		}

		log.info("Loose point's track name = {} and id = {}", freeTrackName, freeTrackId);
	}

	@Override
	public void close() {
		if (helper != null) {
			helper.close();
			helper = null;
			db = null;
			tracks = null;
		}
	}

	private void load() {
		Cursor cursor = db.query("tracks", SELECT_TRACK_COLUMNS, "id!=" + freeTrackId, null, null, null, null);

		tracks = new ArrayList<Track>(cursor.getCount());

		if (!cursor.moveToFirst())
			return;

		do {
			Track track = new Track(cursor.getString(1), cursor.getString(2), cursor.getString(3));
			track.setLocal(true);
			track.setLocalId(cursor.getLong(0));
			track.setActive(cursor.getInt(4) != 0);
			track.setPrivate(cursor.getInt(6) != 0);
			track.setHname(cursor.getString(5));
			tracks.add(track);
		} while (cursor.moveToNext());

		cursor.close();
	}

	@Override
	public void loadAsync(final Handler handler) {
		new Thread() {
			@Override
			public void run() {
				load();
				handler.sendMessage(new Message());
			}
		}.start();
	}

	@Override
	public List<Track> getTracks() {
		return tracks;
	}

	@Override
	public void loadPoints(Track track, Handler handler) {
		handler.sendMessage(new Message());
	}

	@Override
	public List<Point> getPoints(Track track) {
		ArrayList<Point> points = new ArrayList<Point>();

		if (!track.isLocal())
			throw new IllegalArgumentException("Non local track");

		Cursor cursor = db.query("points", SELECT_POINT_COLUMNS, "trackId=?",
				new String[] {String.valueOf(track.getLocalId())}, null, null, null);

		if (!cursor.moveToFirst())
			return points;

		do {
			Point point = new Point(cursor.getString(1), cursor.getString(2), cursor.getString(5), cursor.getString(7),
					cursor.getInt(3), cursor.getInt(4));
			points.add(point);
		} while (cursor.moveToNext());

		cursor.close();

		return points;
	}

	private static class TrackDatabaseHelper extends SQLiteOpenHelper {
		public TrackDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TRACKS_SQL);
			db.execSQL(CREATE_POINTS_SQL);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);

			if (!db.isReadOnly())
				db.execSQL("PRAGMA foreign_keys=ON;");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == newVersion) {
				return;
			}

			if (newVersion > 66666) {
				db.execSQL("drop table points;");
				db.execSQL("drop table tracks;");
			}

			onCreate(db);
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}
	}
}
