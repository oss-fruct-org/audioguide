package org.fruct.oss.audioguide.fragments;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.SeekBar;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.AudioPlayer;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PanelFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PanelFragment extends Fragment {
	private static final Logger log = LoggerFactory.getLogger(PanelFragment.class);

	private BroadcastReceiver positionReceiver;

	private SeekBar seekBar;
	private int duration;

	private boolean isDragging;
	private int lastProgress;

	/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PanelFragment.
     */
    public static PanelFragment newInstance(int duration) {
        PanelFragment fragment = new PanelFragment();
        Bundle args = new Bundle();
		args.putInt("duration", duration);

        fragment.setArguments(args);
        return fragment;
    }
    public PanelFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
			duration = getArguments().getInt("duration");
		}

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(positionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (seekBar != null && !isDragging) {
					seekBar.setProgress(intent.getIntExtra("position", 0));
				}
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_POSITION));

	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(positionReceiver);
		positionReceiver = null;

		super.onDestroy();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_panel, container, false);

		view.findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startService(new Intent(TrackingService.ACTION_STOP, null, getActivity(), TrackingService.class));
			}
		});

		final View playButton = view.findViewById(R.id.play_button);
		final View pauseButton = view.findViewById(R.id.pause_button);

		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.VISIBLE);
				getActivity().startService(new Intent(TrackingService.ACTION_UNPAUSE, null,
						getActivity(), TrackingService.class));
			}
		});

		pauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				pauseButton.setVisibility(View.GONE);
				playButton.setVisibility(View.VISIBLE);
				getActivity().startService(new Intent(TrackingService.ACTION_PAUSE, null,
						getActivity(), TrackingService.class));
			}
		});

		seekBar = ((SeekBar) view.findViewById(R.id.seek_bar));
		seekBar.setMax(duration);

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
				if (fromUser) {
					lastProgress = position;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				isDragging = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				isDragging = false;
				Intent intent = new Intent(TrackingService.ACTION_SEEK, null, getActivity(), TrackingService.class);
				intent.putExtra("position", lastProgress);
				getActivity().startService(intent);
			}
		});

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		seekBar = null;
	}
}
