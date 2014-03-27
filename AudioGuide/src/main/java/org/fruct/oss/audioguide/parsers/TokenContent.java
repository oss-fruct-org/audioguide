package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.GetsStorage;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class TokenContent implements IContent {
	private String accessToken;

	public String getAccessToken() {
		return accessToken;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		TokenContent token = new TokenContent();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "auth_token");
		token.accessToken = GetsResponse.readText(parser);
		parser.require(XmlPullParser.END_TAG, null, "auth_token");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return token;
	}
}
