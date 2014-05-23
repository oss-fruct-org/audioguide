package org.fruct.oss.audioguide.gets;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.TokenContent;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Gets implements Runnable {
	public static final String GETS_SERVER = "http://getsi.no-ip.info/getslocal";
	//public static final String GETS_SERVER = "http://oss.fruct.org/projects/gets/service";

	private final static Logger log = LoggerFactory.getLogger(Gets.class);

	private final PriorityQueue<GetsRequest> requestQueue;

	private final Context context;
	private int index = 0;

	public Gets(Context context) {
		this.context = context;

		requestQueue = new PriorityQueue<GetsRequest>(10, new Comparator<GetsRequest>() {
			@Override
			public int compare(GetsRequest getsRequest, GetsRequest getsRequest2) {
				final int priority1 = getsRequest.getPriority();
				final int priority2 = getsRequest2.getPriority();
				if (priority1 == priority2) {
					return getsRequest.getIndex() - getsRequest2.getIndex();
				} else {
					return priority1 - priority2;
				}
			}
		});

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	public void addRequest(GetsRequest request) {
		synchronized (requestQueue) {
			request.setHandler(new Handler());
			request.setIndex(index++);

			requestQueue.add(request);
			requestQueue.notifyAll();
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			GetsRequest request;

			synchronized (requestQueue) {
				while (requestQueue.isEmpty()) {
					try {
						requestQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}

				request = requestQueue.poll();
			}

			processRequest(request);
		}
	}

	private void processRequest(GetsRequest request) {
		String requestString = request.createRequestString();
		String requestUrl = request.getRequestUrl();
		String responseString;

		try {
			responseString = Utils.downloadUrl(requestUrl, requestString);
		} catch (IOException e) {
			String simpleName = request.getClass().getSimpleName();
			if (simpleName.isEmpty()) {
				simpleName = request.getClass().getSuperclass().getSimpleName();
			}

			log.error("Error processing request " + simpleName, e);
			notifyOnError(request);
			return;
		}

		GetsResponse response;
		try {
			response = GetsResponse.parse(responseString, request.getContentClass());
		} catch (GetsException e) {
			log.error("Error parsing response", e);
			notifyOnError(request);
			return;
		}

		if (checkTokenError(response)) {
			log.error("Invalid token error from GeTS");
			return;
		}

		notifyOnPostProcess(request, response);
	}

	private void notifyOnError(final GetsRequest request) {
		request.getHandler().post(new Runnable() {
			@Override
			public void run() {
				request.onError();
			}
		});
	}

	private void notifyOnPostProcess(final GetsRequest request, final GetsResponse response) {
		request.getHandler().post(new Runnable() {
			@Override
			public void run() {
				request.onPostProcess(response);
			}
		});
	}

	private boolean checkTokenError(GetsResponse response) {
		// TODO: Gets should return more specific codes
		//noinspection RedundantIfStatement
		if (response.getCode() != 0 && response.getMessage().toLowerCase().contains("token")) {
			return true;
		} else {
			return false;
		}
	}

	private static volatile Gets instance;
	public static Gets getInstance() {
		if (instance == null) {
			synchronized (Gets.class) {
				if (instance == null) {
					instance = new Gets(App.getContext());
				}
			}
		}

		return instance;
	}

	public void setToken(TokenContent tokenContent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit().putString(GetsStorage.PREF_AUTH_TOKEN, tokenContent.getAccessToken()).apply();
	}

	public String getToken() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);
	}

	void writeTokenTag(XmlSerializer serializer) throws IOException {
		String token = getToken();
		if (token != null) {
			serializer.startTag(null, "auth_token").text(token).endTag(null, "auth_token");
		}
	}
}
