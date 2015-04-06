package org.fruct.oss.audioguide.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import org.fruct.oss.audioguide.track.gets.Gets;
import org.fruct.oss.audioguide.track.gets.GetsException;
import org.fruct.oss.audioguide.track.gets.GetsResponse;
import org.fruct.oss.audioguide.track.gets.parsers.AuthParameters;
import org.fruct.oss.audioguide.track.gets.parsers.AuthParametersParser;
import org.fruct.oss.audioguide.track.gets.parsers.TokenContent;
import org.fruct.oss.audioguide.track.gets.parsers.TokenContentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class GooglePlayServicesHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ServerAuthCodeCallbacks {
	private static final Logger log = LoggerFactory.getLogger(GooglePlayServicesHelper.class);

	public static final int RC_SIGN_IN = 1;
	public static final int RC_GET_CODE = 2;
	public static final int RC_CHECK = 3;

	private final Activity activity;
	private Listener listener;

	private GoogleApiClient client;

	private boolean intentInProgress;

	private String scope;
	private String clientId;

	private AsyncTask<Void, Void, AuthParameters> stage1Task;
	private Handler handler = new Handler(Looper.getMainLooper());

	public GooglePlayServicesHelper(Activity activity) {
		this.activity = activity;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public static boolean isAvailable(Context context) {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		return status == ConnectionResult.SUCCESS || GooglePlayServicesUtil.isUserRecoverableError(status);
	}

	public boolean check() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (status == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(status, activity, RC_CHECK);
			errorDialog.show();
			return false;
		} else {
			return false;
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
	}

	@Override
	public void onConnectionSuspended(int i) {
		client.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (!intentInProgress && connectionResult.hasResolution()) {
			try {
				intentInProgress = true;
				activity.startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
						RC_SIGN_IN, null, 0, 0, 0);
			} catch (IntentSender.SendIntentException e) {
				// The intent was canceled before it was sent.  Return to the default
				// state and attempt to connect to get an updated ConnectionResult.
				intentInProgress = false;
				client.connect();
			}
		} else {
			if (listener != null) {
				listener.onGoogleAuthFailed();
			}
		}
	}

	public void onActivityResult(int requestCode, int responseCode, Intent intent) {
		if (requestCode == RC_SIGN_IN) {
			intentInProgress = false;

			if (!client.isConnecting() && responseCode == Activity.RESULT_OK) {
				client.connect();
			}
		}
	}

	public void login() {
		startStage1();
	}

	public void interrupt() {
		if (client != null && client.isConnected()) {
			client.disconnect();
		}

		if (stage1Task != null) {
			stage1Task.cancel(true);
		}
	}


	private void startStage1() {
		stage1Task = new AsyncTask<Void, Void, AuthParameters>() {
			@Override
			protected AuthParameters doInBackground(Void... params) {
				String request = "<request><params/></request>";
				try {
					String response = Utils.downloadUrl(Gets.GETS_SERVER + "/auth/getAuthParameters.php", request);
					GetsResponse<AuthParameters> getsResponse = GetsResponse.parse(response, new AuthParametersParser());

					if (getsResponse.getCode() != 0) {
						return null;
					}

					return getsResponse.getContent();
				} catch (IOException e) {
					return null;
				} catch (GetsException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(AuthParameters authParameters) {
				super.onPostExecute(authParameters);

				if (authParameters == null) {
					notifyAuthFailed();
					return;
				}

				scope = authParameters.getScope();
				clientId = authParameters.getClientId();
				startStage2();
			}
		};

		stage1Task.execute();
	}

	private void startStage2() {
		client = new GoogleApiClient.Builder(activity)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(new Scope("profile"))
				.requestServerAuthCode(clientId, this)
				.build();
		client.connect();
	}

	private String createExchangeRequest(String exchangeToken) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			serializer.startTag(null, "exchange_token").text(exchangeToken).endTag(null, "exchange_token");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.endDocument();

			return writer.toString();
		} catch (IOException ignore) {
			throw new RuntimeException("StringWriter throw IOException");
		}
	}

	@Override
	public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> set) {
		Set<Scope> scopes = new HashSet<>();

		StringTokenizer tokenizer = new StringTokenizer(scope, " ");

		while (tokenizer.hasMoreElements()) {
			scopes.add(new Scope(tokenizer.nextToken()));
		}

		return CheckResult.newAuthRequiredResult(scopes);
	}

	@Override
	public boolean onUploadServerAuthCode(String idToken, String serverAuthCode) {
		String request = createExchangeRequest(serverAuthCode);

		try {
			String response = Utils.downloadUrl(Gets.GETS_SERVER + "/auth/exchangeToken.php", request);
			GetsResponse<TokenContent> getsResponse = GetsResponse.parse(response, new TokenContentParser());

			if (getsResponse.getCode() != 0) {
				notifyAuthFailed();
				return false;
			}

			notifyAuthCompleted(getsResponse.getContent().getAccessToken());

			return true;
		} catch (IOException e) {
			notifyAuthFailed();
			return false;
		} catch (GetsException e) {
			notifyAuthFailed();
			return false;
		}
	}

	private void notifyAuthFailed() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (listener != null) {
					listener.onGoogleAuthFailed();
				}
			}
		});
	}

	private void notifyAuthCompleted(final String getsToken) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (listener != null) {
					listener.onGoogleAuthCompleted(getsToken);
				}
			}
		});
	}

	public interface Listener {
		void onGoogleAuthFailed();
		void onGoogleAuthCompleted(String getsToken);
	}
}