package org.fruct.oss.audioguide.fragments;



import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager2;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.track.AudioPlayer;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PanelFragment extends Fragment implements FileListener {
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
	private View stopButton;

	private ProgressBar progressBar;

	private FileManager2 fileManager;
	private String loadingUrl;

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

	public void setCurrentPoint(Point point) {
		this.point = point;
	}

	public void startPlaying(int duration) {
		this.duration = duration;

		setPlayingState();

		seekBar.setMax(duration);
		isStarted = true;
	}

	public void stopPlaying() {
		isStarted = false;
		duration = -1;

		setPausedState();
		seekBar.setProgress(0);

		if (fallbackPoint == null) {
			getActivity().getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
					.remove(this).commit();
		} else {
			configureFallbackPoint();
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		log.trace("onCreate");

		setRetainInstance(true);

		fileManager = FileManager2.getInstance();

		if (getArguments() != null) {
			duration = getArguments().getInt("duration", -1);
			fallbackPoint = getArguments().getParcelable("fallbackPoint");
			point = getArguments().getParcelable("point");
		}
	}

	@Override
	public void onStart() {
		super.onStart();

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
				if (stopReceiver != null)
					startPlaying(intent.getIntExtra("duration", -1));
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_START_PLAY));

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(stopReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				stopPlaying();
				PointDetailFragment detailsFragment = (PointDetailFragment) getFragmentManager().findFragmentByTag("details-fragment");
				if (detailsFragment != null && point != null && point.equals(detailsFragment.getPoint())) {
					getFragmentManager().popBackStack("details-fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
				}
			}
		}, new IntentFilter(AudioPlayer.BC_ACTION_STOP_PLAY));
	}

	@Override
	public void onStop() {
		super.onStop();

		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(positionReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(stopReceiver);
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(startReceiver);

		positionReceiver = null;
		stopReceiver = null;
		stopReceiver = null;
	}

	@Override
	public void onDestroy() {
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
		stopButton = view.findViewById(R.id.stop_button);
		playButton = view.findViewById(R.id.play_button);
		pauseButton = view.findViewById(R.id.pause_button);
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isStarted()) {
					setPlayingState();

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
				setPausedState();
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
		} else {
			configureFallbackPoint();
		}

		return view;
	}

	private void configureFallbackPoint() {
		if (!isStarted()) {
			if (fileManager.getLocalFile(fallbackPoint.getAudioUrl(), FileSource.Variant.FULL) == null) {
				setLoadingState();
				loadingUrl = fallbackPoint.getAudioUrl();
				fileManager.addListener(this);
			} else {
				setPausedState();
			}
		}
	}

	private void setPlayingState() {
		pauseButton.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		stopButton.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
	}

	private void setPausedState() {
		pauseButton.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		stopButton.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
	}

	private void setLoadingState() {
		pauseButton.setVisibility(View.GONE);
		playButton.setVisibility(View.GONE);
		stopButton.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		fileManager.removeListener(this);

		seekBar = null;
		stopButton = null;
		playButton = null;
		pauseButton = null;
	}

	public void setFallbackPoint(Point point) {
		this.fallbackPoint = point;
		if (fileManager != null && pauseButton != null) {
			configureFallbackPoint();
		}
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

	@Override
	public void itemLoaded(String url) {
		if (loadingUrl.equals(url)) {
			setPausedState();
		}
	}

	@Override
	public void itemDownloadProgress(String url, int current, int max) {
		if (loadingUrl.equals(url)) {
			progressBar.setMax(max);
			progressBar.setProgress(current);
		}
	}
}
