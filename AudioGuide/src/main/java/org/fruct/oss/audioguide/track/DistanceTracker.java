package org.fruct.oss.audioguide.track;

import android.location.Location;

import org.fruct.oss.audioguide.LocationReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistanceTracker implements LocationReceiver.Listener {
	private static final float MIN_DISTANCE = 50;

	private List<Point> points = new ArrayList<Point>();
	private Set<Point> pointsInRange = new HashSet<Point>();

	private TrackManager trackManager;
	private LocationReceiver locationReceiver;
	private List<Listener> listeners = new ArrayList<Listener>();
	private float radius = MIN_DISTANCE;

	public DistanceTracker(TrackManager trackManager, LocationReceiver locationReceiver) {
		this.trackManager = trackManager;
		this.locationReceiver = locationReceiver;
	}

	public void setTracks(List<Track> tracks) {
		ArrayList<Point> points = new ArrayList<Point>();

		for (Track track : tracks) {
			points.addAll(trackManager.getPoints(track));
		}

		this.points = points;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void start() {
		locationReceiver.setListener(this);
		locationReceiver.start();
	}

	public void stop() {
		locationReceiver.stop();
		locationReceiver.setListener(null);
	}

	@Override
	public void newLocation(Location location) {
		for (Point point : points) {
			boolean isPointInRange = pointsInRange.contains(point);

			Location pointLocation = point.toLocation();
			float distanceMeters = pointLocation.distanceTo(location);

			// TODO: de-hardcode distance
			if (distanceMeters < radius && !isPointInRange) {
				pointsInRange.add(point);
				notifyPointInRange(point);
			} else if (distanceMeters >= radius && isPointInRange) {
				pointsInRange.remove(point);
				notifyPointOutRange(point);
			}
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

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public List<Point> getPointsInRange() {
		return new ArrayList<Point>(pointsInRange);
	}

	public interface Listener {
		void pointInRange(Point point);
		void pointOutRange(Point point);
	}
}
