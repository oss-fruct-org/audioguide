package org.fruct.oss.audioguide.track.track2;

import android.database.Cursor;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.util.List;

public interface TrackManager {
	void insertPoint(Point point);

	void insertTrack(Track track);

	void insertToTrack(Track track, Point point);

	void storeTrackLocal(Track track);

	void requestTracksInRadius(float latitude, float longitude, float radius);

	void requestPointsInRadius(float latitude, float longitude, float radius, boolean autoStore);

	void requestPointsInTrack(Track track);

	void addListener(TrackListener listener);

	void removeListener(TrackListener listener);


	CursorHolder loadTracks();

	CursorHolder loadLocalPoints();

	CursorHolder loadPoints(Track track);

	CursorHolder loadRelations();


	// Accessors
	Model<Track> getTracksModel();

	Model<Track> getLocalTracksModel();

	Model<Track> getRemoteTracksModel();

	Model<Point> getRemotePointsModel();


	Model<Point> getPointsModel();

	Model<Point> getTrackPointsModel(Track category);

	Model<Point> getCategoryPointsModel(Category track);

	List<Category> getCategories();

	void setCategoryState(Category category, boolean isActive);

}
