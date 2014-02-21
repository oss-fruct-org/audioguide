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

	private ArrayStorage remoteStorage;
	private ArrayStorage localStorage;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		remoteStorage = new ArrayStorage()
				.insert(new Track("aaa", "AAA", "uaaa"))
				.insert(new Track("bbb", "BBB", "ubbb"));

		localStorage = new ArrayStorage();

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

		assertEquals(0, localStorage.getTracks().size());
	}

	public void testUpdateLocalTrack() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();
		waitTracksUpdated();

		Track newTrack = new Track("aaa", "AaA", "url");
		trackManager.storeLocal(newTrack);

		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);

		assertEquals(2, tracks.size());
		assertEquals("AaA", tracks.get(0).getDescription());
		assertEquals("BBB", tracks.get(1).getDescription());

		assertTrue(tracks.get(0).isLocal());
		assertFalse(tracks.get(1).isLocal());


		assertEquals(1, localStorage.getTracks().size());
		assertEquals("AaA", localStorage.getTracks().get(0).getDescription());
	}

	public void testUpdateRemoteTrack() {
		testLoadingRemoteTracks();

		remoteStorage.insert(new Track("ccc", "CCC", "uccc"));
		trackManager.loadRemoteTracks();

		waitTracksUpdated();
		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);
		assertEquals(3, tracks.size());
		assertEquals("aaa", tracks.get(0).getName());
		assertEquals("bbb", tracks.get(1).getName());
		assertEquals("uccc", tracks.get(2).getUrl());

		assertEquals(0, localStorage.getTracks().size());
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
			if (isTracksReceived) {
				isTracksReceived = false;
				return;
			}

			try {
				waiter.wait(5000);
			} catch (InterruptedException e) {
			}

			if (!isTracksReceived)
				throw new RuntimeException("Track manager timeout exceeded");
			isTracksReceived = false;
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
