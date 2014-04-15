package org.fruct.oss.audioguide.track;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.util.Downloader;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Target;
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

class IconCache extends LruCache<String, Bitmap> {
	public IconCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1)
			return 32;
		else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			return value.getByteCount() / 1024;
		else
			return value.getAllocationByteCount() / 1024;
	}
}

abstract class FilterModel<T> implements Model<T> {
	public abstract boolean check(T t);

	public ArrayList<T> list = new ArrayList<T>();
	public ArrayList<ModelListener> listeners = new ArrayList<ModelListener>();
	private Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public T getItem(int position) {
		return list.get(position);
	}

	@Override
	public synchronized void addListener(ModelListener listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(ModelListener listener) {
		listeners.remove(listener);
	}

	public void setData(Collection<T> coll) {
		list.clear();
		for (T t : coll) {
			if (check(t)) {
				list.add(t);
			}
		}

		handler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}

	public void notifyDataSetChanged() {
		for (ModelListener listener : listeners) {
			listener.dataSetChanged();
		}
	}
}

public class TrackManager {
	private final static Logger log = LoggerFactory.getLogger(TrackManager.class);

	public static final String PREF_EDITING_TRACK = "pref-editing-track-id";

	public static interface Listener {
		void tracksUpdated();
		void pointsUpdated(Track track);
	}

	private final Downloader iconDownloader;
	private final LruCache<String, Bitmap> iconCache;

	private final ILocalStorage localStorage;
	private final IStorage remoteStorage;

	private volatile boolean isInitialized = false;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	// All known tracks from local and remote storage
	private final Map<String, Track> allTracks = new HashMap<String, Track>();
	private final Map<Track, FilterModel<Point>> pointModels = new HashMap<Track, FilterModel<Point>>();

	private List<Listener> listeners = new ArrayList<Listener>();

