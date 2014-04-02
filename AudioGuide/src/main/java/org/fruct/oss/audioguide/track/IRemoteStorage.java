package org.fruct.oss.audioguide.track;

import java.util.List;

public interface IRemoteStorage {
	void sendPoint(Track track, Point point);
	void sendTrack(Track track, List<Point> points);
}
