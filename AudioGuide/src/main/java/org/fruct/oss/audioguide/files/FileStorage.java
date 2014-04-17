package org.fruct.oss.audioguide.files;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;

public class FileStorage implements Closeable {
	public static final String TABLE = FileDatabaseHelper.TABLE;

	public static final int STATE_REMOTE = 0;
	public static final int STATE_DOWNLOADING = 1;
	public static final int STATE_LOCAL = 2;

	private final SQLiteDatabase db;
	private FileDatabaseHelper dbHelper;

	public FileStorage(Context context) {
		dbHelper = new FileDatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
	}

	@Override
	public void close() {
		dbHelper.close();
	}

	/**
	 * @param remoteUrl url
	 * @return true if file in local storage
	 */
	public synchronized boolean isLocal(String remoteUrl) {
		return count("remoteUrl=? and state=0", arg(remoteUrl)) != 0;
	}

	/**
	 * @param remoteUrl url
	 * @return true if file is being download
	 */
	public synchronized boolean isDownloading(String remoteUrl) {
		return count("remoteUrl=? and state=1", arg(remoteUrl)) != 0;
	}

	/**
	 * @param remoteUrl url
	 * @return true if file is remote
	 */
	public synchronized boolean isRemote(String remoteUrl) {
		return count("remoteUrl=? and state=2", arg(remoteUrl)) != 0;
	}

	/**
	 * Store url if not exist
	 * @param remoteUrl url
	 */
	public synchronized void insertRemoteUrl(String remoteUrl) {
		boolean exists = count("remoteUrl=?", arg(remoteUrl)) != 0;

		if (!exists) {
			ContentValues cv = new ContentValues(2);
			cv.put("remoteUrl", remoteUrl);
			cv.put("state", STATE_REMOTE);
			db.insert(TABLE, null, cv);
			this.notifyAll();
		}
	}

	private String[] getPendingUrlColumns = new String[] {"remoteUrl"};
	public synchronized String getPendingUrl() {
		Cursor c = db.query(TABLE, getPendingUrlColumns, "state=0", null, null, null, null, null);

		if (!c.moveToFirst()) {
			return null;
		}

		return c.getString(0);
	}

	public synchronized void setFileState(String remoteUrl, int state) {
		ContentValues cv = new ContentValues(1);
		cv.put("state", state);
		db.update(TABLE, cv, "remoteUrl=?", arg(remoteUrl));
	}

	private String[] getLocalUrlColumns = new String[] {"localUrl"};
	public synchronized String getLocalUrl(String remoteUrl) {
		Cursor c = db.query(TABLE, getLocalUrlColumns, "state=2 and remoteUrl=?", arg(remoteUrl),
				null, null, null);

		if (!c.moveToFirst()) {
			return null;
		}

		return c.getString(0);
	}

	public synchronized void setFileLocal(String remoteUrl, String localUrl) {
		ContentValues cv = new ContentValues(2);
		cv.put("localUrl", localUrl);
		cv.put("state", STATE_LOCAL);
		db.update(TABLE, cv, "remoteUrl=?", arg(remoteUrl));
	}

	private final String[] args = new String[1];
	private String[] arg(String arg1) {
		args[0] = arg1;
		return args;
	}

	private final String[] args2 = new String[2];
	private String[] arg(String arg1, String arg2) {
		args2[0] = arg1;
		args2[1] = arg2;
		return args2;
	}

	private int count(String whereQuery, String[] args) {
		Cursor c = db.rawQuery("select count(*) from " + TABLE + " where " + whereQuery, args);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}
}
