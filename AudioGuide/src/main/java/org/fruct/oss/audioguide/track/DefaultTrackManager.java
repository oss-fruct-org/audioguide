package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.files.DefaultFileManager;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.util.Utils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultTrackManager implements TrackManager, Closeable {
	private final StorageBackend backend;
	private final CategoriesBackend categoriesBackend;
	private final Database database;
	private final FileManager fileManager;
	private final SharedPreferences pref;

	private List<Category> categories;
	private List<Category> activeCategories;

	private final List<TrackListener> listeners = new ArrayList<TrackListener>();
	private final List<CursorHolder> cursorHolders = new ArrayList<CursorHolder>();

	private final SynchronizerThread synchronizer;

	private Location location = new Location("no-provider");
	private float radius;

	public DefaultTrackManager(Context context, StorageBackend backend, CategoriesBackend catBackend) {
		this.categoriesBackend = catBackend;
		this.backend = backend;

		pref = PreferenceManager.getDefaultSharedPreferences(context);

		fileManager = DefaultFileManager.getInstance();

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
		database.insertPoint(point);
		database.markPointUpdate(point);
		notifyDataChanged();
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

					if (point.getCategoryId() == -1)
						point.setCategoryId(track.getCategoryId());

					if (point.hasAudio()) {
						fileManager.insertRemoteFile("no-title", Uri.parse(point.getAudioUrl()));
					}

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
	public void requestPointsInRadius(final float latitude, final float longitude, boolean autoStore) {
		backend.loadPointsInRadius(latitude, longitude, radius, activeCategories, new Utils.Callback<List<Point>>() {
			@Override
			public void call(List<Point> points) {
				for (Point point : points) {
					database.insertPoint(point);
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
	public void activateTrackMode(Track track) {
		if (track == null)
			pref.edit().remove(PREF_TRACK_MODE).apply();
		else
			pref.edit().putString(PREF_TRACK_MODE, track.getName()).apply();
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
	public Track getTrackByName(String name) {
		if (name == null)
			return null;
		return database.getTrackByName(name);
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
			GetsBackend backend = new GetsBackend();
			instance = new DefaultTrackManager(App.getContext(), backend, backend);
			instance.getCategories();
		}

		return instance;
	}
}
