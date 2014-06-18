package org.fruct.oss.audioguide.track.track2;

import android.content.Context;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class DefaultTrackManager implements TrackManager, Closeable {
	private final StorageBackend backend;
	private final CategoriesBackend categoriesBackend;
	private final Database database;

	public DefaultTrackManager(Context context, StorageBackend backend, CategoriesBackend catBackend) {
		this.categoriesBackend = catBackend;
		this.backend = backend;
		database = new Database(context);
	}

	@Override
	public void close() {
		database.close();
	}

	@Override
	public void insertPoint(Point point) {
		database.insertPoint(point);
	}

	@Override
	public void insertTrack(Track track) {
		database.insertTrack(track);
	}

	@Override
	public void insertToTrack(Track track, Point point) {
		database.insertToTrack(track, point);
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
		return categoriesBackend.loadCategories();
	}

}
