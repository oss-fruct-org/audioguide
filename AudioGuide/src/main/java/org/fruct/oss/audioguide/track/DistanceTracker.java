package org.fruct.oss.audioguide.track;

import android.location.Location;

import org.fruct.oss.audioguide.LocationReceiver;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.track2.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistanceTracker implements LocationReceiver.Listener, ModelListener {
	private final static Logger log = LoggerFactory.getLogger(DistanceTracker.class);

	private Set<Point> pointsInRange = new HashSet<Point>();

	private TrackManager trackManager;
	private LocationReceiver locationReceiver;
	private List<Listener> listeners = new ArrayList<Listener>();
	private int radius;

	private final Model<Track> activeTrackModel;
	private List<PointModelHolder> pointModels = new ArrayList<PointModelHolder>();

	public DistanceTracker(TrackManager trackManager, LocationReceiver locationReceiver) {
		this.trackManager = trackManager;
		this.locationReceiver = locationReceiver;

		activeTrackModel = trackManager.getTracksModel();
	}

	@Override
	public void dataSetChanged() {
		clearPointModels();

		for (Track track : activeTrackModel) {
			pointModels.add(new PointModelHolder(trackManager.getTrackPointsModel(track)));
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void start() {
		log.debug("DistanceTracker stop");

		activeTrackModel.addListener(this);

		locationReceiver.addListener(this);
		locationReceiver.start();

		dataSetChanged();
	}

	public void stop() {
		log.debug("DistanceTracker stop");

		clearPointModels();
		activeTrackModel.removeListener(this);

		locationReceiver.stop();
		locationReceiver.removeListener(this);
	}

	private void clearPointModels() {
		for (PointModelHolder pointModelHolder : pointModels) {
			pointModelHolder.close();
		}
		pointModels.clear();
	}

	@Override
	public void newLocation(Location location) {
		ArrayList<Point> outRange = new ArrayList<Point>();
		ArrayList<Point> inRange = new ArrayList<Point>();

		for (PointModelHolder pointModelHolder : pointModels) {
			for (Point point : pointModelHolder.model) {
				boolean isPointInRange = pointsInRange.contains(point);

				Location pointLocation = point.toLocation();
				float distanceMeters = pointLocation.distanceTo(location);

				// TODO: de-hardcode distance
				if (distanceMeters < radius && !isPointInRange) {
					inRange.add(point);
				} else if (distanceMeters >= radius && isPointInRange) {
					outRange.add(point);
				}
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

	public interface Listener {
		void pointInRange(Point point);
		void pointOutRange(Point point);
	}

	private class PointModelHolder implements ModelListener, Closeable {
		Model<Point> model;

		PointModelHolder(Model<Point> model) {
			this.model = model;
			model.addListener(this);
		}

		public void close() {
			model.removeListener(this);
		}

		@Override
		public void dataSetChanged() {
		}
	}
}
