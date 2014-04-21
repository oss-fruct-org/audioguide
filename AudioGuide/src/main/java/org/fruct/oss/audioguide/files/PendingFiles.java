package org.fruct.oss.audioguide.files;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;

public class PendingFiles implements Closeable {
	public static final String TABLE = FileDatabaseHelper.TABLE;

	private final SQLiteDatabase db;
	private FileDatabaseHelper dbHelper;

	public PendingFiles(Context context) {
		dbHelper = new FileDatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
	}

	@Override
	public void close() {
		dbHelper.close();
	}

	public synchronized void insert(String remoteUrl) {
		ContentValues cv = new ContentValues(1);
		cv.put("remoteUrl", remoteUrl);
		db.insert(TABLE, null, cv);

		notifyAll();
	}

	private String[] getPendingUrlColumns = {"remoteUrl"};
	public synchronized String getPendingUrl() {
		Cursor cursor = null;
		try {
			cursor = db.query(TABLE, getPendingUrlColumns, null, null, null, null, null);
			if (cursor.moveToFirst())
				return cursor.getString(0);
			else
				return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public synchronized void remove(String remoteUrl) {
		db.delete(TABLE, "remoteUrl=?", new String[]{remoteUrl});
	}
}

class FileDatabaseHelper extends SQLiteOpenHelper {
	public static final String TABLE = "files";
	public static final int DB_VERSION = 12;

	public FileDatabaseHelper(Context context) {
		super(context, "files-database", null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE + " " +
				"(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"remoteUrl TEXT);");
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