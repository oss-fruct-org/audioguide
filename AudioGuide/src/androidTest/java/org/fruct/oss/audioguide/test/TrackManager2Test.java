package org.fruct.oss.audioguide.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.track2.DefaultTrackManager;
import org.fruct.oss.audioguide.track.track2.StorageBackend;
import org.fruct.oss.audioguide.track.track2.TestStorageBackend;
import org.fruct.oss.audioguide.track.track2.TrackManager;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;

public class TrackManager2Test extends AndroidTestCase {
	private Context context;
	private TrackManager trackManager;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_context");
		context.deleteDatabase("tracksdb2");
		this.context = context;
	}

	private void createSimpleTrackManager() {
		trackManager = new DefaultTrackManager(context, null, null);
	}

	private void createTrackManagerWithBackend(StorageBackend backend) {
		TestStorageBackend s = createTestBackend();
		trackManager = new DefaultTrackManager(context, s, s);
	}

	private TestStorageBackend createTestBackend() {
		TestStorageBackend backend = new TestStorageBackend();

		Track track = new Track("AAA", "BBB", "CCC");
		ArrayList<Point> points = new ArrayList<Point>() {{
			add(new Point("MMM1", "NNN1", "BBB1", 0f, 0f));
			add(new Point("MMM2", "NNN2", "BBB2", 10f, 10f));
			add(new Point("MMM3", "NNN3", "BBB3", -5f, -1f));
			add(new Point("MMM4", "NNN4", "BBB4", 0f, 1f));
		}};

		backend.updateTrack(track, points);

		return backend;
	}

	public void testEmpty() throws Exception {
		createSimpleTrackManager();

		assertTrue(trackManager.getTracksModel().getCount() == 0);
	}

	public void testStorePoint() throws Exception {
		createSimpleTrackManager();

		Point point = new Point("AAA", "BBB", "CCC", 0f, 0f);
		Track track = new Track("AAA", "BBB", "CCC");

		trackManager.insertPoint(point);
		trackManager.insertTrack(track);
		trackManager.insertToTrack(track, point);

		Model<Track> trackModel = trackManager.getTracksModel();
		Model<Point> pointModel = trackManager.getTrackPointsModel(track);

		assertEquals(1, trackModel.getCount());
		assertEquals(1, pointModel.getCount());

		Point point2 = pointModel.getItem(0);
		Track track2 = trackModel.getItem(0);

		assertEquals(point, point2);
		assertEquals(track.getName(), track2.getName());
		assertEquals(track.getDescription(), track2.getDescription());
		assertEquals(track.getUrl(), track2.getUrl());
	}

	public void testMultiPoint() throws Exception {
		createSimpleTrackManager();

		Point point = new Point("AAA", "BBB", "CCC", 0f, 0f);
		Track track = new Track("AAA", "BBB", "CCC");
		Track track2 = new Track("XXX", "YYY", "ZZZ");

		trackManager.insertTrack(track);
		trackManager.insertTrack(track2);
		trackManager.insertPoint(point);

		trackManager.insertToTrack(track, point);
		trackManager.insertToTrack(track2, point);

		Model<Point> pointModel = trackManager.getTrackPointsModel(track);
		Model<Point> pointModel2 = trackManager.getTrackPointsModel(track2);

		assertEquals(1, pointModel.getCount());
		assertEquals(1, pointModel2.getCount());

		assertTrue(pointModel.getItem(0).equals(pointModel2.getItem(0)));
	}

	public void testRemoteLoading() throws Exception {
		createTrackManagerWithBackend(createTestBackend());

		Model<Track> remoteTrack = trackManager.getRemoteTracksModel();
		Model<Point> remotePoint = trackManager.getRemotePointsModel();

		trackManager.requestTracksInRadius(7);
		assertEquals(1, remoteTrack.getCount());

		trackManager.requestPointsInTrack(remoteTrack.getItem(0));
		assertEquals(3, remotePoint.getCount());

		assertEquals("AAA",  remoteTrack.getItem(0).getName());

		assertEquals("MMM1",  remotePoint.getItem(0).getName());
		assertEquals("MMM3",  remotePoint.getItem(1).getName());
		assertEquals("MMM4",  remotePoint.getItem(2).getName());
	}
}
