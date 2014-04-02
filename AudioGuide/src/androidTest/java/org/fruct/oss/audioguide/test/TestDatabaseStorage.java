package org.fruct.oss.audioguide.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import org.fruct.oss.audioguide.track.DatabaseStorage;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TestDatabaseStorage extends AndroidTestCase {
	private final static Logger log = LoggerFactory.getLogger(TestDatabaseStorage.class);
	DatabaseStorage storage;
	private RenamingDelegatingContext context;

	@Override
	protected void setUp() throws Exception {
		context = new RenamingDelegatingContext(getContext(), "test_");
		storage = new DatabaseStorage(context);
		storage.initialize();
		storage.load();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		storage.close();
		storage = null;

		if (!context.deleteDatabase(DatabaseStorage.DB_NAME))
			log.error("Cannot delete database");
	}

	public void testStoreCached() {
		storage.storeLocalTrack(new Track("AAA", "BBB", "CCC"));
		List<Track> tracks = storage.getTracks();
		assertEquals("BBB", tracks.get(0).getDescription());
	}

	public void testStoreReopen() {
		storage.storeLocalTrack(new Track("AAA", "BBB", "CCC"));

		reopen();

		List<Track> tracks = storage.getTracks();

		assertEquals("BBB", tracks.get(0).getDescription());
	}


	public void testDuplicatedStoreCached() {
		Track track;

		storage.storeLocalTrack(track = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track);

		assertEquals(1, storage.getTracks().size());
	}

	public void testDuplicatedStoreReopen() {
		Track track;

		storage.storeLocalTrack(track = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track);

		reopen();

		assertEquals(1, storage.getTracks().size());
	}

	public void testStoreMultipleItemsCached() {
		Track track, track2;

		storage.storeLocalTrack(track = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track2 = new Track("BBB", "CCC", "DDD"));

		assertEquals(2, storage.getTracks().size());

		assertTrue(storage.getTracks().contains(track));
		assertTrue(storage.getTracks().contains(track2));
	}

	public void testStoreMultipleItemsReopen() {
		Track track, track2;

		storage.storeLocalTrack(track = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track2 = new Track("BBB", "CCC", "DDD"));

		reopen();

		assertEquals(2, storage.getTracks().size());

		assertTrue(storage.getTracks().contains(track));
		assertTrue(storage.getTracks().contains(track2));
	}

	public void testStorePoints() {
		Track track, track2;
		Point point1, point2;

		storage.storeLocalTrack(track = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track2 = new Track("BBB", "CCC", "DDD"));

		reopen();

		List<Point> points = new ArrayList<Point>();
		points.add(point1 = new Point("ZZZ1", "XXX", "CCC", 46, 64));
		points.add(point2 = new Point("ZZZ2", "XXX2", "CCC2", 10, 11));

		assertTrue(track.isLocal());
		storage.storeLocalPoints(track, points);
		points = storage.getPoints(track2);
		assertEquals(0, points.size());

		points = storage.getPoints(track);
		assertEquals(2, points.size());

		reopen();

		points = storage.getPoints(track);
		assertEquals(2, points.size());

		sort(points);
		assertTrue(comparePoints(point1, points.get(0)));
		assertTrue(comparePoints(point2, points.get(1)));
	}

	public void testUpdatePoint() {
		Track track1, track2;
		storage.storeLocalTrack(track1 = new Track("AAA", "BBB", "CCC"));
		storage.storeLocalTrack(track2 = new Track("BBB", "CCC", "DDD"));

		Point point1, point2;
		storage.storePoint(track1, point1 = new Point("ZZZ1", "XXX", "CCC", 46, 64));
		storage.storePoint(track1, point2 = new Point("ZZZ2", "YYY", "CCC", 12, 13));

		reopen();

		List<Point> points = storage.getPoints(track1);
		sort(points);

		assertEquals("XXX", points.get(0).getDescription());
		assertEquals("YYY", points.get(1).getDescription());


		// Update point
		point2.setDescription("QWERTY");
		storage.storePoint(track1, point2);
		reopen();

		points = storage.getPoints(track1);
		sort(points);

		assertEquals(2, points.size());
		assertEquals("XXX", points.get(0).getDescription());
		assertEquals("QWERTY", points.get(1).getDescription());
	}

	private boolean comparePoints(Point p1, Point p2) {
		return p1.getName().equals(p2.getName())
				&& p1.getDescription().equals(p2.getDescription())
				&& p1.getAudioUrl().equals(p2.getAudioUrl())
				&& p1.getLatE6() == p2.getLatE6()
				&& p1.getLonE6() == p2.getLonE6();
	}

	private void sort(List<Point> points) {
		Collections.sort(points, new Comparator<Point>() {
			@Override
			public int compare(Point point, Point point2) {
				return point.getName().compareTo(point2.getName());
			}
		});
	}

	private void reopen() {
		storage.close();
		storage.initialize();
		storage.load();
	}
}
