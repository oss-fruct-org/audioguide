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

	List<Point> getPoints(Track track);
}
