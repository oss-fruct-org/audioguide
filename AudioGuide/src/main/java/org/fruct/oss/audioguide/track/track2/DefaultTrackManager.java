package org.fruct.oss.audioguide.track.track2;

import android.content.Context;
import android.os.AsyncTask;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;

import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultTrackManager implements TrackManager, Closeable {
	private final StorageBackend backend;
	private final CategoriesBackend categoriesBackend;
	private final Database database;

	private final BaseModel<Track> tracksModel = new BaseModel<Track>();

	private final BaseModel<Track> localTrackModel = new BaseModel<Track>();

	private final BaseModel<Track> remoteTrackModel = new BaseModel<Track>();
	private final BaseModel<Point> remotePointModel = new BaseModel<Point>();

	private final BaseModel<Point> localPointModel = new BaseModel<Point>();
	private final HashMap<Track, Reference<Model<Point>>> pointModels = new HashMap<Track, Reference<Model<Point>>>();

	private boolean cacheDirty = true;

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
	public void storeTrackLocal(final Track track) {
		new AsyncTask<Void, Void, List<Point>>() {
			@Override
			protected List<Point> doInBackground(Void... voids) {
				List<Point> points = backend.loadPointsInTrack(track);

				database.insertTrack(track);
				for (Point point : points) {
					database.insertToTrack(track, point);
				}
				track.setLocal(true);
				localTrackModel.insertElement(track);
				refreshTracksModel();

				return points;
			}
		}.execute();
	}

	@Override
	public void requestTracksInRadius(final float latitude, final float longitude, float radius) {
		new AsyncTask<Float, Void, List<Track>>() {
			@Override
			protected List<Track> doInBackground(Float... floats) {
				float radius = floats[0];
				return backend.loadTracksInRadius(latitude, longitude, radius);
			}

			@Override
			protected void onPostExecute(List<Track> tracks) {
				remoteTrackModel.setData(tracks);
				refreshTracksModel();
			}
		}.execute(radius);
	}

	@Override
	public void requestPointsInRadius(final float latitude, final float longitude, float radius) {
		new AsyncTask<Float, Void, List<Point>>() {
			@Override
			protected List<Point> doInBackground(Float... floats) {
				float radius = floats[0];
				return backend.loadPointsInRadius(latitude, longitude, radius);
			}

			@Override
			protected void onPostExecute(List<Point> tracks) {
				remotePointModel.setData(tracks);
			}
		}.execute(radius);
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
		return tracksModel;
	}

	@Override
	public Model<Track> getLocalTracksModel() {
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
	public Model<Point> getPointsModel() {
		refreshCache();
		return localPointModel;
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

	private void refreshTracksModel() {
		Set<String> trackNames = new HashSet<String>();
		ArrayList<Track> tracks = new ArrayList<Track>();

		for (Track track : localTrackModel) {
			if (trackNames.contains(track.getName()))
				continue;
			tracks.add(track);
			trackNames.add(track.getName());
		}

		for (Track track : remoteTrackModel) {
			if (trackNames.contains(track.getName()))
				continue;
			tracks.add(track);
			trackNames.add(track.getName());
		}

		tracksModel.setData(tracks);
	}

	private void refreshPointsModel() {
		localPointModel.setData(database.loadPoints());
	}

	private void refreshCache() {
		if (!cacheDirty)
			return;

		cacheDirty = false;
		localTrackModel.setData(database.loadTracks());
		refreshTracksModel();
		refreshPointsModel();
	}

	private static TrackManager instance;
	public synchronized static TrackManager getInstance() {
		if (instance == null) {
			TestStorageBackend storage = createTestBackend();
			instance = new DefaultTrackManager(App.getContext(), storage, storage);
		}

		return instance;
	}

	private static TestStorageBackend createTestBackend() {
		TestStorageBackend backend = new TestStorageBackend();

		Track track = new Track("AAA", "BBB", "CCC");
		ArrayList<Point> points = new ArrayList<Point>() {{
			add(new Point("MMM1", "NNN1", "", 0f, 0f));
			add(new Point("MMM2", "NNN2", "", 10f, 10f));
			add(new Point("MMM3", "NNN3", "", -5f, -1f));
			add(new Point("MMM4", "NNN4", "", 0f, 1f));
			add(new Point("PTZ", "Petrozavodsk", "", 61.783f, 34.35f));
		}};

		Track category = new Track("ca_other", "Other", "CCC");

		backend.updateTrack(track, points);
		backend.updateTrack(category, points);

		return backend;
	}

}
