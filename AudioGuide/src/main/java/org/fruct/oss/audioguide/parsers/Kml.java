package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Kml implements IContent {
	public List<Point> getPoints() {
		return points;
	}

	public String getName() {
		return name;
	}

	public int getOpen() {
		return open;
	}

	public String getDescription() {
		return description;
	}


	private String name;
	private int open;
	private String description;
	private List<Point> points;

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		Kml kml = new Kml();
		ArrayList<Point> points = new ArrayList<Point>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "kml");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "Document");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("name")) {
				kml.name = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "name");
			} else if (tagName.equals("open")) {
				kml.open = Integer.parseInt(GetsResponse.readText(parser));
				parser.require(XmlPullParser.END_TAG, null, "open");
			} else if (tagName.equals("Placemark")) {
				points.add(Point.parse(parser));
				parser.require(XmlPullParser.END_TAG, null, "Placemark");
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "Document");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "kml");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		kml.points = points;
		return kml;
	}

	public void setTracks(List<Point> loadedPoints) {
		points = loadedPoints;
	}
}
