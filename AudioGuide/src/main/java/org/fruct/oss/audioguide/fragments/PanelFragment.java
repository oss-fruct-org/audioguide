package org.fruct.oss.audioguide.fragments;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;

import org.fruct.oss.audioguide.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PanelFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PanelFragment extends Fragment {
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PanelFragment.
     */
    public static PanelFragment newInstance() {
        PanelFragment fragment = new PanelFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }
    public PanelFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_panel, container, false);

		view.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getFragmentManager().beginTransaction()
						.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
						.remove(PanelFragment.this)
						.commit();

			}
		});

		return view;
	}


}
