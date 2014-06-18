package org.fruct.oss.audioguide.track.track2;

import android.content.Context;
import android.os.AsyncTask;

import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

public class DefaultTrackManager implements TrackManager, Closeable {
	private final StorageBackend backend;
	private final CategoriesBackend categoriesBackend;
	private final Database database;

	private final BaseModel<Track> localTrackModel = new BaseModel<Track>();

	private final BaseModel<Track> remoteTrackModel = new BaseModel<Track>();
	private final BaseModel<Point> remotePointModel = new BaseModel<Point>();

	private final HashMap<Track, Reference<Model<Point>>> pointModels = new HashMap<Track, Reference<Model<Point>>>();

	private boolean cacheDirty = false;

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
		cacheDirty = true;
		database.insertPoint(point);
	}

	@Override
	public void insertTrack(Track track) {
		cacheDirty = true;

		database.insertTrack(track);
	}

	@Override
	public void insertToTrack(Track track, Point point) {
		cacheDirty = true;

		database.insertToTrack(track, point);
	}

	@Override
	public void requestTracksInRadius(float radius) {
		AsyncTask<Float, Void, List<Track>> at = new AsyncTask<Float, Void, List<Track>>() {
			@Override
			protected List<Track> doInBackground(Float... floats) {
				float radius = floats[0];
				return backend.loadTracksInRadius(0, 0, radius);
			}

			@Override
			protected void onPostExecute(List<Track> tracks) {
				remoteTrackModel.setData(tracks);
			}
		};
		at.execute(radius);
	}

	@Override
	public void requestPointsInRadius(float radius) {
		AsyncTask<Float, Void, List<Point>> at = new AsyncTask<Float, Void, List<Point>>() {
			@Override
			protected List<Point> doInBackground(Float... floats) {
				float radius = floats[0];
				return backend.loadPointsInRadius(0, 0, radius);
			}

			@Override
			protected void onPostExecute(List<Point> tracks) {
				remotePointModel.setData(tracks);
			}
		};
		at.execute(radius);
	}

	@Override
	public void requestPointsInTrack(Track track) {
		AsyncTask<Track, Void, List<Point>> at = new AsyncTask<Track, Void, List<Point>>() {
			@Override
			protected List<Point> doInBackground(Track... tracks) {
				return backend.loadPointsInTrack(tracks[0]);
			}

			@Override
			protected void onPostExecute(List<Point> tracks) {
				remotePointModel.setData(tracks);
			}
		};
		at.execute(track);
	}

	@Override
	public Model<Track> getTracksModel() {
		refreshCache();
		return localTrackModel;
	}


	@Override
	public Model<Track> getRemoteTracksModel() {
		refreshCache();
		return remoteTrackModel;
	}

	@Override
	public Model<Point> getRemotePointsModel() {
		refreshCache();
		return remotePointModel;
	}

	@Override
	public Model<Point> getTrackPointsModel(Track track) {
		Reference<Model<Point>> modelRef = pointModels.get(track);
		Model<Point> pointModel;
		if (modelRef == null || modelRef.get() == null) {
			BaseModel<Point> newPointModel = new BaseModel<Point>();
			newPointModel.setData(database.loadPoints(track));
			pointModels.put(track, new WeakReference<Model<Point>>(newPointModel));
			pointModel = newPointModel;
		} else {
			pointModel = modelRef.get();
		}

		return pointModel;
	}

	@Override
	public Model<Point> getCategoryPointsModel(Category category) {
		return null;
	}


	@Override
	public List<Category> getCategories() {
		return categoriesBackend.loadCategories();
	}

	private void refreshCache() {
		if (!cacheDirty)
			return;

		cacheDirty = false;
		localTrackModel.setData(database.loadTracks());
	}
}
