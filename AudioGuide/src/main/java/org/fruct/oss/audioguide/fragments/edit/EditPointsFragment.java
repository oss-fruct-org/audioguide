package org.fruct.oss.audioguide.fragments.edit;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.PointAdapter;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

import java.util.List;

public class EditPointsFragment extends ListFragment implements TrackManager.Listener {
	private static final String ARG_TRACK = "arg-track";

	private MultiPanel multiPanel;
	private Track track;
	private TrackManager trackManager;

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
		trackManager.addListener(this);

		setupAdapter();

        //setListAdapter(new ArrayAdapter<DummyContent.DummyItem>(getActivity(),
        //        android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS));
    }

	@Override
	public void onDestroy() {
		trackManager.removeListener(this);

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


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

	@Override
	public void tracksUpdated() {

	}

	@Override
	public void pointsUpdated(Track track) {

	}

	private void setupAdapter() {
		List<Point> points = trackManager.getPoints(track);
		PointAdapter adapter = new PointAdapter(getActivity(), R.layout.list_point_item, points);
		setListAdapter(adapter);
	}
}
