package org.fruct.oss.audioguide.track;

import org.fruct.oss.audioguide.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackManager {
	public static interface Listener {
		void tracksUpdated();
	}

	private final ILocalStorage localStorage;
	private final IStorage remoteStorage;

	private volatile boolean isInitialized = false;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	// All known tracks from local and remote storage
	private final Map<Track, Track> allTracks = new HashMap<Track, Track>();
	private List<Listener> listeners = new ArrayList<Listener>();

	public TrackManager(ILocalStorage localStorage, IStorage remoteStorage) {
		this.localStorage = localStorage;
		this.remoteStorage = remoteStorage;
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
			allTracks.put(track, track);
		}

		isInitialized = true;
	}

	/**
	 * Method starts loading tracks from remote storage and immediately returns without blocking
	 */
	public void loadRemoteTracks() {
		checkInitialized();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				doLoadRemoteTracks();
			}
		});
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public List<Track> getTracks() {
		return new ArrayList<Track>(allTracks.values());
	}

	public void storeLocal(Track track) {
		checkInitialized();

		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}

		synchronized (allTracks) {
			track.setLocal(true);
			allTracks.put(track, track);
		}
	}

	private void startTask(Runnable runnable) {
		executor.execute(runnable);
	}

	private void doLoadRemoteTracks() {
		remoteStorage.load();

		List<Track> tracks = remoteStorage.getTracks();
		synchronized (allTracks) {
			for (Track track : tracks) {
				if (!allTracks.containsKey(track.getName()))
					allTracks.put(track, track);
			}
		}

		notifyTracksUpdated();
	}

	private synchronized void notifyTracksUpdated() {
		for (Listener listener : listeners)
			listener.tracksUpdated();
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
		IStorage remoteStorage = new ArrayStorage().insert(new Track("AAA", "BBB", "CCC"))
				.insert(new Track("CCC", "DDD", "EEE"));
		instance = new TrackManager(localStorage, remoteStorage);
		instance.initialize();
		return instance;
	}
}
