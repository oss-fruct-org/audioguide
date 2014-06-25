package org.fruct.oss.audioguide.track.track2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fruct.oss.audioguide.gets.Category;
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
	public static final String[] SELECT_CATEGORIES_COLUMNS = {
			"id", "name", "description", "url", "state"
	};

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

	public void markPointUpdate(Point point) {
		long pointId = findPointId(point);
		ContentValues updateCv = new ContentValues(1);
		updateCv.put("pointId", pointId);
		db.insert("point_update", null, updateCv);

		Cursor cursor = db.rawQuery("SELECT tp.trackId FROM tp WHERE tp.pointId=?", Utils.toArray(pointId));
		while (cursor.moveToNext()) {
			ContentValues updateCv2 = new ContentValues(1);
			updateCv.put("trackId", cursor.getLong(0));
			db.insert("track_update", null, updateCv2);
		}
		cursor.close();
	}

	public void markTrackUpdate(Track track) {
		long trackId = findTrackId(track);
		ContentValues updateCv = new ContentValues(1);
		updateCv.put("trackId", trackId);
		db.insert("track_update", null, updateCv);
	}

	public void clearUpdates() {
		db.execSQL("delete from track_update;");
		db.execSQL("delete from point_update;");
	}

	public long insertPoint(Point newPoint, Point oldPoint) {
		if (oldPoint == null && findPointId(newPoint) != -1)
			return -1;

		long oldPointId = -1;
		if (oldPoint != null) {
			oldPointId = findPointId(oldPoint);
			if (oldPointId == -1)
				return -1;
		}

		ContentValues cv = new ContentValues(8);
		cv.put("name", newPoint.getName());
		cv.put("description", newPoint.getDescription());
		cv.put("lat", newPoint.getLatE6());
		cv.put("lon", newPoint.getLonE6());
		cv.put("private", newPoint.isPrivate());

		if (newPoint.hasAudio())
			cv.put("audioUrl", newPoint.getAudioUrl());
		if (newPoint.hasPhoto())
			cv.put("photoUrl", newPoint.getPhotoUrl());
		if (newPoint.getCategoryId() != -1)
			cv.put("categoryId", newPoint.getCategoryId());

		if (oldPoint == null) {
			return db.insert("point", null, cv);
		} else {
			db.update("point", cv, "id=?", Utils.toArray(oldPointId));

			return oldPointId;
		}
	}

	public long insertTrack(Track track) {
		ContentValues cv = new ContentValues(8);
		cv.put("name", track.getName());
		cv.put("description", track.getDescription());
		cv.put("hname", track.getHname());
		cv.put("url", track.getUrl());

		cv.put("active", track.isActive());
		cv.put("local", track.isLocal());
		cv.put("private", track.isPrivate());

		cv.put("categoryId", track.getCategoryId());

		long newId = db.insert("track", null, cv);

		if (newId == -1) {
			// Don't override local track status by remote track
			if (!track.isLocal())
				cv.remove("local");

			db.update("track", cv, "name=?", Utils.toArray(track.getName()));
		}

		return newId;
	}

	public void insertToTrack(Track track, Point point) {
		long pointId = findPointId(point);
		if (pointId == -1)
			pointId = insertPoint(point, null);

		long trackId = findTrackId(track);
		if (trackId == -1)
			trackId = insertTrack(track);

		long relationId = findRelationId(trackId, pointId);
		if (relationId == -1) {
			db.execSQL("INSERT INTO tp VALUES (?, ?, (SELECT (IFNULL (MAX(idx), 0) + 1) FROM tp));", Utils.toArray(trackId, pointId));
		}
	}

	public Cursor loadTracksCursor() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.id AS _id " +
				"FROM track LEFT JOIN category " +
				"ON category.id = track.categoryId " +
				"WHERE category.state = 1 OR category.state IS NULL;", null);
		return cursor;
	}

	public Cursor loadPointsCursor(Track track) {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.id AS _id " +
				"FROM point INNER JOIN tp " +
				"ON tp.pointId=point.id " +
				"WHERE tp.trackId IN (SELECT track.id FROM track WHERE track.name=?) " +
				"ORDER BY tp.idx;", Utils.toArray(track.getName()));
		return cursor;
	}


	public Cursor loadPointsCursor() {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.id AS _id " +
						"FROM point;", null);
		return cursor;
	}

	public Cursor loadRelationsCursor() {
		Cursor cursor = db.rawQuery(
				"SELECT tp.trackId, tp.pointId FROM tp INNER JOIN track " +
						"ON track.id = tp.trackId " +
						"WHERE track.local = 1 " +
						"ORDER BY tp.trackId, tp.idx ", null);
		return cursor;
	}

	public Cursor loadPrivateTracks() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.id AS _id " +
				"FROM track " +
				"WHERE track.private = 1;", null);
		return cursor;

	}

	public Cursor loadUpdatedTracks() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.id AS _id " +
				"FROM track INNER JOIN track_update " +
				"ON track.id = track_update.trackId " +
				"GROUP BY track.id", null);
		return cursor;
	}

	public Cursor loadUpdatedPoints() {
		Cursor cursor = db.rawQuery("SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.id AS _id " +
				"FROM point INNER JOIN point_update " +
				"ON point.id = point_update.pointId " +
				"GROUP BY point.id;", null);
		return cursor;
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
		Cursor cursor = db.query("track", ID_COLUMNS, "name=?",	Utils.toArray(track.getName()), null, null, null);

		try {
			if (!cursor.moveToFirst())
				return -1;
			else
				return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	private long findRelationId(long trackId, long pointId) {
		Cursor cursor = db.query("tp", Utils.toArray("ROWID"), "trackId=? and pointId=?", Utils.toArray(trackId, pointId),
				null, null, null);

		try {
			if (!cursor.moveToFirst())
				return -1;
			else
				return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public List<Category> getCategories() {
		List<Category> categories = new ArrayList<Category>();

		Cursor cursor = db.query("category", SELECT_CATEGORIES_COLUMNS,
				null, null, null, null, null, null);

		while (cursor.moveToNext()) {
			Category category = new Category(cursor.getLong(0), cursor.getString(1),
					cursor.getString(2), cursor.getString(3), cursor.getInt(4) == 1);
			categories.add(category);
		}

		cursor.close();

		return categories;
	}

	public List<Category> getActiveCategories() {
		List<Category> categories = new ArrayList<Category>();

		Cursor cursor = db.query("category", SELECT_CATEGORIES_COLUMNS,
				"state=1", null, null, null, null, null);

		while (cursor.moveToNext()) {
			Category category = new Category(cursor.getLong(0), cursor.getString(1),
					cursor.getString(2), cursor.getString(3), cursor.getInt(4) == 1);
			categories.add(category);
		}

		cursor.close();

		return categories;
	}

	public void updateCategories(List<Category> categories) {
		db.beginTransaction();

		try {
			for (Category category : categories) {
				ContentValues cv = new ContentValues(5);
				cv.put("name", category.getName());
				cv.put("description", category.getDescription());
				cv.put("url", category.getUrl());

				Cursor cursor = db.query("category", new String[]{"state"}, "id=?",
						new String[]{String.valueOf(category.getId())}, null, null, null);

				if (cursor.moveToFirst()) {
					db.update("category", cv, "id=?",
							new String[]{String.valueOf(category.getId())});
					category.setActive(cursor.getInt(0) == 1);
				} else {
					cv.put("state", 1);
					cv.put("id", category.getId());
					db.insert("category", null, cv);
				}

				cursor.close();
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}


	public void setCategoryState(Category category) {
		ContentValues cv = new ContentValues(1);
		cv.put("state", category.isActive() ? 1 : 0);

		db.update("category", cv, "id=?", new String[]{String.valueOf(category.getId())});
	}

	private static class Helper extends SQLiteOpenHelper {
		public static final String DB_NAME = "tracksdb2";
		public static final int DB_VERSION = 6; // published None

		public static final String CREATE_POINT_UPDATES_SQL = "CREATE TABLE point_update " +
				"(pointId INTEGER," +
				"FOREIGN KEY(pointId) REFERENCES point(id) ON DELETE CASCADE);";

		public static final String CREATE_TRACK_UPDATES_SQL = "CREATE TABLE track_update " +
				"(trackId INTEGER," +
				"FOREIGN KEY(trackId) REFERENCES track(id) ON DELETE CASCADE);";

		public static final String CREATE_TRACKS_SQL = "CREATE TABLE track " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name TEXT UNIQUE," +
				"description TEXT," +
				"hname TEXT," +
				"url TEXT," +
				"active INTEGER," +
				"local INTEGER," +
				"private INTEGER," +
				"categoryId INTEGER);";

		public static final String CREATE_POINTS_SQL = "CREATE TABLE point " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name TEXT," +
				"description TEXT," +
				"lat INTEGER," +
				"lon INTEGER," +
				"audioUrl TEXT," +
				"photoUrl TEXT," +
				"categoryId INTEGER," +
				"private INTEGER, " +

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
			db.execSQL(CREATE_POINT_UPDATES_SQL);
			db.execSQL(CREATE_TRACK_UPDATES_SQL);
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
			if (oldVersion != newVersion) {
				db.execSQL("drop table category");
				db.execSQL("drop table track");
				db.execSQL("drop table tp");
				db.execSQL("drop table point");
				db.execSQL("drop table point_update");
				db.execSQL("drop table track_update");
				onCreate(db);
			}
		}
	}
}

