package org.fruct.oss.audioguide.gets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.TokenContent;
import org.fruct.oss.audioguide.track.GetsBackend;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Gets implements Runnable {
	//public static final String GETS_SERVER = "http://getsi.ddns.net/getslocal";
	//public static final String GETS_SERVER = "http://oss.fruct.org/projects/gets/service";
	public static final String GETS_SERVER = "http://gets.cs.petrsu.ru/gets/service";

	private final static Logger log = LoggerFactory.getLogger(Gets.class);

	private final ArrayList<GetsRequest> requestQueue = new ArrayList<GetsRequest>();

	private final Context context;
	private final ConnectivityManager connectivityManager;
	private BroadcastReceiver networkStateReceiver;
	private boolean isNetworkSuspended;

	private HashMap<String, Object> requestEnvironment = new HashMap<String, Object>();
	private Handler handler;
	private boolean isRequestsBlocked;

	public Gets(Context context) {
		this.context = context;

		handler = new Handler(Looper.getMainLooper());
		connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String token = pref.getString(GetsBackend.PREF_AUTH_TOKEN, null);
		if (token != null) {
			setEnv("token", token);
		}

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	private void close() {
		if (networkStateReceiver != null) {
			context.unregisterReceiver(networkStateReceiver);
		}
	}

	public void addRequest(GetsRequest request) {
		synchronized (requestQueue) {
			requestQueue.add(request);
			requestQueue.notifyAll();
		}
	}

	public Object getEnv(String key) {
		synchronized (requestQueue) {
			return requestEnvironment.get(key);
		}
	}

	public void setEnv(String key, Object value) {
		synchronized (requestQueue) {
			if (value == null) {
				requestEnvironment.remove(key);
			} else {
				requestEnvironment.put(key, value);
			}

			requestQueue.notifyAll();
		}
	}

	@Override
	public void run() {
		isRequestsBlocked = false;
		checkNetwork();

		while (!Thread.interrupted()) {
			GetsRequest request = null;

			synchronized (requestQueue) {
				while (isRequestsBlocked || requestQueue.isEmpty() || isNetworkSuspended) {
					isRequestsBlocked = false;
					try {
						requestQueue.wait();
					} catch (InterruptedException e) {
						close();
						return;
					}

					checkNetwork();
				}

				for (Iterator<GetsRequest> iter = requestQueue.iterator(); iter.hasNext(); ) {
					GetsRequest req = iter.next();
					if (req.onPreExecute()) {
						iter.remove();
						request = req;
						break;
					}
				}
			}

			if (request != null)
				processRequest(request);
			else {
				log.debug("All requests blocked, waiting");
				isRequestsBlocked = true;
			}
		}
	}

	private boolean checkNetwork() {
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
			resumeNetwork();
			return true;
		} else {
			suspendNetwork();
			return false;
		}
	}

	private void suspendNetwork() {
		if (isNetworkSuspended) {
			return;
		}

		log.info("Network unavailable, suspending gets operations");
		isNetworkSuspended = true;

		networkStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				synchronized (requestQueue) {
					requestQueue.notifyAll();
				}
			}
		};
		context.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void resumeNetwork() {
		if (!isNetworkSuspended) {
			return;
		}

		log.info("Network available, resuming gets operations");

		isNetworkSuspended = false;
		context.unregisterReceiver(networkStateReceiver);
		networkStateReceiver = null;
	}

	private void processRequest(final GetsRequest request) {
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

		if (request.onPostExecute(response)) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					addRequest(request);
				}
			});
		} else {
			notifyOnPostProcess(request, response);
		}
	}

	private void notifyOnError(final GetsRequest request) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				request.onError();
			}
		});
	}

	private void notifyOnPostProcess(final GetsRequest request, final GetsResponse response) {
		handler.post(new Runnable() {
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

	public void setToken(TokenContent tokenContent) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit().putString(GetsBackend.PREF_AUTH_TOKEN, tokenContent.getAccessToken()).apply();

		setEnv("token", tokenContent.getAccessToken());
	}

	public String getToken() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(GetsBackend.PREF_AUTH_TOKEN, null);
	}

	void writeTokenTag(XmlSerializer serializer) throws IOException {
		String token = getToken();
		if (token != null) {
			serializer.startTag(null, "auth_token").text(token).endTag(null, "auth_token");
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
}
