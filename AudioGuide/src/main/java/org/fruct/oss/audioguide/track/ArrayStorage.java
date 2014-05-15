package org.fruct.oss.audioguide.track;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArrayStorage implements ILocalStorage {
	private HashMap<String, Track> tracks = new HashMap<String, Track>();
	private HashMap<Track, List<Point>> points = new HashMap<Track, List<Point>>();

	public ArrayStorage insert(Track track) {
		storeLocalTrack(track);
		return this;
	}

	public ArrayStorage insert(Point point, Track track) {
		List<Point> trackPoints = points.get(track);

		if (trackPoints == null) {
			trackPoints = new ArrayList<Point>();
			points.put(track, trackPoints);
		}

		trackPoints.add(point);
		return this;
	}

	@Override
	public void storeLocalTrack(Track track) {
		// TODO: this method updates all local tracks instead of update only changed
		if (tracks.containsKey(track.getName()))
			tracks.remove(track.getName());

		tracks.put(track.getName(), track);
	}

	@Override
	public void storeLocalPoints(Track track, List<Point> points) {
		for (Point point : points) {
			insert(point, track);
		}
	}

	@Override
	public void updatePoint(Track track, Point point) {
		if (points.containsKey(track)) {
			List<Point> list = points.get(track);
			for (int i = 0; i < list.size(); i++) {
				Point p = list.get(i);
				if (point.equals(p)) {
					list.set(i, point);
					break;
				}
			}
		}
	}

	@Override
	public void initialize() {
	}

	@Override
	public void close() {
	}


	@Override
	public void loadAsync(final Handler handler) {
	}

	@Override
	public List<Track> getTracks() {
		return new ArrayList<Track>(tracks.values());
	}

	@Override
	public void loadPoints(Track track, Handler handler) {
		handler.sendMessage(new Message());
	}

	@Override
	public List<Point> getPoints(Track track) {
		if (points.containsKey(track))
			return new ArrayList<Point>(points.get(track));
		else
			return new ArrayList<Point>();
	}
}
