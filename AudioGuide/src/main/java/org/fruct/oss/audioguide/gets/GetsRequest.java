package org.fruct.oss.audioguide.gets;

import android.os.Handler;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;

public abstract class GetsRequest {
	protected final Gets gets;

	public GetsRequest(Gets gets) {
		this.gets = gets;
	}

	protected abstract String createRequestString();
	protected abstract String getRequestUrl();

	protected abstract Class<? extends IContent> getContentClass();

	/**
	 * Checks if request can be performed at this time
	 *
	 * @return true if request ready to be executed
	 */
	protected boolean onPreExecute() {
		return true;
	}

	/**
	 * Posts intermediate request result
	 *
	 * @param response Request response
	 * @return true if this request shall be continued
	 */
	protected boolean onPostExecute(GetsResponse response) {
		return false;
	}

	/**
	 * Posts final request result
	 * @param response Request response
	 */
	protected void onPostProcess(GetsResponse response) {
	}

	protected abstract void onError();
}
