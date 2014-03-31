package org.fruct.oss.audioguide.track;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.util.LruCache;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.util.Downloader;
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
import java.util.Objects;
import java.lang.Object;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

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

public class TrackManager {
	private final static Logger log = LoggerFactory.getLogger(TrackManager.class);

	public static interface Listener {
		void tracksUpdated();
		void trackUpdated(Track track);
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

	public void storeLocal(final Track track) {
		checkInitialized();

		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}

		synchronized (allTracks) {
			track.setLocal(true);
			allTracks.put(track.getId(), track);

			if (localStorage.getPoints(track).isEmpty()) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						doLoadRemotePoints(track);
					}
				});
			}
		}

		notifyTrackUpdated(track);
	}

	public void activateTrack(Track track) {
		if (track.isLocal()) {
			track.setActive(true);
			allTracks.put(track.getId(), track);
			localStorage.storeLocalTrack(track);
			notifyTrackUpdated(track);
		}
	}

	public void deactivateTrack(Track track) {
		if (track.isLocal()) {
			track.setActive(false);
			allTracks.put(track.getId(), track);
			localStorage.storeLocalTrack(track);
			notifyTrackUpdated(track);
		}
	}

	public void storePoints(Track track, List<Point> points) {
		localStorage.storeLocalPoints(track, points);
	}

	private void doLoadRemoteTracks() {
		log.trace("doLoadRemoteTracks");
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
	}

	private synchronized void notifyTracksUpdated() {
		for (Listener listener : listeners)
			listener.tracksUpdated();
	}

	private synchronized void notifyTrackUpdated(Track track) {
		for (Listener listener : listeners)
			listener.trackUpdated(track);
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
}
