package org.fruct.oss.audioguide.track;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;

public interface StorageBackend {
	void updateTrack(Track track, List<Point> points) throws InterruptedException, GetsException;

	void updatePoint(Point point)  throws InterruptedException, GetsException;

	void insertPoint(long categoryId, Point point) throws InterruptedException, GetsException;

	void loadTracksInRadius(float lat, float lon, float radius, List<Category> categories, Utils.Callback<List<Track>> callback);

	void loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories, Utils.Callback<List<Point>> callback);

	void loadPointsInTrack(Track track, Utils.Callback<List<Point>> callback);
}
