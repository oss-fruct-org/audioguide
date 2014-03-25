package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Point;

public class PointDetailFragment extends Fragment {
    private static final String ARG_POINT = "point";

    private Point point;

	private MultiPanel multiPanel;

	/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param point Point.
     * @return A new instance of fragment PointDetailFragment.
     */
    public static PointDetailFragment newInstance(Point point) {
        PointDetailFragment fragment = new PointDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POINT, point);
        fragment.setArguments(args);
        return fragment;
    }

    public PointDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            point = getArguments().getParcelable(ARG_POINT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_point_detail, container, false);
		assert view != null;

		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		textView.setText(point.getName());

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


}
