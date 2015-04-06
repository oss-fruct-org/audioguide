package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.IContent;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthRedirectContent implements IContent {
	private String sessionId;
	private String redirectUrl;

	public String getSessionId() {
		return sessionId;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public static AuthRedirectContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		AuthRedirectContent content = new AuthRedirectContent();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			switch (tagName) {
			case "id":
				content.sessionId = XmlUtil.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "id");
				break;
			case "redirect_url":
				content.redirectUrl = XmlUtil.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "redirect_url");
				break;
			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		return content;
	}
}

