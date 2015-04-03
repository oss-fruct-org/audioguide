package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.ContentParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class CategoriesParser implements ContentParser<CategoriesContent> {
	@Override
	public CategoriesContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		return CategoriesContent.parse(parser);
	}
}
