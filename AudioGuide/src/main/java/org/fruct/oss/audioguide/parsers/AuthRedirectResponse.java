package org.fruct.oss.audioguide.parsers;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class AuthRedirectResponse implements IContent {
	@Element(name = "id")
	private String sessionId;

	@Element(name = "redirect_url")
	private String redirectUrl;

	public String getSessionId() {
		return sessionId;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}
}
