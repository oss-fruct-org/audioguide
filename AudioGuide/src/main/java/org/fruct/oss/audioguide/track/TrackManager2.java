package org.fruct.oss.audioguide.track;

import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.gets.CategoriesRequest;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.FilterModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.parsers.CategoriesContent;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.Object;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Marks methods in TrackManager that can modify data storage and from that should
 * be called methods of "notify*" group
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
@interface DatasetModifier {
}

public class TrackManager2 {
	private final static Logger log = LoggerFactory.getLogger(TrackManager2.class);

	public static final String PREF_EDITING_TRACK = "pref-editing-track-id";

	public static interface Listener {
		void tracksUpdated();
		void pointsUpdated(Track track);
	}

	// Storages
	private final ILocalStorage localStorage;
	private final IStorage remoteStorage;

	private volatile boolean isInitialized = false;

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private FileManager fileManager;

	// Models
	private BaseModel<Track> allTracksModel;
	private FilterModel<Track> activeTracksModel;
	private FilterModel<Track> localTracksModel;


	private BaseModel<Point> freePointsModel;

	// All known tracks from local and remote storage
	private final Map<String, Track> allTracks = new HashMap<String, Track>();
	private final Map<Track, BaseModel<Point>> pointModels = new HashMap<Track, BaseModel<Point>>();

	// Categories
	private List<Category> categories;

	private List<Listener> listeners = new ArrayList<Listener>();

	public TrackManager2(ILocalStorage localStorage, IStorage remoteStorage) {
		this.localStorage = localStorage;
		this.remoteStorage = remoteStorage;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public void initialize() {
		if (isInitialized)
			throw new IllegalStateException("TrackManager already initialized");

		fileManager = FileManager.getInstance();

		// TODO: storages should be closed
		localStorage.initialize();
		remoteStorage.initialize();

		localStorage.loadAsync(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				for (Track track : localStorage.getTracks()) {
					track.setLocal(true);
					allTracks.put(track.getId(), track);
					notifyTracksUpdated();
				}
			}
		});

		isInitialized = true;

