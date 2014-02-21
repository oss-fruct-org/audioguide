package org.fruct.oss.audioguide.track;

import java.util.ArrayList;
import java.util.List;
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

		isInitialized = true;
	}

	/**
	 * Method starts loading tracks from remote storage and immidiately returns without blocking
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
		return localStorage.getTracks();
	}

	public void updateLocalTrack(Track track) {
		checkInitialized();

		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}
	}

	private void startTask(Runnable runnable) {
		executor.execute(runnable);
	}

	private void doLoadRemoteTracks() {
		remoteStorage.load();

		List<Track> tracks = remoteStorage.getTracks();

		// Store all remote tracks to local storage
		synchronized (localStorage) {
			for (Track track : tracks) {
				localStorage.storeLocalTrack(track);
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
