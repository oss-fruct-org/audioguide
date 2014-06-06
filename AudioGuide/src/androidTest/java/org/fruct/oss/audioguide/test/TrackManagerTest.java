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

public class TrackManagerTest extends AndroidTestCase {
	private TrackManager trackManager;

	private ArrayStorage remoteStorage;
	private ArrayStorage localStorage;

	private Track track1;
	private Track track2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		remoteStorage = new ArrayStorage()
				.insert(track1 = new Track("aaa", "AAA", "uaaa"))
				.insert(track2 = new Track("bbb", "BBB", "ubbb"))
				.insert(new Point("qwe", "asd", "", 64, 31), track1);

		localStorage = new ArrayStorage();

		trackManager = new TrackManager(localStorage, remoteStorage);
	}

	public void testEmptyLocalStorage() {
		trackManager.initialize();
		assertEquals(0, trackManager.getTracks().size());
	}

	public void testLoadingRemoteTracks() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();

		trackManager.waitTasks();
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
		trackManager.waitTasks();

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

		trackManager.waitTasks();
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
		trackManager.waitTasks();

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
		trackManager.waitTasks();

		trackManager.storeLocal(track1);
		trackManager.activateTrack(track1);
		trackManager.waitTasks();

		List<Track> tracks = trackManager.getTracks();
		sortTracks(tracks);

		assertFalse(tracks.get(1).isActive());
		assertTrue(tracks.get(0).isActive());
	}

	public void testDoubleStorage() {
		trackManager.initialize();
		trackManager.loadRemoteTracks();
		trackManager.waitTasks();

		trackManager.storeLocal(track1);
		trackManager.waitTasks();

		trackManager.storeLocal(track1);
		trackManager.waitTasks();

		List<Point> points = trackManager.getPoints(track1);
		assertEquals(1, points.size());
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
