package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.IContent;
import org.fruct.oss.audioguide.track.gets.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthParameters implements IContent {
	String clientId;
	String scope;

	public String getClientId() {
		return clientId;
	}

	public String getScope() {
		return scope;
	}

}