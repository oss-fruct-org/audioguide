package org.fruct.oss.audioguide.events;

import org.fruct.oss.audioguide.track.Point;

public class PlayPositionEvent {
	private final Point point;
	private final int position;

	public PlayPositionEvent(int position, Point currentPoint) {
		this.position = position;
		this.point = currentPoint;
	}

	public int getPosition() {
		return position;
	}

	public Point getPoint() {
		return point;
	}
}
