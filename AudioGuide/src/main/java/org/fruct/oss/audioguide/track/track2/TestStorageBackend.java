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
	public HashMap<Track, List<Point>> storage;

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
	public List<Track> loadTracksInRadius(float lat, float lon, float radius) {
		List<Track> tracksInRadius = new ArrayList<Track>();

		for (Map.Entry<Track, List<Point>> trackEntry: storage.entrySet()) {
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

	@Override
	public List<Point> loadPointsInRadius(float lat, float lon, float radius) {
		List<Point> pointsInRadius = new ArrayList<Point>();

		for (Map.Entry<Track, List<Point>> trackEntry: storage.entrySet()) {
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
		return new ArrayList<Category>() {{
			add(new Category(1, "audio.other", "Description", "Url"));
			add(new Category(2, "audio.shops", "Shops", "Url"));
		}};
	}
}
