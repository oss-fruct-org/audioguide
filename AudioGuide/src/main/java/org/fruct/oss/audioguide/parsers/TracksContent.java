package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TracksContent implements IContent {
	private List<Track> tracks;

	public List<Track> getTracks() {
		return tracks;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		ArrayList<Track> tracks = new ArrayList<Track>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "tracks");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("track")) {
				tracks.add(parseTrack(parser));
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "tracks");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		TracksContent tracksContent = new TracksContent();
		tracksContent.tracks = tracks;
		return tracksContent;
	}

	private static Track parseTrack(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "track");

		String name = "";
		String description = "";
		String url = "";

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("name")) {
				name = GetsResponse.readText(parser);
			} else if (tagName.equals("description")) {
				description = GetsResponse.readText(parser);
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "track");
		return new Track(name, description, url);
	}
}
