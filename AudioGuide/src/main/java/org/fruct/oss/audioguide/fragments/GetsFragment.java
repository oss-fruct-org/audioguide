package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.dialogs.WebViewDialog;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.gets.LoginStage1Request;
import org.fruct.oss.audioguide.gets.LoginStage2Request;
import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.track.GetsBackend;

public class GetsFragment extends Fragment implements WebViewDialog.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
	private MultiPanel multiPanel;

	private TextView loginLabel;


	// TODO: store between screen rotations
	private String sessionId;
	private SharedPreferences pref;
	private Button logoutButton;
	//private Button manageFilesButton;
	private Button signInButton;

	public static GetsFragment newInstance() {
		return new GetsFragment();
	}
	public GetsFragment() {
	}

	private void logout() {
		pref.edit().remove(GetsBackend.PREF_AUTH_TOKEN).apply();
		initializeLoginLabel();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gets, container, false);
		assert view != null;

		signInButton = (Button) view.findViewById(R.id.sign_in_button);
		//manageFilesButton = (Button) view.findViewById(R.id.manage_files_button);
		logoutButton = ((Button) view.findViewById(R.id.logout_button));
		loginLabel = ((TextView) view.findViewById(R.id.login_label));

		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				logout();
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

		/*manageFilesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FileManagerFragment fileManager = FileManagerFragment.newInstance(false);
				multiPanel.replaceFragment(fileManager, GetsFragment.this);
			}
		});*/

		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
		Gets gets = Gets.getInstance();
		gets.addRequest(new LoginStage1Request(gets) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				if (response.getCode() == 1) {
					return;
				}

				AuthRedirectResponse redirect = (AuthRedirectResponse) response.getContent();
				sessionId = redirect.getSessionId();

				WebViewDialog authDialog = new WebViewDialog(redirect.getRedirectUrl(), new Predicate<String>() {
					@Override
					public boolean apply(String url) {
						return url.startsWith(Gets.GETS_SERVER + "/include/GoogleAuth.php");
					}
				});
				authDialog.show(getFragmentManager(), "auth-dialog");
				authDialog.setListener(GetsFragment.this);
			}

			@Override
			protected void onError() {
				showError("Error login");
			}
		});
	}

	private void authenticateStage2() {
		Gets gets = Gets.getInstance();
		gets.addRequest(new LoginStage2Request(gets, sessionId) {
			@Override
			protected void onError() {
				showError("Error login");
			}
		});
	}

	@Override
	public void onSuccess() {
		authenticateStage2();
	}

	private void initializeLoginLabel() {
		String token = pref.getString(GetsBackend.PREF_AUTH_TOKEN, null);

		if (token == null) {
			loginLabel.setText("Not signed in");
		} else {
			loginLabel.setText("Signed in");
		}

		if (pref.getString(GetsBackend.PREF_AUTH_TOKEN, null) != null) {
			logoutButton.setVisibility(View.VISIBLE);
			//manageFilesButton.setVisibility(View.VISIBLE);
		} else {
			logoutButton.setVisibility(View.GONE);
			//manageFilesButton.setVisibility(View.GONE);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(GetsBackend.PREF_AUTH_TOKEN)) {
			initializeLoginLabel();
		}
	}

	private void showError(final String error) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
				}
			});
	}
}
