package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.fruct.oss.audioguide.track.gets.IContent;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthParametersParser implements ContentParser<AuthParameters> {
	@Override
	public  AuthParameters parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		AuthParameters content = new AuthParameters();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			switch (tagName) {
			case "client_id":
				content.clientId = XmlUtil.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "client_id");
				break;

			case "scope":
				content.scope = XmlUtil.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "scope");
				break;

			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		return content;
	}

}
