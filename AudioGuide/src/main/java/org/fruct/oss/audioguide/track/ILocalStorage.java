package org.fruct.oss.audioguide.track;

import java.io.IOException;
import java.util.List;

public interface ILocalStorage extends IStorage {
	/**
	 * Store or update track in local storage
	 * @param track Track to update
	 */
	void storeLocalTrack(Track track);

	/**
	 * Store points to storage
	 * @param track
	 * @param points
	 */
	void storeLocalPoints(Track track, List<Point> points);

	/**
	 * Update point of track
	 */
	void updatePoint(Track track, Point point);

	/**
	 * Get content of track
	 * This method should return immediately
	 * @param track Track
	 */
	List<Point> getPoints(Track track);

	/**
	 * Get list of tracks
	 * This method should return immediately
	 * @return list of tracks
	 */
	List<Track> getTracks();
}
