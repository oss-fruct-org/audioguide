package org.fruct.oss.audioguide.track;

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
	 * This method can do long asynchronous work
	 */
	void load();

	/**
	 * Get list of tracks
	 * This method should return immediately
	 * @return list of tracks
	 */
	List<Track> getTracks();

	/**
	 * Get content of track
	 * This method can do long asynchronous work
	 * @param track Track
	 */
	List<Point> getPoints(Track track) throws IOException;
}
