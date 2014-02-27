package org.fruct.oss.audioguide.track;

import java.util.List;

public interface ILocalStorage extends IStorage {
	/**
	 * Store or update track in local storage
	 * @param track Track to update
	 */
	void storeLocalTrack(Track track);

	void updateLocalTrack(Track track, String field, Object value);

	/**
	 * Store points to storage
	 * @param track
	 * @param points
	 */
	void storeLocalPoints(Track track, List<Point> points);
}
