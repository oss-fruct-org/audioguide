package org.fruct.oss.audioguide.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private final Map<String, Track> allTracks = new HashMap<String, Track>();
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

		localStorage.load();

		for (Track track : localStorage.getTracks()) {
			track.setLocal(true);
			allTracks.put(track.getName(), track);
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
			allTracks.put(track.getName(), track);
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
				allTracks.put(track.getName(), track);
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
}
