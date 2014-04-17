package org.fruct.oss.audioguide.test;

import android.location.Location;
import android.test.AndroidTestCase;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.track.ArrayStorage;
import org.fruct.oss.audioguide.track.DistanceTracker;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DistanceTrackerTest extends AndroidTestCase {
	private DistanceTracker distanceTracker;
	private LocationReceiver locationReceiver;
	private ExecutorService executor;


	private Point pointInRange;
	private Point pointOutRange;

	// TODO: Broken test
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		executor = Executors.newSingleThreadExecutor();

		Track track = new Track("Test track 1", "Test track 1 desc", "");

		ArrayStorage localStorage = new ArrayStorage();
		ArrayStorage remoteStorage = new ArrayStorage()
				.insert(track)
				.insert(new Point("A", "AA", "AAA", 61676249, 34559868), track)
				.insert(new Point("B", "BB", "BBB", 61645226, 34588145), track);

		TrackManager trackManager = new TrackManager(localStorage, remoteStorage);
		trackManager.setExecutor(executor);
		trackManager.initialize();
		trackManager.storeLocal(track);
		trackManager.activateTrack(track);

		executor.shutdown();
		if ( !executor.awaitTermination(5, TimeUnit.SECONDS)) {
			throw new TimeoutException();
		}


		locationReceiver = new LocationReceiver(getContext());
		locationReceiver.disableRealLocation();

		distanceTracker = new DistanceTracker(trackManager, locationReceiver);
		distanceTracker.setRadius(500);
		//distanceTracker.setTracks(trackManager.getTracks());
		distanceTracker.addListener(new MockListener());
		distanceTracker.start();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		locationReceiver.stop();
		distanceTracker.stop();
	}

	public void testNotInRange() {
		locationReceiver.mockLocation(createLocation(61.641552,34.598956));
		assertNull(pointInRange);
		assertNull(pointOutRange);
	}

	public void testEnterRange() {
		locationReceiver.mockLocation(createLocation(61.643400, 34.594233));
		assertNotNull(pointInRange);
		assertNull(pointOutRange);

		assertEquals("B", pointInRange.getName());
	}

	public void testEnterExitRange() {
		locationReceiver.mockLocation(createLocation(61.643400, 34.594233));
		locationReceiver.mockLocation(createLocation(61.641552,34.598956));
		assertNotNull(pointOutRange);

		assertEquals("B", pointOutRange.getName());
	}

	private Location createLocation(double lat, double lon) {
		Location location = new Location("Test");

		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAccuracy(0);

		return location;
	}

	private class MockListener implements DistanceTracker.Listener {
		@Override
		public void pointInRange(Point point) {
			pointInRange = point;
		}

		@Override
		public void pointOutRange(Point point) {
			pointOutRange = point;
		}
	}
}
