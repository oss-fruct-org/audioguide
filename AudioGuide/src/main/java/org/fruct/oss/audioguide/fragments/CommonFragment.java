package org.fruct.oss.audioguide.fragments;



import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.AudioService;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link CommonFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class CommonFragment extends Fragment {
	private final static Logger log = LoggerFactory.getLogger(CommonFragment.class);

	private MenuItem navigateAction;

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 * @return A new instance of fragment CommonFragment.
	 */
	public static CommonFragment newInstance() {
		return new CommonFragment();
	}
	public CommonFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log.debug("onCreate");

		setHasOptionsMenu(true);
		setRetainInstance(true);

		getActivity().startService(new Intent(getActivity(), TrackingService.class));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		getActivity().stopService(new Intent(getActivity(), TrackingService.class));
		getActivity().stopService(new Intent(getActivity(), AudioService.class));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		return null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.navigate, menu);
		navigateAction = menu.findItem(R.id.action_navigate);

		updateMenuIcon();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_navigate:
			if (AudioService.isRunning(getActivity()))
				getActivity().stopService(new Intent(getActivity(), AudioService.class));
			else
				getActivity().startService(new Intent(getActivity(), AudioService.class));

			updateMenuIcon();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void updateMenuIcon() {
		if (AudioService.isRunning(getActivity())) {
			navigateAction.setTitle("Unfollow");
			navigateAction.setIcon(R.drawable.ic_action_volume_muted);
		} else {
			navigateAction.setTitle("Follow");
			navigateAction.setIcon(R.drawable.ic_action_volume_on);
		}
	}
}
