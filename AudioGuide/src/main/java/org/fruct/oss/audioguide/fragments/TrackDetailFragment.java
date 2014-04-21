package org.fruct.oss.audioguide.fragments;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Use the {@link TrackDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class TrackDetailFragment extends Fragment {
	private final static Logger log = LoggerFactory.getLogger(TrackDetailFragment.class);
	private static final String ARG_TRACK = "track";
	private static final String STATE_TRACK = "track";

	private Track track;
	private TrackManager trackManager;

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param track Track to show.
	 * @return A new instance of fragment TrackDetailFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static TrackDetailFragment newInstance(Track track) {
		TrackDetailFragment fragment = new TrackDetailFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_TRACK, track);
		fragment.setArguments(args);
		return fragment;
	}

	public TrackDetailFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null)
			track = getArguments().getParcelable(ARG_TRACK);

		if (savedInstanceState != null)
			track = savedInstanceState.getParcelable(STATE_TRACK);

		trackManager = TrackManager.getInstance();
	}

	@Override
	public void onDestroy() {
		trackManager = null;
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_track_detail, container, false);

		final TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setText(track.getHumanReadableName());

		final TextView desc = (TextView) view.findViewById(android.R.id.text2);
		desc.setText(track.getDescription());

		final Button downloadButton = (Button) view.findViewById(R.id.localImage);
		downloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				trackManager.storeLocal(track);
				trackManager.refreshPoints(track);
			}
		});

		final Button activateButton = (Button) view.findViewById(R.id.activeImage);
		activateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (track.isActive())
					trackManager.deactivateTrack(track);
				else
					trackManager.activateTrack(track);
				setActivateButtonText(activateButton);
			}
		});

		setActivateButtonText(activateButton);

		return view;
	}

	private void setActivateButtonText(Button button) {
		if (track.isActive())
			button.setText("Deactivate");
		else
			button.setText("Activate");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_TRACK, track);
	}
}