	public TrackManager(ILocalStorage localStorage, IStorage remoteStorage) {
		this.localStorage = localStorage;
		this.remoteStorage = remoteStorage;
		this.iconDownloader = new Downloader(App.getContext(), "point-icons");
		this.iconCache = new IconCache(1024);
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public void initialize() {
		if (isInitialized)
			throw new IllegalStateException("TrackManager already initialized");

		localStorage.initialize();
		remoteStorage.initialize();

		// TODO: storages should be closed
		localStorage.load();

		for (Track track : localStorage.getTracks()) {
			track.setLocal(true);
			allTracks.put(track.getId(), track);
		}

		isInitialized = true;
	}

	public void destroy() {
		localStorage.close();
		remoteStorage.close();
	}

	/**
	 * Method starts loading tracks from remote storage and immediately returns without blocking
	 */
	public void loadRemoteTracks() {
		log.trace("loadRemoteTracks");
		checkInitialized();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				doLoadRemoteTracks();
			}
		});
	}


	public void refresh() {
		loadRemoteTracks();
	}

	public synchronized void addListener(Listener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public List<Track> getTracks() {
		ArrayList<Track> tracks = new ArrayList<Track>(allTracks.values());
		Collections.sort(tracks);
		return tracks;
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
		FilterModel<Point> model = pointModels.get(track);
		if (model == null) {
			model = new FilterModel<Point>() {
				@Override
				public boolean check(Point point) {
					return true;
				}
			};

			model.setData(getPoints(track));
			pointModels.put(track, model);
		}

		return model;
	}

	public Track getEditingTrack() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String id = pref.getString(PREF_EDITING_TRACK, null);

		if (id != null) {
			return allTracks.get(id);
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

	public List<Point> getPoints(Track track) {
		return new ArrayList<Point>(localStorage.getPoints(track));
	}

	public Bitmap getPointIconBitmap(Point point) {
		if (point.hasPhoto()) {
			Bitmap bitmap = iconCache.get(point.getPhotoUrl());
			if (bitmap != null)
				return bitmap;

			Uri remotePhotoUri = Uri.parse(point.getPhotoUrl());
			Uri localPhotoUri = iconDownloader.getUri(remotePhotoUri);

			if (localPhotoUri != null && !localPhotoUri.equals(remotePhotoUri)) {
				String localPhotoPath = localPhotoUri.getPath();
				Bitmap newBitmap = BitmapFactory.decodeFile(localPhotoPath);
				iconCache.put(point.getPhotoUrl(), newBitmap);
				return newBitmap;
			}
		}
		return null;
	}

	@DatasetModifier
	public void storeLocal(final Track track) {
		checkInitialized();

		boolean isWasRemote = !track.isLocal();
		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}

		track.setLocal(true);
		allTracks.put(track.getId(), track);

		// Don't download track again
		if (isWasRemote) {
			synchronized (allTracks) {
				if (localStorage.getPoints(track).isEmpty()) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							doLoadRemotePoints(track);
						}
					});
				}
			}
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
		log.trace("Store point to track " + track.getName() + ": " + point.getName());
		localStorage.storePoint(track, point);
	}

	@DatasetModifier
	public void sendPoint(Track track, Point point) {
		if (remoteStorage instanceof IRemoteStorage) {
			((IRemoteStorage) remoteStorage).sendPoint(track, point);
		}
	}

	@DatasetModifier
	public void sendTrack(Track track) {
		if (remoteStorage instanceof IRemoteStorage) {
			List<Point> points = localStorage.getPoints(track);
			((IRemoteStorage) remoteStorage).sendTrack(track, points);
		}
	}

	@DatasetModifier
	private void doLoadRemoteTracks() {
		remoteStorage.load();

		List<Track> tracks = remoteStorage.getTracks();
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
	private void doLoadRemotePoints(final Track track) {
		List<Point> points = remoteStorage.getPoints(track);
		if (points == null || points.isEmpty())
			return;

		localStorage.storeLocalPoints(track, points);
		for (Point point : points) {
			if (point.hasPhoto()) {
				iconDownloader.insertUri(Uri.parse(point.getPhotoUrl()));
			}
		}
		notifyPointsUpdated(track);
	}

	private synchronized void notifyPointsUpdated(Track track) {
		for (Listener listener : listeners)
			listener.pointsUpdated(track);

		FilterModel<Point> pointModel = pointModels.get(track);

		if (pointModel != null) {
			pointModel.notifyDataSetChanged();
		}
	}

	private synchronized void notifyTracksUpdated() {
		for (Listener listener : listeners)
			listener.tracksUpdated();

		allTracksModel.setData(allTracks.values());
		activeTracksModel.setData(allTracks.values());
		localTracksModel.setData(allTracks.values());
	}

	private synchronized void notifyTrackUpdated(Track track) {

	}

	private void checkInitialized() {
		if (!isInitialized)
			throw new IllegalStateException("TrackManager not initialized");
	}

	private static TrackManager instance;
	public static TrackManager getInstance() {
		if (instance != null)
			return instance;

		ILocalStorage localStorage = new DatabaseStorage(App.getContext());
		Track track1 = new Track("Empty track", "Empty track description", "CCC");
		Track track2 = new Track("Simple track", "Simple track description", "EEE");
		ArrayStorage remoteStorage = new ArrayStorage()
				.insert(track1)
				.insert(track2)
				.insert(new Point("PetrSu", "Petrosavodsk state university",
						"http://kappa.cs.karelia.ru/~ivashov/audioguide/DescenteInfinie.ogg",
						"http://kappa.cs.karelia.ru/~ivashov/audioguide/petrsu.png",
						61.786616, 34.352004), track2)
				.insert(new Point("Vokzal", "Petrosavodsk vokzal", "", 61.784699, 34.345883), track2)
				.insert(new Point("Neglinlka", "River neglinka", "", 61.777575, 34.355340), track2);


		for (int i = 0; i < 10; i++) {
			remoteStorage.insert(new Point("PetrSu " + i, "Petrosavodsk state university",
					"http://kappa.cs.karelia.ru/~ivashov/audioguide/DescenteInfinie.ogg",
					"http://kappa.cs.karelia.ru/~ivashov/audioguide/petrsu.png",
					61.786616, 34.352004), track2)
					.insert(new Point("Vokzal " + i, "Petrosavodsk vokzal", "", 61.784699,34.345883), track2)
					.insert(new Point("Neglinlka " + i, "River neglinka", "", "http://kappa.cs.karelia.ru/~ivashov/audioguide/reka1.jpg", 61.777575, 34.355340), track2);

		}

		GetsStorage getsStorage = new GetsStorage();

		instance = new TrackManager(localStorage, getsStorage);
		instance.initialize();
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

	private FilterModel<Track> allTracksModel = new FilterModel<Track>() {
		@Override
		public boolean check(Track track) {
			return true;
		}
	};
	private FilterModel<Track> activeTracksModel = new FilterModel<Track>() {
		@Override
		public boolean check(Track track) {
			return track.isActive();
		}
	};
	private FilterModel<Track> localTracksModel = new FilterModel<Track>() {
		@Override
		public boolean check(Track track) {
			return track.isLocal();
		}
	};
}
