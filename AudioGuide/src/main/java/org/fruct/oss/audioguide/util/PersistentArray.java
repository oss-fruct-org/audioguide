package org.fruct.oss.audioguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PersistentArray {
	private final Context context;
	private final String name;
	private final Helper helper;
	private final SQLiteDatabase db;

	public PersistentArray(Context context, String name) {
		this.context = context;
		this.name = name;
		this.helper = new Helper(context, name);
		this.db = helper.getWritableDatabase();
	}

	public void close() {
		helper.close();
	}

	public void put(String value) {
		ContentValues cv = new ContentValues(1);
		cv.put("value", value);
		db.insert(name, null, cv);
	}

	public String getFirst() {
		Cursor cursor = db.rawQuery("SELECT value from " + name + ";", null);
		if (!cursor.moveToFirst())
			return null;

		String value = cursor.getString(0);
		cursor.close();
		return value;
	}

	public void delete(String value) {
		db.execSQL("DELETE from " + name + " WHERE value=?;", Utils.toArray(value));
	}

	private class Helper extends SQLiteOpenHelper {
		private String name;

		public Helper(Context context, String name) {
			super(context, name, null, 1);

			this.name = name;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + name + " (value TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE " + name);
			onCreate(db);
		}
	}

}
