package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.GetsLoginActivity;
import org.fruct.oss.audioguide.MainActivity;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.gets.Gets;
import org.fruct.oss.audioguide.track.gets.GetsException;
import org.fruct.oss.audioguide.track.gets.GetsResponse;
import org.fruct.oss.audioguide.track.gets.parsers.UserInfo;
import org.fruct.oss.audioguide.track.gets.parsers.UserInfoParser;
import org.fruct.oss.audioguide.util.GooglePlayServicesHelper;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class GetsFragment extends Fragment implements View.OnClickListener, GooglePlayServicesHelper.Listener,
		MainActivity.ActivityResultListener {
	private static final Logger log = LoggerFactory.getLogger(GetsFragment.class);

	private static final String PREF_AUTH_TOKEN = "pref_auth_token";
	private static final String PREF_USER_INFO = "pref_user_info";

	private GooglePlayServicesHelper googlePlayServicesHelper;
	private static final int RC_GETS_FRAGMENT = 4;

	private View userInfoLayout;
	private ImageView userInfoImageView;
	private TextView userInfoTextView;

	private Button webLoginButton;
	private Button googleLoginButton;
	private Button logoutButton;

	private UserInfoTask userInfoTask;
	private LogoutTask logoutTask;

	private SharedPreferences pref;

	public static GetsFragment newInstance() {
		return new GetsFragment();
	}

	public GetsFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section3),
				ActionBar.NAVIGATION_MODE_STANDARD, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gets, container, false);

		webLoginButton = (Button) view.findViewById(R.id.button_login_web_browser);
		googleLoginButton = (Button) view.findViewById(R.id.button_login_google);
		logoutButton = (Button) view.findViewById(R.id.button_logout);

		userInfoLayout = view.findViewById(R.id.layout_user_info);
		userInfoTextView = (TextView) view.findViewById(R.id.text_user_name);
		userInfoImageView = (ImageView) view.findViewById(android.R.id.icon);

		webLoginButton.setOnClickListener(this);
		googleLoginButton.setOnClickListener(this);
		logoutButton.setOnClickListener(this);

		updateViewState();

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public void onStart() {
		super.onStart();
		updateViewState();
	}

	@Override
	public void onDestroy() {
		if (googlePlayServicesHelper != null) {
			googlePlayServicesHelper.setListener(null);
			googlePlayServicesHelper.interrupt();
		}

		if (userInfoTask != null) {
			userInfoTask.cancel(true);
		}

		if (logoutTask != null) {
			logoutTask.cancel(true);
		}

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_login_web_browser:
			Intent intent = new Intent(getActivity(), GetsLoginActivity.class);
			startActivityForResult(intent, RC_GETS_FRAGMENT);
			break;

		case R.id.button_login_google:
			startGoogleLogin();
			break;

		case R.id.button_logout:
			String getsToken = pref.getString(PREF_AUTH_TOKEN, null);

			if (getsToken != null) {
				logoutTask = new LogoutTask();
				logoutTask.execute(getsToken);
			}

			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RC_GETS_FRAGMENT) {
			if (resultCode != Activity.RESULT_OK) {
				Toast.makeText(getActivity(), R.string.str_google_login_web_error, Toast.LENGTH_LONG).show();
			} else {
				onGoogleAuthCompleted(data.getStringExtra("auth_token"));
			}
		}
	}

	@Override
	public void onActivityResultRedirect(int requestCode, int resultCode, Intent data) {
		if ((requestCode == GooglePlayServicesHelper.RC_SIGN_IN
				|| requestCode == GooglePlayServicesHelper.RC_GET_CODE)
				&& googlePlayServicesHelper != null) {
			googlePlayServicesHelper.onActivityResult(requestCode, resultCode, data);
		} else if (requestCode == GooglePlayServicesHelper.RC_CHECK || resultCode == Activity.RESULT_OK) {
			startGoogleLogin();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		String getsToken = pref.getString(PREF_AUTH_TOKEN, null);

		menu.findItem(R.id.action_refresh).setVisible(getsToken != null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			String getsToken = pref.getString(PREF_AUTH_TOKEN, null);

			if (getsToken != null) {
				userInfoTask = new UserInfoTask();
				userInfoTask.execute(getsToken);
			}
			return true;
		} else {
			return false;
		}
	}

	private void updateViewState() {
		String getsToken = pref.getString(PREF_AUTH_TOKEN, null);
		boolean isLogged = getsToken != null;

		if (isLogged) {
			webLoginButton.setVisibility(View.GONE);
			googleLoginButton.setVisibility(View.GONE);
			logoutButton.setVisibility(View.VISIBLE);
		} else {
			webLoginButton.setVisibility(View.VISIBLE);
			googleLoginButton.setVisibility(GooglePlayServicesHelper.isAvailable(getActivity())
					? View.VISIBLE : View.GONE);
			logoutButton.setVisibility(View.GONE);
		}

		UserInfo userInfo = UserInfo.load(pref, PREF_USER_INFO);
		if (userInfo != null) {
			if (userInfo.getImageUrl() != null) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.displayImage(sizedImageUrl(userInfo.getImageUrl(),
						Utils.getDP(64)), userInfoImageView);
			}

			userInfoTextView.setText(userInfo.getName());
			userInfoLayout.setVisibility(View.VISIBLE);
		} else {
			userInfoLayout.setVisibility(View.GONE);
		}
	}

	private void startGoogleLogin() {
		googlePlayServicesHelper = new GooglePlayServicesHelper(getActivity());
		googlePlayServicesHelper.setListener(this);

		if (googlePlayServicesHelper.check()) {
			googlePlayServicesHelper.login();
		} else {
			onGoogleAuthFailed();
		}
	}

	@Override
	public void onGoogleAuthFailed() {
		Toast.makeText(getActivity(), R.string.str_google_login_error, Toast.LENGTH_LONG).show();
		googlePlayServicesHelper.setListener(null);
		googlePlayServicesHelper.interrupt();
	}

	@Override
	public void onGoogleAuthCompleted(String getsToken) {
		Toast.makeText(getActivity(), R.string.str_google_login_success, Toast.LENGTH_LONG).show();

		pref.edit().putString(PREF_AUTH_TOKEN, getsToken).apply();

		updateViewState();
		getActivity().supportInvalidateOptionsMenu();

		userInfoTask = new UserInfoTask();
		userInfoTask.execute(getsToken);
	}

	private class UserInfoTask extends AsyncTask<String, Void, UserInfo> {
		@Override
		protected UserInfo doInBackground(String... params) {
			String getsToken = params[0];
			return updateUserInfo(getsToken);
		}

		@Override
		protected void onPostExecute(UserInfo userInfo) {
			if (userInfo == null) {
				return;
			}

			userInfo.save(pref, PREF_USER_INFO);

			updateViewState();
		}
	}

	private class LogoutTask extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			String getsToken = params[0];
			return logout(getsToken);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			pref.edit().remove(PREF_AUTH_TOKEN)
					.remove(PREF_USER_INFO)
					.apply();

			updateViewState();
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	private UserInfo updateUserInfo(String getsToken) {
		String request = createAuthTokenRequest(getsToken);
		try {
			String responseXml = Utils.downloadUrl(Gets.GETS_SERVER + "/userInfo.php", request);
			GetsResponse<UserInfo> response = GetsResponse.parse(responseXml, new UserInfoParser());

			return response.getContent();
		} catch (IOException | GetsException e) {
			log.error("Can't update user info", e);
			return null;
		}
	}

	private boolean logout(String getsToken) {
		String request = createAuthTokenRequest(getsToken);
		try {
			String responseXml = Utils.downloadUrl(Gets.GETS_SERVER + "/auth/revokeToken.php", request);
			GetsResponse response = GetsResponse.parse(responseXml, null);

			return response.getCode() == 0;
		} catch (IOException | GetsException e) {
			log.error("Can't update user info", e);
			return false;
		}
	}

	private String createAuthTokenRequest(String authToken) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.endDocument();

			return writer.toString();
		} catch (IOException ignore) {
			throw new RuntimeException("StringWriter thrown IOException");
		}
	}

	private String sizedImageUrl(String url, int newSize) {
		// https://lh0.googleusercontent.com/-BBBBBBBBBBBB/AAAAAAAAAAI/AAAAAAAAAYY/CCCCCCC/photo.jpg?sz=50
		int idx = url.lastIndexOf("?sz=");
		if (idx == -1)
			return url + "?sz=" + newSize;
		else
			return url.substring(0, idx) + "?sz=" + newSize;
	}
}