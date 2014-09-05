package org.fruct.oss.audioguide.fragments;



import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.AudioPlayer;
import org.fruct.oss.audioguide.track.Point;
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
	private BroadcastReceiver stopReceiver;
	private BroadcastReceiver startReceiver;

	private SeekBar seekBar;

	private int duration = -1;
	private boolean isStarted;

	private Point fallbackPoint;
	private Point point;

	private boolean isDragging;

	private int lastProgress;
	private View playButton;
	private View pauseButton;
	private PointDetailFragment detailsFragment;

	/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PanelFragment.
     */
    public static PanelFragment newInstance(Point fallbackPoint, int duration, Point playingPoint) {
        PanelFragment fragment = new PanelFragment();

        Bundle args = new Bundle();
		args.putInt("duration", duration);
		args.putParcelable("fallbackPoint", fallbackPoint);
		args.putParcelable("point", playingPoint);

		fragment.setArguments(args);
        return fragment;
    }

    public PanelFragment() {
    }

	public boolean isStarted() {
		return isStarted;
	}

	public void startPlaying(int duration) {
		this.duration = duration;

		pauseButton.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);

		seekBar.setMax(duration);
		isStarted = true;
	}

	public void stopPlaying() {
		isStarted = false;
		duration = -1;

		pauseButton.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		seekBar.setProgress(0);

		if (fallbackPoint == null) {
			getActivity().getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
					.remove(this).commit();
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		log.trace("onCreate");

		setRetainInstance(true);

		if (getArguments() != null) {
			duration = getArguments().getInt("duration", -1);
			fallbackPoint = getArguments().getParcelable("fallbackPoint");
			point = getArguments().getParcelable("point");
		}

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(positionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (seekBar != null && !isDragging) {
					seekBar.setProgress(intent.getIntExtra("position", 0));
				}
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_POSITION));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(startReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				startPlaying(intent.getIntExtra("duration", -1));
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_START_PLAY));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(stopReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				stopPlaying();
				if (getFragmentManager().findFragmentByTag("details-fragment") == detailsFragment) {
					getFragmentManager().popBackStack("details-fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
					detailsFragment = null;
				}
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_STOP_PLAY));
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(positionReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(stopReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(startReceiver);
		positionReceiver = null;
		stopReceiver = null;
		stopReceiver = null;

		log.trace("onDestroy");
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		log.trace("onAttach");
	}

	@Override
	public void onDetach() {
		super.onDetach();
		log.trace("onDetach");
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

		seekBar = ((SeekBar) view.findViewById(R.id.seek_bar));
		playButton = view.findViewById(R.id.play_button);
		pauseButton = view.findViewById(R.id.pause_button);

		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isStarted()) {
					pauseButton.setVisibility(View.VISIBLE);
					playButton.setVisibility(View.GONE);

					getActivity().startService(new Intent(TrackingService.ACTION_UNPAUSE, null,
							getActivity(), TrackingService.class));
				} else if (fallbackPoint != null) {
					Intent intent = new Intent(TrackingService.ACTION_PLAY, null,
							getActivity(), TrackingService.class);
					intent.putExtra(TrackingService.ARG_POINT, fallbackPoint);
					getActivity().startService(intent);
				}
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


		if (duration != -1) {
			startPlaying(duration);
			if (getFragmentManager().findFragmentByTag("details-fragment") == null) {
				detailsFragment = PointDetailFragment.newInstance(point, true);
				getFragmentManager().beginTransaction()
						.addToBackStack("details-fragment")
						.add(R.id.panel_details, detailsFragment, "details-fragment")
						.commit();
			}
		} else {
			playButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.GONE);
		}

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		seekBar = null;
		playButton = null;
		pauseButton = null;
	}

	public void setFallbackPoint(Point point) {
		this.fallbackPoint = point;
	}

	public void clearFallbackPoint() {
		this.fallbackPoint = null;

		if (!isStarted()) {
			FragmentActivity activity = getActivity();
			activity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
					.remove(this).commitAllowingStateLoss();
		}
	}
}
