package org.fruct.oss.audioguide.events;

import org.fruct.oss.audioguide.track.Point;

public class PointOutRangeEvent {
	private Point point;

	public PointOutRangeEvent(Point point) {
		this.point = point;
	}

	public Point getPoint() {
		return point;
	}
}
