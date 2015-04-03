package org.fruct.oss.audioguide.track;

import android.location.Location;

import org.fruct.oss.audioguide.gets.Category;

import java.io.Closeable;
import java.util.List;

public interface TrackManager extends Closeable {
	public static final String PREF_TRACK_MODE = "pref_track_mode";

	void storeTrackLocal(Track track);

	void requestTracksInRadius();

	void requestPointsInRadius(float latitude, float longitude, boolean autoStore);

	void requestPointsInTrack(Track track);

	void activateTrackMode(Track track);

	void addListener(TrackListener listener);

	void removeListener(TrackListener listener);

	void close();

	CursorHolder loadTracks();

	CursorHolder loadLocalTracks();

	CursorHolder loadLocalPoints();

	CursorHolder loadPoints(Track track);

	Track getTrackByName(String name);

	void updateUserLocation(Location location);

	void updateLoadRadius(float radius);


	List<Category> getCategories();

	List<String> getPointPhotos(Point point);

	void setCategoryState(Category category, boolean isActive);

	void deleteTrack(Track track, boolean deleteFromServer);

	void requestPointsCleanup();

	void synchronizeFileManager();
}
