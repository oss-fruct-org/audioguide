package org.fruct.oss.audioguide.track;

import android.location.Location;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;

import java.util.List;

public interface TrackManager {
	public static final String PREF_TRACK_MODE = "pref_track_mode";

	void insertPoint(Point point);

	void insertTrack(Track track);

	void insertToTrack(Track track, Point point);

	void storeTrackLocal(Track track);

	void requestTracksInRadius();

	void requestPointsInRadius(float latitude, float longitude, boolean autoStore);

	void requestPointsInTrack(Track track);

	void activateTrackMode(Track track);

	void addListener(TrackListener listener);

	void removeListener(TrackListener listener);


	CursorHolder loadTracks();

	CursorHolder loadPrivateTracks();


	CursorHolder loadLocalPoints();

	CursorHolder loadPoints(Track track);

	CursorHolder loadRelations();

	Track getTrackByName(String name);

	void updateUserLocation(Location location);

	void updateLoadRadius(float radius);


	List<Category> getCategories();

	void setCategoryState(Category category, boolean isActive);

}
