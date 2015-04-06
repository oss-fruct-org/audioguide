package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class UserInfoParser implements ContentParser<UserInfo> {
	@Override
	public UserInfo parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		return UserInfo.parse(parser);
	}
}
