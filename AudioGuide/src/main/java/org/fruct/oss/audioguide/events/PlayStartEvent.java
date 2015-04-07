package org.fruct.oss.audioguide.events;

import org.fruct.oss.audioguide.track.Point;

public class PlayStartEvent {
	private int duration;
	private Point point;

	public PlayStartEvent(int duration, Point point) {
		this.duration = duration;
		this.point = point;
	}

	public int getDuration() {
		return duration;
	}

	public Point getPoint() {
		return point;
	}
}
