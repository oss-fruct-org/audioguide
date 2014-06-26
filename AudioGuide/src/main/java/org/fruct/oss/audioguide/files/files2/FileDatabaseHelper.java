package org.fruct.oss.audioguide.files.files2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FileDatabaseHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 6;
	private static final String CREATE_FILE_TABLE = "CREATE TABLE file " +
			"(title TEXT, " +
			"localUrl TEXT, " +
			"cacheUrl TEXT, " +
			"remoteUrl TEXT," +
			"uploadRequested INTEGER)"; // 1 - requested, 0 - not requested

	public FileDatabaseHelper(Context context) {
		super(context, "filesdb2", null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_FILE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			db.execSQL("drop table file;");
			onCreate(db);
		}
	}
}
