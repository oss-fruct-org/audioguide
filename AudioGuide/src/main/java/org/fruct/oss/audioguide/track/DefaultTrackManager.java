package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.config.Config;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.util.Utils;

import java.io.Closeable;
import java.io.IOException;
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
	private final Refresher refresher;

	private Location location = new Location("no-provider");
	private float radius;
	private boolean isClosed;

	public DefaultTrackManager(Context context, StorageBackend backend, CategoriesBackend catBackend) {
		this.categoriesBackend = catBackend;
		this.backend = backend;

		pref = PreferenceManager.getDefaultSharedPreferences(context);

		fileManager = FileManager.getInstance();
		database = new Database(context);

		synchronizeFileManager();

		refresher = new Refresher(context, database, this);

		if (!Config.isEditLocked()) {
			synchronizer = new SynchronizerThread(database, backend);
			synchronizer.start();
			synchronizer.initializeHandler();
		} else {
			synchronizer = null;
		}
	}

	@Override
	public synchronized void close() {
		if (synchronizer != null) {
			synchronizer.interrupt();
			synchronizer.quit();
		}

		if (backend instanceof Closeable) {
			try {
				((Closeable) backend).close();
			} catch (IOException ignored) {
			}
		}

		database.close();
		isClosed = true;
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
	public void insertToTrack(Track track, Point point, int selectedPosition) {
		database.insertToTrack(track, point, selectedPosition);
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
						fileManager.requestTransfer(point.getAudioUrl(), FileSource.Variant.FULL);
						fileManager.requestDownload(point.getAudioUrl(), FileSource.Variant.FULL, FileManager.Storage.PERSISTENT);
					}
				}

				database.insertPointsToTrack(track, points);
				notifyDataChanged();
			}
		});
	}

	@Override
	public void requestTracksInRadius() {
		if (categories == null)
			loadRemoteCategories();

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
		if (categories == null)
			loadRemoteCategories();

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
					point.setCategoryId(track.getCategoryId());
				}

				database.insertPointsToTrack(track, points);

				notifyDataChanged();
			}
		});
	}

	@Override
	public void requestPointsCleanup() {
		database.cleanupPoints(location, radius);
		notifyDataChanged();
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
	public CursorHolder loadLocalTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadLocalTracks();
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
		refresher.updateUserLocation(location);
	}

	@Override
	public void updateLoadRadius(float radius) {
		this.radius = radius * 1000;
		refresher.updateLoadRadius(radius * 1000);
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
	public List<String> getPointPhotos(Point point) {
		Cursor photoCursor = database.loadPointPhotos(point);
		ArrayList<String> ret = new ArrayList<String>(photoCursor.getCount());
		while (photoCursor.moveToNext()) {
			ret.add(photoCursor.getString(0));
		}
		photoCursor.close();
		return ret;
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
		//Gets.getInstance().setEnv("categories", activeCategories);
		notifyDataChanged();
	}

	@Override
	public void deleteTrack(Track track, boolean deleteFromServer) {
		if (deleteFromServer && track.isPrivate()) {
			backend.deleteTrack(track, new Utils.Callback<Track>() {
				@Override
				public void call(Track track) {
					database.deleteTrack(track);
					notifyDataChanged();
				}
			});
		} else {
			database.deleteTrack(track);
		}
	}

	@Override
	public void synchronizeFileManager() {
		List<String> localUrls = database.loadLocalAudioUrls();
		fileManager.retainPersistentUrls(localUrls);
	}

	private void loadRemoteCategories() {
		categoriesBackend.loadCategories(new Utils.Callback<List<Category>>() {
			@Override
			public void call(List<Category> categories) {
				DefaultTrackManager.this.categories = categories;
				database.updateCategories(categories);
				activeCategories = database.getActiveCategories();
				//requestTracksInRadius();

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
		if (instance == null || instance.isClosed) {
			GetsBackend backend = new GetsBackend();
			instance = new DefaultTrackManager(App.getContext(), backend, backend);
			instance.getCategories();
		}

		return instance;
	}
}
