package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.gets.IContent;

public class TokenContent implements IContent {
	String accessToken;

	public String getAccessToken() {
		return accessToken;
	}
}
