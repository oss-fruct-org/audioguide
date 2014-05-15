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

	public Gets(Context context) {
		this.context = context;

		requestQueue = new PriorityQueue<GetsRequest>(10, new Comparator<GetsRequest>() {
			@Override
			public int compare(GetsRequest getsRequest, GetsRequest getsRequest2) {
				return getsRequest.getPriority() - getsRequest2.getPriority();
			}
		});

		new Thread(this).start();
	}

	public GetsResponse addRequestSync(final GetsRequest request) {
		synchronized (requestQueue) {
			requestQueue.add(request);
			requestQueue.notifyAll();
			Handler handler = new Handler();
			request.setHandler(handler);
		}

		synchronized (request) {
			while (!request.isCompleted()) {
				try {
					request.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}
		}

		return request.getResponse();
	}

	public void addRequest(GetsRequest request) {
		synchronized (requestQueue) {
			requestQueue.add(request);
			requestQueue.notifyAll();

			request.setHandler(new Handler());
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

			try {
				processRequest(request);
			} finally {
				synchronized (request) {
					request.setCompleted();
					request.notifyAll();
				}
			}
		}
	}

	private void processRequest(GetsRequest request) {
		String requestString = request.createRequestString();
		String requestUrl = request.getRequestUrl();
		String responseString = null;

		try {
			responseString = Utils.downloadUrl(requestUrl, requestString);
		} catch (IOException e) {
			log.error("Error processing request " + request.getClass().getSimpleName(), e);
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

		request.setResponse(response);
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
