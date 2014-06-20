package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test storage backend that stores all data in memory and interprets latitude and longitude as metric units
 */
public class TestStorageBackend implements StorageBackend, CategoriesBackend {
	public HashMap<Track, List<Point>> storage = new HashMap<Track, List<Point>>();

	private boolean isDisabled;
	private ArrayList<Category> categories = new ArrayList<Category>() {{
		add(new Category(1, "audio.other", "Description", "Url"));
		add(new Category(2, "audio.shops", "Shops", "Url"));
	}};

	@Override
	public void updateTrack(Track track, List<Point> points) {
		Track track2 = new Track(track);
		List<Point> points2 = Utils.map(points, new Utils.Function<Point, Point>() {
			@Override
			public Point apply(Point point) {
				return new Point(point);
			}
		});
		storage.put(track2, points2);
	}

	@Override
	public List<Track> loadTracksInRadius(float lat, float lon, float radius, final List<Category> activeCategories) {
		checkEnabled();

		List<Track> tracksInRadius = new ArrayList<Track>();

		for (final Map.Entry<Track, List<Point>> trackEntry: storage.entrySet()) {
			if (!checkTrackCategoryIsActive(trackEntry.getKey(), activeCategories))
				continue;

			for (Point point : trackEntry.getValue()) {
				float pLat = point.getLatE6() / 1e6f;
				float pLon = point.getLonE6() / 1e6f;

				if ((pLat - lat) * (pLat - lat) + (pLon - lon) * (pLon - lon) < radius * radius) {
					tracksInRadius.add(new Track(trackEntry.getKey()));
					break;
				}
			}
		}

		return tracksInRadius;
	}

	private boolean checkTrackCategoryIsActive(Track track, List<Category> activeCategories) {
		if (activeCategories != null) {
			boolean found = false;
			for (Category cat : activeCategories) {
				if (cat.getId() == track.getCategoryId()) {
					found = true;
					break;
				}
			}

			if (!found)
				return false;
		}
		return true;
	}

	@Override
	public List<Point> loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories) {
		checkEnabled();

		List<Point> pointsInRadius = new ArrayList<Point>();

		for (Map.Entry<Track, List<Point>> trackEntry: storage.entrySet()) {
			if (!checkTrackCategoryIsActive(trackEntry.getKey(), activeCategories))
				continue;

			for (Point point : trackEntry.getValue()) {
				float pLat = point.getLatE6() / 1e6f;
				float pLon = point.getLonE6() / 1e6f;

				if ((pLat - lat) * (pLat - lat) + (pLon - lon) * (pLon - lon) < radius * radius) {
					pointsInRadius.add(new Point(point));
				}
			}
		}

		return pointsInRadius;
	}

	@Override
	public List<Point> loadPointsInTrack(Track track) {
		checkEnabled();
		List<Point> list = storage.get(track);
		if (list == null)
			return null;

		return Utils.map(storage.get(track), new Utils.Function<Point, Point>() {
			@Override
			public Point apply(Point point) {
				return new Point(point);
			}
		});
	}

	@Override
	public List<Category> loadCategories() {
		return categories;
	}

	public void disable() {
		isDisabled = true;
	}

	private void checkEnabled() {
		if (isDisabled)
			throw new IllegalStateException();
	}
}
