package org.fruct.oss.audioguide.track.gets;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public interface ContentParser<T extends IContent> {
	T parse(XmlPullParser parser) throws IOException, XmlPullParserException;
}
