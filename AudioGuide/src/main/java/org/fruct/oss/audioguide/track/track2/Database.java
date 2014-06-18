package org.fruct.oss.audioguide.track.track2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class Database {
	private boolean isClosed;

	private final SQLiteDatabase db;
	private final Helper helper;

	public static final String[] SELECT_POINTS_COLUMNS = { "name", "description", "lat", "lon", "audioUrl", "photoUrl" };
	public static final String[] SELECT_TRACKS_COLUMNS = { "name", "description", "url", "hname", "active" };

	public static final String[] SEARCH_POINT_COLUMNS = { "name", "description", "lat", "lon" };
	public static final String[] ID_COLUMNS = {"id"};

	public Database(Context context) {
		helper = new Helper(context);
		db = helper.getWritableDatabase();
	}

	public void close() {
		if (!isClosed) {
			helper.close();
			isClosed = true;
		}
	}

	public long insertPoint(Point point) {
		if (findPointId(point) != -1)
			return -1;

		ContentValues cv = new ContentValues(7);
		cv.put("name", point.getName());
		cv.put("description", point.getDescription());
		cv.put("lat", point.getLatE6());
		cv.put("lon", point.getLonE6());

		if (point.hasAudio())
			cv.put("audioUrl", point.getAudioUrl());
		if (point.hasPhoto())
			cv.put("photoUrl", point.getPhotoUrl());
		if (point.getCategoryId() != -1)
			cv.put("categoryId", point.getCategoryId());

		return db.insert("point", null, cv);
	}

	public long insertTrack(Track track) {
		ContentValues cv = new ContentValues();
		cv.put("name", track.getName());
		cv.put("description", track.getDescription());
		cv.put("hname", track.getHname());
		cv.put("url", track.getUrl());
		cv.put("active", track.isActive());

		return db.insert("track", null, cv);
	}

	public void insertToTrack(Track track, Point point) {
		long pointId = findPointId(point);
		if (pointId == -1)
			pointId = insertPoint(point);

		long trackId = findTrackId(track);
		if (trackId == -1)
			trackId = insertTrack(track);

		db.execSQL("INSERT INTO tp VALUES (?, ?, (SELECT (IFNULL (MAX(idx), 0) + 1) FROM tp));", Utils.toArray(trackId, pointId));
	}

	public List<Track> loadTracks() {
		List<Track> tracks = new ArrayList<Track>();

		Cursor cursor = db.query("track", SELECT_TRACKS_COLUMNS, null, null, null, null, null);

		while (cursor.moveToNext()) {
			Track track = new Track(cursor.getString(0), cursor.getString(1), cursor.getString(2));
			tracks.add(track);
		}

		cursor.close();

		return tracks;
	}

	public List<Point> loadPoints(Track track) {
		List<Point> points = new ArrayList<Point>();

		Cursor cursor = db.rawQuery("SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon " +
				"FROM point INNER JOIN tp " +
				"ON tp.pointId=point.id " +
				"WHERE tp.trackId IN (SELECT track.id FROM track WHERE track.name=?) " +
				"ORDER BY tp.idx;", Utils.toArray(track.getName()));


		while (cursor.moveToNext()) {
			Point point = new Point(cursor.getString(0), cursor.getString(1), cursor.getString(2),
					cursor.getString(3), cursor.getInt(4), cursor.getInt(5));
			points.add(point);
		}

		cursor.close();

		return points;
	}

	private long findPointId(Point point) {
		Cursor cursor = db.query("point", ID_COLUMNS, "name=? and description=? and lat=? and lon=?",
				Utils.toArray(point.getName(), point.getDescription(), point.getLatE6(), point.getLonE6()), null, null, null);

		try {
			if (!cursor.moveToFirst())
				return -1;
			else
				return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	private long findTrackId(Track track) {
		Cursor cursor = db.query("point", ID_COLUMNS, "name=?",	Utils.toArray(track.getName()), null, null, null);

		try {
			if (!cursor.moveToFirst())
				return -1;
			else
				return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}


	private static class Helper extends SQLiteOpenHelper {
		public static final String DB_NAME = "tracksdb2";
		public static final int DB_VERSION = 1; // published None

		public static final String CREATE_TRACKS_SQL = "CREATE TABLE track " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name TEXT," +
				"description TEXT," +
				"hname TEXT," +
				"url TEXT," +
				"active INTEGER);";

		public static final String CREATE_POINTS_SQL = "CREATE TABLE point " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name TEXT," +
				"description TEXT," +
				"lat INTEGER," +
				"lon INTEGER," +
				"audioUrl TEXT," +
				"photoUrl TEXT," +
				"categoryId INTEGER," +

				"FOREIGN KEY(categoryId) REFERENCES category(id));";

		public static final String CREATE_TRACK_POINTS_SQL = "CREATE TABLE tp " +
				"(trackId INTEGER, pointId INTEGER, idx INTEGER," +
				"FOREIGN KEY(trackId) REFERENCES track(id) ON DELETE CASCADE," +
				"FOREIGN KEY(pointId) REFERENCES point(id) ON DELETE CASCADE);";

		public static final String CREATE_CATEGORIES_SQL = "CREATE TABLE category " +
				"(id INTEGER PRIMARY KEY," +
				"name TEXT," +
				"description TEXT," +
				"url TEXT," +
				"state INTEGER);"; // 1 - active, 0 - non-active

		public Helper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_CATEGORIES_SQL);
			db.execSQL(CREATE_POINTS_SQL);
			db.execSQL(CREATE_TRACKS_SQL);
			db.execSQL(CREATE_TRACK_POINTS_SQL);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);

			if (!db.isReadOnly()) {
				db.execSQL("PRAGMA foreign_keys = ON;");
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}
}

