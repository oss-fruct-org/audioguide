package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.util.List;

public interface TrackManager {
	void insertPoint(Point point);

	void insertTrack(Track track);

	void insertToTrack(Track track, Point point);

	void requestTracksInRadius(float radius);

	void requestPointsInRadius(float radius);

	void requestPointsInTrack(Track track);


	// Accessors
	Model<Track> getTracksModel();

	Model<Track> getRemoteTracksModel();

	Model<Point> getRemotePointsModel();


	Model<Point> getTrackPointsModel(Track category);

	Model<Point> getCategoryPointsModel(Category track);

	List<Category> getCategories();
}
