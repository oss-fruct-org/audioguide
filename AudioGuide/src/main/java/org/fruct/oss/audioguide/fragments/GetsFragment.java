package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewFragment;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.android.internal.util.Predicate;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.WebViewDialog;
import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.TokenContent;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.Utils;

import java.io.IOException;

public class GetsFragment extends Fragment implements WebViewDialog.Listener {
    private MultiPanel multiPanel;

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
		final CheckBox anonCheckBox = ((CheckBox) view.findViewById(R.id.anon_check));
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

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		anonCheckBox.setChecked(pref.getBoolean(GetsStorage.PREF_AUTH_ANON, false));
		setAnonBoxState(signInButton, anonCheckBox);

		return view;
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

	private void authenticate() throws IOException, GetsException {
		// Authentication stage 1
		String responseString = Utils.downloadUrl(GetsStorage.GETS_SERVER + "/userLogin.php", GetsStorage.LOGIN_STAGE_1);
		GetsResponse response = GetsResponse.parse(responseString, AuthRedirectResponse.class);
		AuthRedirectResponse redirect = ((AuthRedirectResponse) response.getContent());

		sessionId = redirect.getSessionId();
		String redirectUrl = redirect.getRedirectUrl();

		WebViewDialog authDialog = new WebViewDialog(redirectUrl, new Predicate<String>() {
			@Override
			public boolean apply(String url) {
				return url.startsWith(GetsStorage.GETS_SERVER + "/include/GoogleAuth.php");
			}
		});
		authDialog.show(getFragmentManager(), "auth-dialog");
		authDialog.setListener(this);
	}

	@Override
	public void onSuccess() {
		// Authentication stage 2
		String responseString;
		try {
			responseString = Utils.downloadUrl(GetsStorage.GETS_SERVER + "/userLogin.php",
					String.format(GetsStorage.LOGIN_STAGE_2, sessionId));
			GetsResponse response = GetsResponse.parse(responseString, TokenContent.class);

			if (response.getCode() != 0) {
				Toast.makeText(getActivity(), "Error: " + response.getMessage(), Toast.LENGTH_LONG).show();
				return;
			}

			TokenContent tokenContent = ((TokenContent) response.getContent());

			String accessToken = tokenContent.getAccessToken();
			Toast.makeText(getActivity(), accessToken, Toast.LENGTH_LONG).show();

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			pref.edit().putString(GetsStorage.PREF_AUTH_TOKEN, accessToken).apply();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GetsException e) {
			e.printStackTrace();
		}
	}
}
