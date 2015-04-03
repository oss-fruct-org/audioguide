package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.gets.IContent;

import java.util.List;

public class Kml implements IContent {
	List<Point> points;

	public List<Point> getPoints() {
		return points;
	}
}
