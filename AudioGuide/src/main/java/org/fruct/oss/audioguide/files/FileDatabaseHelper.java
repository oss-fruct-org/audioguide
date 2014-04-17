package org.fruct.oss.audioguide.files;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fruct.oss.audioguide.track.DatabaseStorage;

class FileDatabaseHelper extends SQLiteOpenHelper {
	public static final String TABLE = "files";
	public static final int DB_VERSION = DatabaseStorage.DB_VERSION;

	public FileDatabaseHelper(Context context) {
		super(context, "files-database", null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE + " " +
		"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"localUrl TEXT," +
				"remoteUrl TEXT," +
				"state INTEGER);"); // 0 - remote, 1 - downloading, 2 - local
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table " + TABLE + ";");
		onCreate(db);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table " + TABLE + ";");
		onCreate(db);
	}
}
