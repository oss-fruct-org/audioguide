package org.fruct.oss.audioguide.gets;

import android.os.Handler;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;

public abstract class GetsRequest {
	protected final Gets gets;
	private Handler handler;

	private boolean completed;
	private GetsResponse response;

	public GetsRequest(Gets gets) {
		this.gets = gets;
	}

	void setHandler(Handler handler) {
		this.handler = handler;
	}

	Handler getHandler() {
		return handler;
	}

	void setCompleted() {
		this.completed = true;
	}

	boolean isCompleted() {
		return completed;
	}

	protected abstract String createRequestString();
	protected abstract String getRequestUrl();
	protected abstract int getPriority();
	protected abstract Class<? extends IContent> getContentClass();

	protected abstract void onPostProcess(GetsResponse response);
	protected abstract void onError();

	public void setResponse(GetsResponse response) {
		this.response = response;
	}

	public GetsResponse getResponse() {
		return response;
	}
}
