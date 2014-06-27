package org.fruct.oss.audioguide.track;

import android.database.Cursor;

public interface CursorReceiver {
	Cursor swapCursor(Cursor cursor);
}
