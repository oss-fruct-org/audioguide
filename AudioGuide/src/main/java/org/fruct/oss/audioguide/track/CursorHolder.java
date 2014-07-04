package org.fruct.oss.audioguide.track;

import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;

import java.io.Closeable;

public abstract class CursorHolder implements Closeable {
	public static interface Listener {
		void onReady(Cursor cursor);
	}

	private Listener listener;
	private CursorReceiver cursorReceiver;
	private Cursor cursor;

	private boolean isClosed;

	public synchronized void attachToReceiver(CursorReceiver receiver) {
		this.cursorReceiver = receiver;

		if (cursor != null) {
			cursorReceiver.swapCursor(cursor);
		}
	}

	public synchronized void attachToAdapter(final CursorAdapter adapter) {
		attachToReceiver(new CursorReceiver() {
			@Override
			public Cursor swapCursor(Cursor cursor) {
				return adapter.swapCursor(cursor);
			}
		});
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

		Cursor oldCursor = cursorReceiver.swapCursor(cursor);
		if (oldCursor != null) {
			oldCursor.close();
		}

		if (listener != null) {
			listener.onReady(cursor);
		}
	}

	boolean isClosed() {
		return isClosed;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
		if (cursor != null) {
			listener.onReady(cursor);
		}
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
