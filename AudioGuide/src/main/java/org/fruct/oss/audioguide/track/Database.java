package org.fruct.oss.audioguide.track;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fruct.oss.audioguide.gets.Category;
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
			updateCv2.put("trackId", cursor.getLong(0));
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

	public void clearTrackUpdates() {
		db.execSQL("delete from track_update;");
	}

	public void clearPointUpdate(long pointId) {
		db.execSQL("delete from point_update where pointId=?", Utils.toArray(pointId));
	}

	public long insertPoint(Point newPoint) {
		long existingPointId = findPointId(newPoint);

		ContentValues cv = new ContentValues(10);
		cv.put("name", newPoint.getName());
		cv.put("description", newPoint.getDescription());
		cv.put("lat", newPoint.getLatE6());
		cv.put("lon", newPoint.getLonE6());
		cv.put("private", newPoint.isPrivate());
		cv.put("uuid", newPoint.getUuid());

		if (newPoint.hasAudio())
			cv.put("audioUrl", newPoint.getAudioUrl());
		if (newPoint.hasPhoto())
			cv.put("photoUrl", newPoint.getPhotoUrl());
		if (newPoint.getCategoryId() != -1)
			cv.put("categoryId", newPoint.getCategoryId());
		if (newPoint.getTime() != null)
			cv.put("time", newPoint.getTime());

		if (existingPointId == -1)
			return db.insert("point", null, cv);
		else {
			db.update("point", cv, "id=?", Utils.toArray(existingPointId));
			return existingPointId;
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

	public void insertPointsToTrack(Track track, List<Point> points) {
		long trackId = findTrackId(track);

		db.beginTransaction();
		try {
			if (trackId == -1)
				trackId = insertTrack(track);
			db.delete("tp", "tp.trackId=?", Utils.toArray(trackId));
			int idx = 0;
			for (Point point : points) {
				long pointId = insertPoint(point);
				db.execSQL("INSERT INTO tp VALUES (?, ?, ?);", Utils.toArray(trackId, pointId, idx));
				idx++;
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public void insertToTrack(Track track, Point point, int position) {
		long pointId = insertPoint(point);

		long trackId = findTrackId(track);
		if (trackId == -1)
			trackId = insertTrack(track);

		// Reorder points
		db.beginTransaction();
		try {
			db.execSQL("UPDATE tp SET idx = idx + 1 WHERE idx >= ? AND trackId=?;", Utils.toArray(position, trackId));
			db.execSQL("INSERT INTO tp VALUES (?, ?, ?);", Utils.toArray(trackId, pointId, position));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public Cursor loadTracksCursor() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, track.id AS _id " +
				"FROM track LEFT JOIN category " +
				"ON category.id = track.categoryId " +
				"WHERE category.state = 1 OR category.state IS NULL;", null);
		return cursor;
	}

	public Cursor loadPointsCursor(Track track) {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.categoryId, point.time, point.uuid, point.id AS _id " +
						"FROM point INNER JOIN tp " +
						"ON tp.pointId=point.id " +
						"WHERE tp.trackId IN (SELECT track.id FROM track WHERE track.name=?) " +
						"ORDER BY tp.idx;", Utils.toArray(track.getName())
		);
		return cursor;
	}


	public Cursor loadPointsCursor() {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.categoryId, point.time, point.uuid, point.id AS _id " +
						"FROM point LEFT JOIN category " +
						"ON category.id = point.categoryId " +
						"WHERE category.state=1 OR category.state IS NULL;", null);
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
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, track.id AS _id " +
				"FROM track " +
				"WHERE track.private = 1;", null);
		return cursor;
	}

	public Cursor loadUpdatedTracks() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, track.id AS _id " +
				"FROM track INNER JOIN track_update " +
				"ON track.id = track_update.trackId " +
				"GROUP BY track.id", null);
		return cursor;
	}

	public Cursor loadUpdatedPoints() {
		// FIXME: java.lang.IllegalStateException: database /data/data/org.fruct.oss.audioguide/databases/tracksdb2 already closed
		// From synchronized
		Cursor cursor = db.rawQuery("SELECT point.name, point.description, point.audioUrl, point.photoUrl, point.lat, point.lon, point.private, point.categoryId, point.time, point.uuid, point.id AS _id " +
				"FROM point INNER JOIN point_update " +
				"ON point.id = point_update.pointId " +
				"GROUP BY point.id;", null);
		return cursor;
	}

	private long findPointId(Point point) {
		Cursor cursor;
		if (point.getUuid() == null) {
			cursor = db.query("point", ID_COLUMNS, "name=? and description=?",
					Utils.toArray(point.getName(), point.getDescription()), null, null, null);
		} else {
			cursor = db.query("point", ID_COLUMNS, "uuid=?",
					Utils.toArray(point.getUuid()), null, null, null);
		}

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
		Cursor cursor = db.query("track", ID_COLUMNS, "name=?", Utils.toArray(track.getName()), null, null, null);

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

	public Track getTrackByName(String name) {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, track.id AS _id " +
				"FROM track " +
				"WHERE track.name=?;", Utils.toArray(name));

		try {
			if (cursor.moveToFirst()) {
				return new Track(cursor);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	public void deleteTrack(Track track) {
		db.delete("point", "point.id IN (SELECT tp.pointId FROM tp INNER JOIN track ON track.id=tp.trackId WHERE track.name=?);", new String[]{ track.getName() });
		db.delete("track", "name=?", new String[]{ track.getName() });
	}

	private static class Helper extends SQLiteOpenHelper {
		public static final String DB_NAME = "tracksdb2";
		public static final int DB_VERSION = 1; // published None

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
				"time TEXT, " +
				"uuid TEXT, " +

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

