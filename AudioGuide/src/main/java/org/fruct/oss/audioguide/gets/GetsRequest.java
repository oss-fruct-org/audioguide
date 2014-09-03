package org.fruct.oss.audioguide.gets;

import android.os.Handler;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;

public abstract class GetsRequest {
	protected final Gets gets;
	private int index;

	public GetsRequest(Gets gets) {
		this.gets = gets;
	}


	protected abstract String createRequestString();
	protected abstract String getRequestUrl();

	protected abstract Class<? extends IContent> getContentClass();

	protected boolean onPreExecute() {
		return true;
	}

	protected boolean onPostExecute(GetsResponse response) {
		return false;
	}

	protected void onPostProcess(GetsResponse response) {
	}

	protected abstract void onError();
}
