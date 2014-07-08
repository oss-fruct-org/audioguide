package org.fruct.oss.audioguide.files;

import android.graphics.Bitmap;
import android.net.Uri;

import org.fruct.oss.audioguide.parsers.GetsException;

import java.io.Closeable;
import java.io.IOException;

public interface FileManager extends Closeable {

	void close();

	public enum ScaleMode {
		NO_SCALE, SCALE_CROP, SCALE_FIT
	}

	Uri insertLocalFile(String title, Uri localUri);
	void insertRemoteFile(String title, Uri remoteUri);

	String getLocalPath(Uri remoteUri);
	Uri uploadLocalFile(Uri localUri) throws IOException, GetsException;

	Bitmap getImageBitmap(String remoteUrl, int width, int height, ScaleMode mode);

	boolean isLocal(Uri uri);

	void addWeakListener(FileListener pointCursorAdapter);
}
