package org.fruct.oss.audioguide.track;

import android.database.Cursor;
import android.location.Location;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.track2.CursorHolder;
import org.fruct.oss.audioguide.track.track2.CursorReceiver;
import org.fruct.oss.audioguide.track.track2.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistanceTracker implements LocationReceiver.Listener, CursorReceiver {
	private final static Logger log = LoggerFactory.getLogger(DistanceTracker.class);

	private Set<Point> pointsInRange = new HashSet<Point>();

	private TrackManager trackManager;
	private LocationReceiver locationReceiver;
	private List<Listener> listeners = new ArrayList<Listener>();
	private int radius;

	private final List<Point> points = new ArrayList<Point>();
	private final CursorHolder pointsCursorHolder;
	private Cursor currentCursor;

	public DistanceTracker(TrackManager trackManager, LocationReceiver locationReceiver) {
		this.trackManager = trackManager;
		this.locationReceiver = locationReceiver;

		pointsCursorHolder = trackManager.loadLocalPoints();
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void start() {
		log.debug("DistanceTracker stop");

		pointsCursorHolder.attachToReceiver(this);

		locationReceiver.addListener(this);
		locationReceiver.start();
	}

	public void stop() {
		log.debug("DistanceTracker stop");

		locationReceiver.stop();
		locationReceiver.removeListener(this);

		pointsCursorHolder.close();
	}

	@Override
	public void newLocation(Location location) {
		ArrayList<Point> outRange = new ArrayList<Point>();
		ArrayList<Point> inRange = new ArrayList<Point>();

		for (Point point : points) {
			boolean isPointInRange = pointsInRange.contains(point);

			Location pointLocation = point.toLocation();
			float distanceMeters = pointLocation.distanceTo(location);

			if (distanceMeters < radius && !isPointInRange) {
				inRange.add(point);
			} else if (distanceMeters >= radius && isPointInRange) {
				outRange.add(point);
			}
		}

		for (Point point : outRange) {
			pointsInRange.remove(point);
			notifyPointOutRange(point);
		}

		for (Point point : inRange) {
			pointsInRange.add(point);
			notifyPointInRange(point);
		}
	}

	private void notifyPointInRange(Point point) {
		for (Listener listener : listeners)
			listener.pointInRange(point);
	}

	private void notifyPointOutRange(Point point) {
		for (Listener listener : listeners)
			listener.pointOutRange(point);
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public List<Point> getPointsInRange() {
		return new ArrayList<Point>(pointsInRange);
	}

	@Override
	public Cursor swapCursor(Cursor cursor) {
		Cursor oldCursor = currentCursor;

		points.clear();
		while (cursor.moveToNext()) {
			Point point = new Point(cursor);
			points.add(point);
		}

		return oldCursor;
	}

	public interface Listener {
		void pointInRange(Point point);
		void pointOutRange(Point point);
	}
}
