package org.fruct.oss.audioguide.track;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DatabaseStorage implements ILocalStorage {
	private final static Logger log = LoggerFactory.getLogger(DatabaseStorage.class);

	public static final String DB_NAME = "tracksdb";
	public static final int DB_VERSION = 1;
	public static final String CREATE_SQL = "CREATE TABLE tracks " +
			"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
			"name TEXT," +
			"description TEXT," +
			"url TEXT)";

	public static final String[] SELECT_COLUMNS = {
			"id", "name", "description", "url"
	};

	private final Context context;

	private TrackDatabaseHelper helper;
	private SQLiteDatabase db;

	private ArrayList<Track> tracks;

	public DatabaseStorage(Context context) {
		this.context = context;
	}

	@Override
	public void storeLocalTrack(Track track) {
		ContentValues cv = new ContentValues();
		cv.put("name", track.getName());
		cv.put("description", track.getDescription());
		cv.put("url", track.getUrl());

		db.insert("tracks", null, cv);
	}

	@Override
	public void initialize() {
		helper = new TrackDatabaseHelper(context);
		db = helper.getWritableDatabase();
	}

	@Override
	public void close() {
		if (helper != null) {
			helper.close();
			helper = null;
		}
	}

	@Override
	public void load() {
		Cursor cursor = db.query("tracks", SELECT_COLUMNS, null, null, null, null, null);

		tracks = new ArrayList<Track>(cursor.getCount());

		if (!cursor.moveToFirst())
			return;

		do {
			Track track = new Track(cursor.getString(1), cursor.getString(2), cursor.getString(3));
			track.setLocal(true);
			tracks.add(track);
		} while (cursor.moveToNext());
	}

	@Override
	public List<Track> getTracks() {
		return tracks;
	}

	private static class TrackDatabaseHelper extends SQLiteOpenHelper {
		public TrackDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);

			log.debug("Context: {}", context);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_SQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}
}
