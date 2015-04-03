package org.fruct.oss.audioguide.track;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class Database {
	private boolean isClosed;

	private final SQLiteDatabase db;
	private final Helper helper;

	public static final String[] SELECT_CATEGORIES_COLUMNS = {
			"id", "name", "description", "url", "state"
	};

	public static final String[] ID_COLUMNS = {"id"};

	public static final String URL_TYPE_AUDIO = "audio";
	public static final String URL_TYPE_PHOTO = "photo";

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
			cv.put("audioUrl", findOrInsertUrl(newPoint.getAudioUrl(), URL_TYPE_AUDIO));
		if (newPoint.hasPhoto())
			cv.put("photoUrl", findOrInsertUrl(newPoint.getPhotoUrl(), URL_TYPE_PHOTO));
		if (newPoint.getCategoryId() != -1)
			cv.put("categoryId", newPoint.getCategoryId());
		if (newPoint.getTime() != null)
			cv.put("time", newPoint.getTime());

		if (existingPointId == -1)
			existingPointId = db.insert("point", null, cv);
		else {
			db.update("point", cv, "id=?", Utils.toArray(existingPointId));
		}

		db.execSQL("DELETE FROM point_photo WHERE pointId=?", Utils.toArray());
		for (String photoUrl : newPoint.getPhotoUrls()) {
			insertPointPhoto(existingPointId, photoUrl);
		}

		return existingPointId;
	}

	public long insertTrack(Track track) {
		ContentValues cv = new ContentValues(9);
		cv.put("name", track.getName());
		cv.put("description", track.getDescription());
		cv.put("hname", track.getHname());
		cv.put("url", track.getUrl());

		cv.put("active", track.isActive());
		cv.put("local", track.isLocal());
		cv.put("private", track.isPrivate());

		cv.put("categoryId", track.getCategoryId());

		if (track.hasPhoto())
			cv.put("photoUrl", findOrInsertUrl(track.getPhotoUrl(), URL_TYPE_PHOTO));

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

	private void insertPointPhoto(long pointId, String url) {
		long photoUrlId = findOrInsertUrl(url, URL_TYPE_PHOTO);
		db.execSQL("INSERT INTO point_photo VALUES (?, ?);", Utils.toArray(pointId, photoUrlId));
	}

	public Cursor loadPointPhotos(Point point) {
		long pointId = findPointId(point);

		Cursor cursor = db.rawQuery("SELECT DISTINCT url.url " +
				"FROM url " +
				"INNER JOIN point_photo ON point_photo.urlId = url.id " +
				"INNER JOIN point ON point.id = point_photo.pointId " +
				"WHERE point.id=?;", Utils.toArray(pointId));
		return cursor;
	}

	public Cursor loadTracksCursor() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, url.url, track.id AS _id " +
				"FROM track LEFT JOIN category ON category.id = track.categoryId " +
				"LEFT JOIN url ON url.id = track.photoUrl " +
				"WHERE category.state = 1 OR category.state IS NULL;", null);
		return cursor;
	}

	public Cursor loadPointsCursor(Track track) {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, url_audio.url, url_photo.url, point.lat, point.lon, point.private, point.categoryId, point.time, point.uuid, point.id AS _id " +
						"FROM point INNER JOIN tp ON tp.pointId=point.id " +
						"INNER JOIN track ON track.id = tp.trackId " +
						"LEFT JOIN url AS url_audio ON url_audio.id = point.audioUrl " +
						"LEFT JOIN url AS url_photo ON url_photo.id = point.photoUrl " +
						"WHERE track.name=? " +
						"ORDER BY tp.idx;", Utils.toArray(track.getName())
		);
		return cursor;
	}

	public Cursor loadPointsCursor() {
		Cursor cursor = db.rawQuery(
				"SELECT point.name, point.description, url_audio.url, url_photo.url, point.lat, point.lon, point.private, point.categoryId, point.time, point.uuid, point.id AS _id " +
						"FROM point LEFT JOIN category ON category.id = point.categoryId " +
						"LEFT JOIN url AS url_audio ON url_audio.id = point.audioUrl " +
						"LEFT JOIN url AS url_photo ON url_photo.id = point.photoUrl " +
						"WHERE category.state=1 OR category.state IS NULL;", null);
		return cursor;
	}

	public Cursor loadLocalTracks() {
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, url.url, track.id AS _id " +
				"FROM track " +
				"LEFT JOIN url ON url.id = track.photoUrl " +
				"WHERE track.local = 1;", null);
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

	private long findOrInsertUrl(String url, String type) {
		Cursor cursor = db.query("url", ID_COLUMNS, "url=?", Utils.toArray(url), null, null, null);
		try {
			if (!cursor.moveToFirst()) {
				ContentValues cv = new ContentValues(3);
				cv.put("url", url);
				cv.put("type", type);

				return db.insert("url", null, cv);
			} else {
				return cursor.getLong(0);
			}
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
		Cursor cursor = db.rawQuery("SELECT track.name, track.description, track.url, track.local, track.categoryId, track.private, track.hname, url.url, track.id AS _id " +
				"FROM track LEFT JOIN url ON url.id = track.photoUrl " +
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

	public void cleanupPoints(Location location, float radius) {
		db.beginTransaction();
		Cursor cursor = null;
		try {
			cursor = db.rawQuery("SELECT point.lat, point.lon, point.id " +
					"FROM point " +
					"LEFT JOIN tp ON tp.pointId = point.id " +
					"LEFT JOIN track ON tp.trackId = track.id " +
					"WHERE NOT point.private " +
					"AND (track.id IS NULL OR NOT track.local);", null);

			float[] dist = new float[1];
			String[] whereArgs = new String[1];
			while (cursor.moveToNext()) {
				double lat = cursor.getDouble(0) / 1e6;
				double lon = cursor.getDouble(1) / 1e6;
				int id = cursor.getInt(2);

				Location.distanceBetween(lat, lon, location.getLatitude(), location.getLongitude(), dist);
				if (dist[0] > radius) {
					whereArgs[0] = String.valueOf(id);
					db.delete("point", "id=?", whereArgs);
				}
			}

			db.execSQL("DELETE FROM track WHERE track.id NOT IN " +
					"(SELECT tp.trackId FROM tp INNER JOIN point ON tp.pointId = point.id);");

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public List<String> gcUrls() {
		Cursor cursor = db.rawQuery("SELECT url.url, url.id " +
				"FROM url " +
				"LEFT JOIN point_photo ON point_photo.urlId = url.id " +
				"LEFT JOIN track ON track.photoUrl = url.id " +
				"LEFT JOIN point AS point1 ON point1.photoUrl = url.id " +
				"LEFT JOIN point AS point2 ON point2.audioUrl = url.id " +

				"WHERE point_photo.pointId IS NULL " +
				"AND track.id IS NULL " +
				"AND point1.id IS NULL " +
				"AND point2.id IS NULL;", null);

		List<String> ret = new ArrayList<String>(cursor.getCount());

		try {
			db.beginTransaction();
			while (cursor.moveToNext()) {
				ret.add(cursor.getString(0));
				db.delete("url", "id=?", Utils.toArray(cursor.getInt(1)));
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		cursor.close();
		return ret;
	}

	private static class Helper extends SQLiteOpenHelper {
		public static final String DB_NAME = "tracksdb2";
		public static final int DB_VERSION = 2; // published 2

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
				"categoryId INTEGER," +
				"photoUrl INTEGER," +

				"FOREIGN KEY (photoUrl) REFERENCES url(id));";

		public static final String CREATE_POINTS_SQL = "CREATE TABLE point " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"name TEXT," +
				"description TEXT," +
				"lat INTEGER," +
				"lon INTEGER," +
				"audioUrl INTEGER," +
				"photoUrl INTEGER," +
				"categoryId INTEGER," +
				"private INTEGER, " +
				"time TEXT, " +
				"uuid TEXT, " +

				"FOREIGN KEY (audioUrl) REFERENCES url(id)," +
				"FOREIGN KEY (photoUrl) REFERENCES url(id)," +
				"FOREIGN KEY (categoryId) REFERENCES category(id));";

		public static final String CREATE_TRACK_POINTS_SQL = "CREATE TABLE tp " +
				"(trackId INTEGER, pointId INTEGER, idx INTEGER," +
				"FOREIGN KEY (trackId) REFERENCES track(id) ON DELETE CASCADE," +
				"FOREIGN KEY (pointId) REFERENCES point(id) ON DELETE CASCADE);";

		public static final String CREATE_CATEGORIES_SQL = "CREATE TABLE category " +
				"(id INTEGER PRIMARY KEY," +
				"name TEXT," +
				"description TEXT," +
				"url TEXT," +
				"state INTEGER);"; // 1 - active, 0 - non-active

		public static final String CREATE_URLS_SQL = "CREATE TABLE url " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"url TEXT UNIQUE," +
				"type TEXT);";

		public static final String CREATE_POINT_PHOTO_SQL = "CREATE TABLE point_photo " +
				"(pointId INTEGER," +
				"urlId INTEGER," +

				"FOREIGN KEY (pointId) REFERENCES point(id) ON DELETE CASCADE," +
				"FOREIGN KEY (urlId) REFERENCES url(id));";

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
			db.execSQL(CREATE_URLS_SQL);
			db.execSQL(CREATE_POINT_PHOTO_SQL);
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
				db.execSQL("DROP TABLE IF EXISTS point_update");
				db.execSQL("DROP TABLE IF EXISTS track_update");
				db.execSQL("DROP TABLE IF EXISTS point_photo");
				db.execSQL("DROP TABLE IF EXISTS url");
				db.execSQL("DROP TABLE IF EXISTS category");
				db.execSQL("DROP TABLE IF EXISTS tp");
				db.execSQL("DROP TABLE IF EXISTS point");
				db.execSQL("DROP TABLE IF EXISTS track");
				onCreate(db);
			}

			/*for (int version = oldVersion + 1; version <= newVersion; version++) {
				if (version == 2) {
					// Add field photoUrl to track
				}
			}*/
		}
	}
}

