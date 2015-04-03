package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class KmlParser implements ContentParser<Kml> {
	@Override
	public Kml parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		Kml kml = new Kml();
		ArrayList<Point> points = new ArrayList<>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "kml");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "Document");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			switch (tagName) {
			case "Placemark":
				points.add(Point.parse(parser));
				parser.require(XmlPullParser.END_TAG, null, "Placemark");
				break;
			default:
				XmlUtil.skip(parser);
				break;
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
}
