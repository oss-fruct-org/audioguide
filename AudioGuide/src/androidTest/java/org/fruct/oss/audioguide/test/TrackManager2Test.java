package org.fruct.oss.audioguide.test;

import android.content.Context;
import android.os.AsyncTask;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.track2.DefaultTrackManager;
import org.fruct.oss.audioguide.track.track2.StorageBackend;
import org.fruct.oss.audioguide.track.track2.TestStorageBackend;
import org.fruct.oss.audioguide.track.track2.TrackManager;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TrackManager2Test extends InstrumentationTestCase {
	private Context context;
	private TrackManager trackManager;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RenamingDelegatingContext context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_context");
		context.deleteDatabase("tracksdb2");
		this.context = context;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void createSimpleTrackManager() {
		trackManager = new DefaultTrackManager(context, null, null);
	}

	private void createTrackManagerWithBackend(TestStorageBackend backend) {
		trackManager = new DefaultTrackManager(context, backend, backend);
	}

	private TestStorageBackend createTestBackend() {
		TestStorageBackend backend = new TestStorageBackend();

		Track track = new Track("AAA", "BBB", "CCC");
		track.setCategoryId(1);
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

		assertTrue(trackManager.getLocalTracksModel().getCount() == 0);
	}

	public void testStorePoint() throws Exception {
		createSimpleTrackManager();

		Point point = new Point("AAA", "BBB", "CCC", 0f, 0f);
		Track track = new Track("AAA", "BBB", "CCC");

		trackManager.insertPoint(point);
		trackManager.insertTrack(track);
		trackManager.insertToTrack(track, point);

		Model<Track> trackModel = trackManager.getLocalTracksModel();
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

	public void testRemoteLoading() throws Throwable {
		createTrackManagerWithBackend(createTestBackend());

		final Model<Track> remoteTrack = trackManager.getRemoteTracksModel();
		Model<Point> remotePoint = trackManager.getRemotePointsModel();

		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(2);
		final CountDownLatch latch3 = new CountDownLatch(3);

		final ModelListener listener = new ModelListener() {
			@Override
			public void dataSetChanged() {
				latch.countDown();
				latch2.countDown();
				latch3.countDown();
			}
		};

		remoteTrack.addListener(listener);
		remotePoint.addListener(listener);

		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				trackManager.requestTracksInRadius(0, 0, 7f);
			}
		});

		assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));

		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				trackManager.requestPointsInRadius(0, 0, 7f);
			}
		});

		assertTrue(latch2.await(1000, TimeUnit.MILLISECONDS));

		assertEquals(1, remoteTrack.getCount());
		assertEquals(3, remotePoint.getCount());

		assertEquals("AAA",  remoteTrack.getItem(0).getName());

		assertEquals("MMM1",  remotePoint.getItem(0).getName());
		assertEquals("MMM3",  remotePoint.getItem(1).getName());
		assertEquals("MMM4",  remotePoint.getItem(2).getName());

		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				trackManager.requestPointsInTrack(remoteTrack.getItem(0));
			}
		});

		assertTrue(latch3.await(1000, TimeUnit.MILLISECONDS));
		assertEquals(4, remotePoint.getCount());
	}

	public void testAsync() throws Throwable {
		final CountDownLatch latch = new CountDownLatch(1);

		final AsyncTask<Void, Void, String> at = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				return "123";
			}

			@Override
			protected void onPostExecute(String aVoid) {
				super.onPostExecute(aVoid);
				latch.countDown();
			}
		};

		runTestOnUiThread(new Runnable() {
			@Override
			public void run() {
				at.execute();
			}
		});

		assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
		assertEquals("123", at.get());
	}
}
