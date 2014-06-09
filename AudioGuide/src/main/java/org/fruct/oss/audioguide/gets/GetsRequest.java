package org.fruct.oss.audioguide.gets;

import android.os.Handler;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;

import java.util.Map;

public abstract class GetsRequest {
	protected final Gets gets;
	private Handler handler;
	private int index;

	public GetsRequest(Gets gets) {
		this.gets = gets;
	}

	void setHandler(Handler handler) {
		this.handler = handler;
	}

	Handler getHandler() {
		return handler;
	}

	protected abstract String createRequestString();
	protected abstract String getRequestUrl();

	protected abstract Class<? extends IContent> getContentClass();

	protected boolean onPreExecute() {
		return true;
	}

	protected void onPostProcess(GetsResponse response) {

	}

	protected abstract void onError();


	void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}
