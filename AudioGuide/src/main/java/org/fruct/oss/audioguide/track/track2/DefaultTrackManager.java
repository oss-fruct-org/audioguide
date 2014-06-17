package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.util.List;

public class DefaultTrackManager implements TrackManager {
	private final StorageBackend backend;

	public DefaultTrackManager(StorageBackend backend) {
		this.backend = backend;
	}

	@Override
	public void insertPoint(Point point) {

	}

	@Override
	public void insertTrack(Track track) {

	}

	@Override
	public void insertToTrack(Track track, Point point) {

	}

	@Override
	public void requestTracksInRadius(float radius) {

	}

	@Override
	public void requestPointsInRadius(float radius) {

	}

	@Override
	public void requestPointsInTrack(Track track) {

	}

	@Override
	public Model<Track> getTracksModel() {
		return null;
	}


	@Override
	public Model<Track> getRemoteTracksModel() {
		return null;
	}

	@Override
	public Model<Point> getRemotePointsModel() {
		return null;
	}


	@Override
	public Model<Point> getTrackPointsModel(Track category) {
		return null;
	}

	@Override
	public Model<Point> getCategoryPointsModel(Category track) {
		return null;
	}

	@Override
	public List<Category> getCategories() {
		return null;
	}
}
