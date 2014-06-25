package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;
import android.location.Location;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.util.List;

public interface TrackManager {
	void insertPoint(Point point);

	void updatePoint(Point newPoint, Point oldPoint);

	void insertTrack(Track track);

	void insertToTrack(Track track, Point point);

	void storeTrackLocal(Track track);

	void requestTracksInRadius();

	void requestPointsInRadius(float latitude, float longitude, float radius, boolean autoStore);

	void requestPointsInTrack(Track track);


	void addListener(TrackListener listener);

	void removeListener(TrackListener listener);


	CursorHolder loadTracks();

	CursorHolder loadPrivateTracks();


	CursorHolder loadLocalPoints();

	CursorHolder loadPoints(Track track);

	CursorHolder loadRelations();


	void updateUserLocation(Location location);

	void updateLoadRadius(float radius);


	Model<Track> getLocalTracksModel();

	Model<Point> getTrackPointsModel(Track category);

	List<Category> getCategories();

	void setCategoryState(Category category, boolean isActive);

}
