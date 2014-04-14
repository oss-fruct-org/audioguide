package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.WebViewDialog;
import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.TokenContent;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.Utils;

import java.io.File;
import java.io.IOException;

public class GetsFragment extends Fragment implements WebViewDialog.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
	private MultiPanel multiPanel;

	private TextView loginLabel;


	// TODO: store between screen rotations
	private String sessionId;

	public static GetsFragment newInstance() {
		return new GetsFragment();
	}
	public GetsFragment() {
	}

	private void setAnonBoxState(Button signInButton, CheckBox anonCheckBox) {
		signInButton.setEnabled(!anonCheckBox.isChecked());
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.edit().putBoolean(GetsStorage.PREF_AUTH_ANON, anonCheckBox.isChecked()).apply();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gets, container, false);
		assert view != null;

		final Button signInButton = (Button) view.findViewById(R.id.sign_in_button);
		final Button manageFilesButton = (Button) view.findViewById(R.id.manage_files_button);
		final CheckBox anonCheckBox = ((CheckBox) view.findViewById(R.id.anon_check));
		loginLabel = ((TextView) view.findViewById(R.id.login_label));

		anonCheckBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setAnonBoxState(signInButton, anonCheckBox);
			}
		});

		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					authenticate();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		manageFilesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FileManagerFragment fileManager = FileManagerFragment.newInstance();
				multiPanel.replaceFragment(fileManager, GetsFragment.this);
			}
		});

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		anonCheckBox.setChecked(pref.getBoolean(GetsStorage.PREF_AUTH_ANON, false));
		setAnonBoxState(signInButton, anonCheckBox);

		pref.registerOnSharedPreferenceChangeListener(this);

		initializeLoginLabel();

		return view;
	}

	@Override
	public void onDestroyView() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroyView();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			multiPanel = (MultiPanel) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement MultiPanel");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		multiPanel = null;
	}

	private void authenticate() {
		AsyncTask<Void, Void, String> stage1Task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				GetsResponse response;
				try {
					String responseString = Utils.downloadUrl(GetsStorage.GETS_SERVER + "/userLogin.php", GetsStorage.LOGIN_STAGE_1);
					response = GetsResponse.parse(responseString, AuthRedirectResponse.class);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				} catch (GetsException e) {
					e.printStackTrace();
					return null;
				}
				AuthRedirectResponse redirect = ((AuthRedirectResponse) response.getContent());

				sessionId = redirect.getSessionId();
				return redirect.getRedirectUrl();
			}

			@Override
			protected void onPostExecute(String redirectUrl) {
				WebViewDialog authDialog = new WebViewDialog(redirectUrl, new Predicate<String>() {
					@Override
					public boolean apply(String url) {
						return url.startsWith(GetsStorage.GETS_SERVER + "/include/GoogleAuth.php");
					}
				});
				authDialog.show(getFragmentManager(), "auth-dialog");
				authDialog.setListener(GetsFragment.this);

			}
		};
		stage1Task.execute();
	}

	private void authenticateStage2() {
		AsyncTask<Void, Void, String> stage2Task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				GetsResponse response;
				try {
					String responseString = Utils.downloadUrl(GetsStorage.GETS_SERVER + "/userLogin.php",
							String.format(GetsStorage.LOGIN_STAGE_2, sessionId));
					response = GetsResponse.parse(responseString, TokenContent.class);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				} catch (GetsException e) {
					e.printStackTrace();
					return null;
				}

				if (response.getCode() != 0) {
					Toast.makeText(getActivity(), "Error: " + response.getMessage(), Toast.LENGTH_LONG).show();
					return null;
				}

				TokenContent tokenContent = ((TokenContent) response.getContent());
				return tokenContent.getAccessToken();
			}

			@Override
			protected void onPostExecute(String accessToken) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
				pref.edit().putString(GetsStorage.PREF_AUTH_TOKEN, accessToken).apply();
			}
		};
		stage2Task.execute();
	}

	@Override
	public void onSuccess() {
		authenticateStage2();
	}

	private void initializeLoginLabel() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String token = pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);

		if (token == null) {
			loginLabel.setText("Not signed in");
		} else {
			loginLabel.setText("Signed in");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(GetsStorage.PREF_AUTH_TOKEN)) {
			initializeLoginLabel();
		}
	}
}
