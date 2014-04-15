package org.fruct.oss.audioguide.fragments.edit;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.PointModelAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

public class EditPointsFragment extends ListFragment {
	private static final String ARG_TRACK = "arg-track";

	private MultiPanel multiPanel;
	private Track track;
	private TrackManager trackManager;

	private PointModelAdapter adapter;

	public static EditPointsFragment newInstance(Track track) {
		EditPointsFragment fragment = new EditPointsFragment();

		Bundle args = new Bundle();
		args.putParcelable(ARG_TRACK, track);
		fragment.setArguments(args);

		return fragment;
	}

    public EditPointsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			track = getArguments().getParcelable(ARG_TRACK);
		}

		trackManager = TrackManager.getInstance();

		adapter = new PointModelAdapter(getActivity(), R.layout.list_point_item, trackManager.getPointsModel(track));
		setListAdapter(adapter);
	}

	@Override
	public void onDestroy() {
		adapter.close();

		super.onDestroy();
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
}
