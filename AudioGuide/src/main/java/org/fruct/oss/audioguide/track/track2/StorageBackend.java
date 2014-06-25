package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;

public interface StorageBackend {
	void updateTrack(Track track, List<Point> points);

	void loadTracksInRadius(float lat, float lon, float radius, List<Category> categories, Utils.Callback<List<Track>> callback);

	void loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories, Utils.Callback<List<Point>> callback);

	void loadPointsInTrack(Track track, Utils.Callback<List<Point>> callback);
}
