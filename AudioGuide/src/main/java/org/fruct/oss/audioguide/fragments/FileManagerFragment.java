package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import org.fruct.oss.audioguide.MultiPanel;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link org.fruct.oss.audioguide.MultiPanel}
 * interface.
 */
public class FileManagerFragment extends ListFragment {
	private MultiPanel multiPanel;

	// TODO: Rename and change types of parameters
    public static FileManagerFragment newInstance() {
        FileManagerFragment fragment = new FileManagerFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileManagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
        }
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
