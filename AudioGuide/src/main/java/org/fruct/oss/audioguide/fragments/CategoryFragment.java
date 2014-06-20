package org.fruct.oss.audioguide.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.fruct.oss.audioguide.R;

public class CategoryFragment extends Fragment {
	private ExpandableListView listView;

	public static CategoryFragment newInstance() {
		return new CategoryFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_category, container, false);

		listView = (ExpandableListView) view.findViewById(R.id.list_view);

		return view;
	}
}
