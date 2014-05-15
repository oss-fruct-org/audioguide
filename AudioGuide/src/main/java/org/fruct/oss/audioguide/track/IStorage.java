package org.fruct.oss.audioguide.track;

import android.os.Handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface IStorage extends Closeable {
	/**
	 * Perform some initialization tasks like database opening
	 */
	void initialize();

	/**
	 * Close storage releasing all resources
	 */
	void close();

	/**
	 * Load or reload track list from storage
	 * This method should return immediately
	 */
	void loadAsync(Handler handler);

	/**
	 * Load or reload track list from storage
	 * This method should return immediately
	 */
	void loadPoints(Track track, Handler handler);
}
