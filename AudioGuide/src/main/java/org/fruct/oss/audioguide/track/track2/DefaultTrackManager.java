package org.fruct.oss.audioguide.track.track2;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

	private List<Category> categories;
	private List<Category> activeCategories;

	private float lastLat, lastLon, lastRadius;
	private final List<TrackListener> listeners = new ArrayList<TrackListener>();
	private final List<CursorHolder> cursorHolders = new ArrayList<CursorHolder>();

	private final SynchronizerThread synchronizer;

	private Location location = new Location("no-provider");
	private float radius;

	public DefaultTrackManager(Context context, StorageBackend backend, CategoriesBackend catBackend) {
		this.categoriesBackend = catBackend;
		this.backend = backend;
		database = new Database(context);
		synchronizer = new SynchronizerThread(database, backend);
		synchronizer.start();
		synchronizer.initializeHandler();
	}

	@Override
	public void close() {
		database.close();
		synchronizer.interrupt();
	}

	@Override
	public void insertPoint(Point point) {
		database.insertPoint(point, null);
		database.markPointUpdate(point);
		notifyDataChanged();
	}

	@Override
	public void updatePoint(Point newPoint, Point oldPoint) {
		database.insertPoint(newPoint, oldPoint);
		database.markPointUpdate(newPoint);
	}

	@Override
	public void insertTrack(Track track) {
		database.insertTrack(track);
		database.markTrackUpdate(track);
		notifyDataChanged();
	}

	@Override
	public void insertToTrack(Track track, Point point) {
		database.insertToTrack(track, point);
		database.markTrackUpdate(track);
		notifyDataChanged();
	}

	@Override
	public void storeTrackLocal(final Track track) {
		backend.loadPointsInTrack(track, new Utils.Callback<List<Point>>() {
			@Override
			public void call(List<Point> points) {
				track.setLocal(true);
				database.insertTrack(track);

				for (Point point : points) {
					point.setPrivate(track.isPrivate());
					database.insertToTrack(track, point);
				}
				notifyDataChanged();
			}
		});
	}

	@Override
	public void requestTracksInRadius() {
		backend.loadTracksInRadius((float) location.getLatitude(), (float) location.getLongitude(), radius, activeCategories, new Utils.Callback<List<Track>>() {
			@Override
			public void call(List<Track> tracks) {
				for (Track track : tracks) {
					track.setLocal(false);
					database.insertTrack(track);
				}

				notifyDataChanged();
			}
		});
	}

	@Override
	public void requestPointsInRadius(final float latitude, final float longitude, float radius, boolean autoStore) {
		lastLat = latitude;
		lastLon = longitude;
		lastRadius = radius;
		backend.loadPointsInRadius(latitude, longitude, radius, activeCategories, new Utils.Callback<List<Point>>() {
			@Override
			public void call(List<Point> points) {
				for (Point point : points) {
					database.insertPoint(point, null);
				}

				notifyDataChanged();
			}
		});
	}

	@Override
	public void requestPointsInTrack(final Track track) {
		backend.loadPointsInTrack(track, new Utils.Callback<List<Point>>() {
			@Override
			public void call(List<Point> points) {
				if (points == null)
					return;

				for (Point point : points) {
					point.setPrivate(track.isPrivate());
					database.insertToTrack(track, point);
				}

				notifyDataChanged();
			}
		});
	}

	@Override
	public void addListener(TrackListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(TrackListener listener) {
		listeners.remove(listener);
	}

	@Override
	public CursorHolder loadTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadTracksCursor();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadPrivateTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPrivateTracks();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadLocalPoints() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPointsCursor();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;

	}

	@Override
	public CursorHolder loadPoints(final Track track) {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPointsCursor(track);
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadRelations() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadRelationsCursor();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public void updateUserLocation(Location location) {
		this.location = location;
	}

	@Override
	public void updateLoadRadius(float radius) {
		this.radius = radius;
	}

	@Override
	public Model<Track> getLocalTracksModel() {
		return localTrackModel;
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
	public List<Category> getCategories() {
		if (categories == null) {
			categories = database.getCategories();
			activeCategories = database.getActiveCategories();
			loadRemoteCategories();
		}

		return categories;
	}

	@Override
	public void setCategoryState(Category category, boolean isActive) {
		category.setActive(isActive);
		database.setCategoryState(category);

		for (Category cat : categories) {
			if (category.getId() == cat.getId()) {
				cat.setActive(isActive);
			}
		}

		activeCategories = database.getActiveCategories();
		requestTracksInRadius();
	}

	private void loadRemoteCategories() {
		categoriesBackend.loadCategories(new Utils.Callback<List<Category>>() {
			@Override
			public void call(List<Category> categories) {
				DefaultTrackManager.this.categories = categories;
				database.updateCategories(categories);
				activeCategories = database.getActiveCategories();
				requestTracksInRadius();

			}
		});
	}

	private void notifyDataChanged() {
		for (TrackListener listener : listeners) {
			listener.onDataChanged();
		}

		reQueryCursorHolders();
	}

	private CursorHolder addCursorHolder(CursorHolder cursorHolder) {
		cursorHolders.add(cursorHolder);
		return cursorHolder;
	}

	private void reQueryCursorHolders() {
		for (Iterator<CursorHolder> iterator = cursorHolders.iterator(); iterator.hasNext(); ) {
			CursorHolder holder = iterator.next();

			if (holder.isClosed()) {
				iterator.remove();
				continue;
			}

			holder.queryAsync();
		}
	}


	private static DefaultTrackManager instance;
	public synchronized static TrackManager getInstance() {
		if (instance == null) {
			TestStorageBackend storage = createTestBackend();
			GetsBackend backend = new GetsBackend();
			instance = new DefaultTrackManager(App.getContext(), backend, backend);
			instance.getCategories();
		}

		return instance;
	}

	private static TestStorageBackend createTestBackend() {
		TestStorageBackend backend = new TestStorageBackend();

		Track track = new Track("AAA", "BBB", "CCC");
		track.setCategoryId(1);
		ArrayList<Point> points = new ArrayList<Point>() {{
			add(new Point("MMM1", "NNN1", "", 0f, 0f));
			add(new Point("MMM2", "NNN2", "", 10f, 10f));
			add(new Point("MMM3", "NNN3", "", -5f, -1f));
			add(new Point("MMM4", "NNN4", "", 0f, 1f));
			add(new Point("PTZ", "Petrozavodsk", "", 61.783f, 34.35f));
		}};

		Track category = new Track("ca_audio.shops", "Other", "CCC");
		category.setCategoryId(2);
		ArrayList<Point> points2 = new ArrayList<Point>() {{
			add(new Point("Free", "Free", "", 61.785f, 34.356f));
		}};

		backend.updateTrack(track, points);
		backend.updateTrack(category, points2);

		return backend;
	}

}
