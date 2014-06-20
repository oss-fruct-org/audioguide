package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.util.List;

public interface StorageBackend {
	void updateTrack(Track track, List<Point> points);

	List<Track> loadTracksInRadius(float lat, float lon, float radius, List<Category> categories);

	List<Point> loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories);

	List<Point> loadPointsInTrack(Track track);


}
