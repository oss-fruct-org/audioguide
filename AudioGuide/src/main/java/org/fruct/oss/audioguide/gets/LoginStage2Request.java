package org.fruct.oss.audioguide.gets;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.TokenContent;

public abstract class LoginStage2Request extends GetsRequest {
	private final String sessionId;

	public LoginStage2Request(Gets gets, String sessionId) {
		super(gets);
		this.sessionId = sessionId;
	}

	@Override
	protected String createRequestString() {
		return "<request><params><id>" + sessionId + "</id></params></request>";
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/userLogin.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return TokenContent.class;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {
		TokenContent tokenContent = ((TokenContent) response.getContent());

		gets.setToken(tokenContent);
	}

	@Override
	protected abstract void onError();
}
