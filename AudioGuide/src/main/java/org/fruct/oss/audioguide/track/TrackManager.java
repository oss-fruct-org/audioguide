package org.fruct.oss.audioguide.track;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackManager {
	public static interface Listener {
		void tracksUpdated();
		void trackUpdated(Track track);
		void pointsUpdated(Track track);
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
		checkInitialized();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				doLoadRemoteTracks();
			}
		});
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

	public void storeLocal(final Track track) {
		checkInitialized();

		synchronized (localStorage) {
			localStorage.storeLocalTrack(track);
		}

		synchronized (allTracks) {
			track.setLocal(true);
			allTracks.put(track.getId(), track);

			executor.execute(new Runnable() {
				@Override
				public void run() {
					doLoadRemotePoints(track);
				}
			});
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
		remoteStorage.load();

		List<Track> tracks = remoteStorage.getTracks();
		synchronized (allTracks) {
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
		Track track1 = new Track("AAA", "BBB", "CCC");
		Track track2 = new Track("CCC", "DDD", "EEE");
		IStorage remoteStorage = new ArrayStorage()
				.insert(track1)
				.insert(track2)
				.insert(new Point("Point1", "Point1 desc", "http://example.com/1.ogg", 61, 34), track1)
				.insert(new Point("Point2", "Point2 desc", "http://example.com/2.ogg", 62, 35), track1);


		instance = new TrackManager(localStorage, remoteStorage);
		instance.initialize();
		return instance;
	}
}
