package org.fruct.oss.audioguide.track;

import org.fruct.oss.audioguide.track.gets.Category;

import java.io.Closeable;
import java.util.List;

public interface TrackManager extends Closeable {
	public static final String PREF_TRACK_MODE = "pref_track_mode";

	void activateTrackMode(Track track);

	void close();

	CursorHolder loadTracks();

	CursorHolder loadLocalTracks();

	CursorHolder loadPrivateTracks();

	CursorHolder loadPublicTracks();

	CursorHolder loadLocalPoints();

	CursorHolder loadPoints(Track track);


	Track getTrackByName(String name);


	List<String> getPointPhotos(Point point);

	void setCategoryState(Category category, boolean isActive);
}
