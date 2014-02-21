package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import org.fruct.oss.audioguide.track.ArrayStorage;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackManagerTest extends AndroidTestCase implements TrackManager.Listener {
	private TrackManager trackManager;

	private boolean isTracksReceived;
	private final Object waiter = new Object();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ArrayStorage remoteStorage = new ArrayStorage()
				.insert(new Track("aaa", "AAA", "uaaa"))
				.insert(new Track("bbb", "BBB", "ubbb"));

		ArrayStorage localStorage = new ArrayStorage();

		trackManager = new TrackManager(localStorage, remoteStorage);
		trackManager.addListener(this);
		isTracksReceived = false;
	}

	public void testEmptyLocalStorage() {
		trackManager.initialize();
		assertEquals(0, trackManager.getTracks().size());
	}

	public void testLoadingRemoteTracks() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();

		waitTracksUpdated();
		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);
		assertEquals(2, tracks.size());
		assertEquals("aaa", tracks.get(0).getName());
		assertEquals("bbb", tracks.get(1).getName());
	}

	public void testUpdateLocalTrack() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();
		waitTracksUpdated();

		Track newTrack = new Track("aaa", "AaA", "url");
		trackManager.updateLocalTrack(newTrack);

		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);
		assertEquals(2, tracks.size());
		assertEquals("AaA", tracks.get(0).getDescription());
		assertEquals("BBB", tracks.get(1).getDescription());
	}

	@Override
	public void tracksUpdated() {
		synchronized (waiter) {
			isTracksReceived = true;
			waiter.notifyAll();
		}
	}

	private void waitTracksUpdated() {
		synchronized (waiter) {
			if (isTracksReceived)
				return;
			try {
				waiter.wait(5000);
			} catch (InterruptedException e) {
			}

			if (!isTracksReceived)
				throw new RuntimeException("Track manager timeout exceeded");
		}
	}

	private void sortTracks(List<Track> tracks) {
		Collections.sort(tracks, new Comparator<Track>() {
			@Override
			public int compare(Track track, Track track2) {
				return track.getName().compareTo(track2.getName());
			}
		});
	}
}
