package org.fruct.oss.audioguide.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class PostUrlContent implements IContent {
	private String postUrl;

	public String getPostUrl() {
		return postUrl;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		PostUrlContent token = new PostUrlContent();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "post_url");
		token.postUrl = GetsResponse.readText(parser);
		parser.require(XmlPullParser.END_TAG, null, "post_url");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return token;
	}
}
