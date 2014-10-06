package org.fruct.oss.audioguide.fragments;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class AboutFragment extends Fragment implements View.OnClickListener {
	private static final Logger log = LoggerFactory.getLogger(AboutFragment.class);

	private Deque<Long> times = new ArrayDeque<Long>(5);

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public AboutFragment() {
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
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_about, container, false);

		TextView textView = (TextView) view.findViewById(R.id.textView);
		textView.setOnClickListener(this);

		return view;
	}


	@Override
	public void onClick(View v) {
		times.addLast(System.currentTimeMillis());
		checkTimes();
	}

	private void checkTimes() {
		if (times.size() < 5)
			return;

		while (times.size() > 5) {
			times.removeFirst();
		}

		int idx = 0;
		long[] tt = new long[5];
		for (long t : times) {
			tt[idx++] = t;
		}

		long d1 = tt[1] - tt[0];
		long d2 = tt[2] - tt[1];

		long d3 = tt[3] - tt[2];
		long d4 = tt[4] - tt[3];

		long da = tt[2] - tt[0];
		long db = tt[4] - tt[2];

		log.trace("EA: " + d1 + " " + d2 + " " + d3 + " " + d4 + ": " + da + " " + db);

		if (percent(d1, d2) < 0.2 && percent(d3, d4) < 0.2 && percent(da, db * 2) < 0.3) {
			Toast.makeText(getActivity(), Config.isEditLocked() ? "Edit mode activated" : "Edit mode deactivated", Toast.LENGTH_SHORT).show();
			Config.toggleEditLocked(getActivity());
		}
	}

	private double percent(long a, long b) {
		long min = Math.min(a, b);
		long max = Math.max(a, b);

		double ret = (double) max / min - 1.0;
		return ret;
	}
}
