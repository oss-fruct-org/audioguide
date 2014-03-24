package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import org.fruct.oss.audioguide.track.ArrayStorage;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackManagerTest extends AndroidTestCase implements TrackManager.Listener {
	private TrackManager trackManager;

	private boolean isTracksReceived;
	private final Object waiter = new Object();

	private ArrayStorage remoteStorage;
	private ArrayStorage localStorage;

	private Track track1;
	private Track track2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		remoteStorage = new ArrayStorage()
				.insert(track1 = new Track("aaa", "AAA", "uaaa"))
				.insert(track2 = new Track("bbb", "BBB", "ubbb"));

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

	public void testAddPoints() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();
		waitTracksUpdated();

		Point point1, point2;
		ArrayList<Point> spoints = new ArrayList<Point>();
		spoints.add(point1 = new Point("name", "description", "audioUrl", 10, 11));
		spoints.add(point2 = new Point("name2", "description2", "audioUrl2", 20, 21));

		trackManager.storePoints(track1, spoints);

		List<Point> points = localStorage.getPoints(track1);

		assertTrue(points.contains(point1));
		assertTrue(points.contains(point2));
	}

	public void testActivate() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();
		waitTracksUpdated();

		trackManager.storeLocal(track1);
		trackManager.activateTrack(track1);
		waitTracksUpdated();

		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);

		assertFalse(tracks.get(1).isActive());
		assertTrue(tracks.get(0).isActive());
	}

	@Override
	public void tracksUpdated() {
		synchronized (waiter) {
			isTracksReceived = true;
			waiter.notifyAll();
		}
	}

	@Override
	public void trackUpdated(Track track) {
		synchronized (waiter) {
			isTracksReceived = true;
			waiter.notifyAll();
		}
	}

	@Override
	public void pointsUpdated(Track track) {
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