		allTracksModel = new BaseModel<Track>();
		activeTracksModel = new ActiveTracksFilterModel(allTracksModel);
		localTracksModel = new LocalTracksFilterModel(allTracksModel);
	}

	public void destroy() {
		localStorage.close();
		remoteStorage.close();

		activeTracksModel.close();
		localTracksModel.close();
	}

	public void loadCategories() {
		Gets gets = Gets.getInstance();
		gets.addRequest(new CategoriesRequest(gets) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				// TODO: need some code to re-download categories after network up
				if (response.getCode() != 0) {
					loadCachedCategories();
					return;
				}

				synchronized (localStorage) {
					categories = CategoriesContent.filterByPrefix(((CategoriesContent)
							response.getContent()).getCategories());
					if (localStorage instanceof DatabaseStorage) {
						((DatabaseStorage) localStorage).updateCategories(categories);
					}
				}
			}

			@Override
			protected void onError() {
				super.onError();
				loadCachedCategories();
			}
		});
	}

	public void setCategoryState(Category category, boolean isActive) {
		DatabaseStorage storage = (DatabaseStorage) localStorage;
		category.setActive(isActive);
		storage.setCategoryState(category);
	}

	private void loadCachedCategories() {
		if (localStorage instanceof DatabaseStorage) {
			new AsyncTask<Void, Void, List<Category>>() {
				@Override
				protected List<Category> doInBackground(Void... voids) {
					synchronized (localStorage) {
						return ((DatabaseStorage) localStorage).getCategories();
					}
				}

				@Override
				protected void onPostExecute(List<Category> loadedCategories) {
					synchronized (localStorage) {
						categories = loadedCategories;
					}
				}
			}.execute();
		}
	}

	public List<Category> getCategories() {
		return categories;
	}

	/**
	 * Method starts loading tracks from remote storage and immediately returns without blocking
	 */
	public void loadRemoteTracks() {
		log.trace("loadRemoteTracks");
		checkInitialized();

		remoteStorage.loadAsync(new Handler() {
			@Override
			public void handleMessage(Message msg) {
				ArrayList<Track> tracks = msg.getData().getParcelableArrayList("tracks");
				processLoadedRemoteTracks(tracks);
			}
		});
	}

	public void refreshPoints(final Track track) {
		remoteStorage.loadPoints(track, new Handler() {
			@Override
			public void handleMessage(Message msg) {
				ArrayList<Point> points = msg.getData().getParcelableArrayList("points");
				processLoadedRemotePoints(track, points);
			}
		});
	}

	public synchronized void addListener(Listener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public List<Track> getActiveTracks() {
		Collection<Track> allTracks = this.allTracks.values();
		List<Track> tracks = Utils.select(allTracks, new Utils.Predicate<Track>() {
			@Override
			public boolean apply(Track track) {
				return track.isActive();
			}
		});
		Collections.sort(tracks);
		return tracks;
	}

	public Model<Track> getTracksModel() {
		return allTracksModel;
	}

	public Model<Track> getActiveTracksModel() {
		return activeTracksModel;
	}

	public Model<Track> getLocalTracksModel() {
		return localTracksModel;
	}

	public Model<Point> getPointsModel(Track track) {
		if (track == null) {
			if (freePointsModel == null) {
				freePointsModel = new BaseModel<Point>();
				freePointsModel.setData(getPoints(null));
			}

			return freePointsModel;
		} else {
			BaseModel<Point> model = pointModels.get(track);
			if (model == null) {
				model = new BaseModel<Point>();
				model.setData(getPoints(track));
				pointModels.put(track, model);
			}

			return model;
		}
	}

	public Track getEditingTrack() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String id = pref.getString(PREF_EDITING_TRACK, null);

		if (id != null) {
			Track track = allTracks.get(id);
			if (track != null && track.isLocal())
				return track;
			else
				return null;
		} else {
			return null;
		}
	}

	public void setEditingTrack(Track track) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());

		if (track == null) {
			pref.edit().remove(PREF_EDITING_TRACK).apply();
			return;
		}

		// TODO: add check for edit availability
		if (track.isLocal()) {
			pref.edit().putString(PREF_EDITING_TRACK, track.getId()).apply();
		}
	}

	private List<Point> getPoints(Track track) {
		return new ArrayList<Point>(localStorage.getPoints(track));
	}

	@DatasetModifier
	public void storeLocal(final Track track) {
		checkInitialized();

		track.setLocal(true);
		allTracks.put(track.getId(), track);

		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}

		notifyTracksUpdated();
	}

	@DatasetModifier
	public void activateTrack(Track track) {
		if (track.isLocal()) {
			track.setActive(true);
			allTracks.put(track.getId(), track);
			localStorage.storeLocalTrack(track);
			notifyTracksUpdated();
		}
	}

	@DatasetModifier
	public void deactivateTrack(Track track) {
		if (track.isLocal()) {
			track.setActive(false);
			allTracks.put(track.getId(), track);
			localStorage.storeLocalTrack(track);
			notifyTracksUpdated();
		}
	}

	@DatasetModifier
	public void storePoints(Track track, List<Point> points) {
		localStorage.storeLocalPoints(track, points);
	}

	@DatasetModifier
	public void storePoint(Track track, Point point) {
		if (track != null)
			log.trace("Store point to track " + track.getName() + ": " + point.getName());

		localStorage.updatePoint(track, point);

		if (point.hasPhoto()) {
			fileManager.insertImageUri(Uri.parse(point.getPhotoUrl()));
		}

		if (point.hasAudio()) {
			fileManager.insertAudioUri(Uri.parse(point.getAudioUrl()).toString());
		}

		notifyPointsUpdated(track);
	}

	@DatasetModifier
	public void sendPoint(Track track, Point point) {
		if (remoteStorage instanceof IRemoteStorage) {
			((IRemoteStorage) remoteStorage).sendPoint(track, point);
		}
	}

	@DatasetModifier
	public void sendTrack(final Track track) {
		if (remoteStorage instanceof IRemoteStorage) {
			List<Point> points = localStorage.getPoints(track);
			((IRemoteStorage) remoteStorage).sendTrack(track, points);
		}
	}

	public void updateUserLocation(Location location) {
		if (remoteStorage instanceof GetsStorage) {
			((GetsStorage) remoteStorage).updateUserLocation(location);
		}
	}

	public void updateLoadRadius(int radius) {
		if (remoteStorage instanceof GetsStorage) {
			((GetsStorage) remoteStorage).updateLoadRadius(radius);
		}
	}


	private void processLoadedRemoteTracks(List<Track> tracks) {
		synchronized (allTracks) {
			for (Iterator<Map.Entry<String, Track>> iter = allTracks.entrySet().iterator(); iter.hasNext();) {
				Track track = iter.next().getValue();
				if (!track.isLocal()) {
					iter.remove();
				}
			}

			for (Track track : tracks) {
				if (!allTracks.containsKey(track.getId()))
					allTracks.put(track.getId(), track);
			}
		}

		notifyTracksUpdated();
	}

	@DatasetModifier
	private void processLoadedRemotePoints(Track track, ArrayList<Point> points) {
		if (points == null || points.isEmpty())
			return;

		localStorage.storeLocalTrack(track);
		localStorage.storeLocalPoints(track, points);

		for (Point point : points) {
			if (point.hasPhoto()) {
				fileManager.insertImageUri(Uri.parse(point.getPhotoUrl()));
			}

			if (point.hasAudio()) {
				fileManager.insertAudioUri(Uri.parse(point.getAudioUrl()).toString());
			}
		}

		notifyTracksUpdated();
		notifyPointsUpdated(track);
	}

	private synchronized void notifyPointsUpdated(Track track) {
		for (Listener listener : listeners)
			listener.pointsUpdated(track);

		BaseModel<Point> pointModel = track == null ? freePointsModel : pointModels.get(track);

		if (pointModel != null) {
			pointModel.setData(localStorage.getPoints(track));
		}
	}

	private synchronized void notifyTracksUpdated() {
		for (Listener listener : listeners)
			listener.tracksUpdated();

		allTracksModel.setData(allTracks.values());
	}

	private void checkInitialized() {
		if (!isInitialized)
			throw new IllegalStateException("TrackManager not initialized");
	}

	private static TrackManager2 instance;
	public synchronized static TrackManager2 getInstance() {
		if (instance != null)
			return instance;

		ILocalStorage localStorage = new DatabaseStorage(App.getContext());
		GetsStorage getsStorage = new GetsStorage();

		instance = new TrackManager2(localStorage, getsStorage);
		instance.initialize();
		instance.loadCategories();
		instance.loadRemoteTracks();

		return instance;
	}

	public void waitTasks() {
		final Object lock = new Object();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (lock) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					synchronized (lock) {
						lock.notify();
					}
				}
			});

			try {
				lock.wait(5000);
			} catch (InterruptedException e) {
				throw new RuntimeException("Timeout");
			}
		}
	}

	private static class ActiveTracksFilterModel extends FilterModel<Track> {
		public ActiveTracksFilterModel(Model<Track> baseModel) {
			super(baseModel);
		}

		@Override
		public boolean check(Track track) {
			return track.isActive();
		}
	}

	private static class LocalTracksFilterModel extends FilterModel<Track> {
		public LocalTracksFilterModel(Model<Track> baseModel) {
			super(baseModel);
		}

		@Override
		public boolean check(Track track) {
			return track.isLocal();
		}
	}
}
