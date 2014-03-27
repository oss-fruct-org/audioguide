package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthRedirectResponse implements IContent {
	private String sessionId;
	private String redirectUrl;

	public String getSessionId() {
		return sessionId;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		AuthRedirectResponse content = new AuthRedirectResponse();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("id")) {
				content.sessionId = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "id");
			} else if (tagName.equals("redirect_url")) {
				content.redirectUrl = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "redirect_url");
			} else {
				Utils.skip(parser);
			}
		}

		return content;
	}
}
