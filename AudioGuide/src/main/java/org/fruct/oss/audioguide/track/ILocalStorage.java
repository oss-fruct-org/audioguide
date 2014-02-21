package org.fruct.oss.audioguide.track;

public interface ILocalStorage extends IStorage {
	/**
	 * Store or update track in local storage
	 * @param track Track to update
	 */
	void storeLocalTrack(Track track);
}
