package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.CursorAdapter;

import java.io.Closeable;

public abstract class CursorHolder implements Closeable {
	private CursorAdapter adapter;
	private Cursor cursor;

	private boolean isClosed;

	public synchronized void attachToAdapter(CursorAdapter adapter) {
		this.adapter = adapter;

		if (cursor != null) {
			adapter.changeCursor(cursor);
		}
	}

	public synchronized void close() {
		isClosed = true;
		if (cursor != null) {
			cursor.close();
			cursor = null;
		}
	}

	synchronized void onCursorReady(Cursor cursor) {
		this.cursor = cursor;
		adapter.changeCursor(cursor);
	}

	boolean isClosed() {
		return isClosed;
	}

	void queryAsync() {
		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... voids) {
				return doQuery();
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				onCursorReady(cursor);
			}
		}.execute();
	}

	protected abstract Cursor doQuery();
}
