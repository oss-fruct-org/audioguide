package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class TokenContentParser implements ContentParser<TokenContent> {
	@Override
	public TokenContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		TokenContent token = new TokenContent();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "auth_token");
		token.accessToken = XmlUtil.readText(parser);
		parser.require(XmlPullParser.END_TAG, null, "auth_token");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return token;
	}
}
