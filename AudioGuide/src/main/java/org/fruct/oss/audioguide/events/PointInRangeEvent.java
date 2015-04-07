package org.fruct.oss.audioguide.events;

import org.fruct.oss.audioguide.track.Point;

public class PointInRangeEvent {
	private Point point;

	public PointInRangeEvent(Point point) {
		this.point = point;
	}

	public Point getPoint() {
		return point;
	}
}
