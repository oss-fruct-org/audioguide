package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class TracksParser implements ContentParser<TracksContent> {
	public TracksContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
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

		return new TracksContent(tracks);
	}

	private static Track parseTrack(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "track");

		String name = "";
		String description = "";
		String url = "";
		String hname = null;
		String access = "r";
		String photoUrl = null;
		long categoryId = -1;

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			switch (tagName) {
			case "photoUrl":
				photoUrl = XmlUtil.readText(parser);
				break;
			case "category_id":
				categoryId = Long.parseLong(XmlUtil.readText(parser));
				break;
			case "access":
				access = XmlUtil.readText(parser);
				break;
			case "hname":
				hname = XmlUtil.readText(parser);
				break;
			case "name":
				name = XmlUtil.readText(parser);
				break;
			case "description":
				description = XmlUtil.readText(parser);
				break;
			default:
				Utils.skip(parser);
				break;
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "track");
		Track track = new Track(name, description, url);
		track.setHname(hname);
		track.setPrivate(access.equals("rw"));
		track.setCategoryId(categoryId);
		track.setPhotoUrl(photoUrl);
		return track;
	}
}
