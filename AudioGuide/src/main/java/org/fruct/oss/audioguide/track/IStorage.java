package org.fruct.oss.audioguide.track;

import java.io.Closeable;
import java.util.List;

public interface IStorage extends Closeable {
	/**
	 * Perform some initialization tasks like database opening or network connection creating
	 */
	void initialize();

	/**
	 * Close storage releasing all resources
	 */
	void close();

	/**
	 * Load or reload track list from storage
	 */
	void load();

	/**
	 * Get list of tracks
	 * @return list of tracks
	 */
	List<Track> getTracks();
}
