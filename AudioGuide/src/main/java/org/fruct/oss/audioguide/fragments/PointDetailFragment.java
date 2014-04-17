package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.track.AudioService;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;

public class PointDetailFragment extends Fragment implements FileListener {
    private static final String ARG_POINT = "point";
	private static final String STATE_POINT = "point";

	private Point point;

	private MultiPanel multiPanel;
	private FileManager fileManager;

	private String pendingUrl;
	private ImageView imageView;
	private Bitmap imageBitmap;

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

		if (savedInstanceState != null) {
			point = savedInstanceState.getParcelable(STATE_POINT);
		}

		fileManager = FileManager.getInstance();
		fileManager.addWeakListener(this);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_point_detail, container, false);
		assert view != null;

		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		textView.setText(point.getName());

		imageView = (ImageView) view.findViewById(android.R.id.icon);
		tryUpdateImage();

		setupAudioButton(view);

		return view;
	}

	private void setupAudioButton(View view) {
		final Button buttonPlay = (Button) view.findViewById(R.id.button_play);
		final Button buttonDetails = (Button) view.findViewById(R.id.button_details);
		final Button buttonStop = (Button) view.findViewById(R.id.button_stop);

		buttonPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (point == null)
					return;

				Intent intent = new Intent(AudioService.ACTION_PLAY,
						Uri.parse(point.getAudioUrl()),
						getActivity(), AudioService.class);
				getActivity().startService(intent);

				buttonPlay.setVisibility(View.GONE);
				buttonStop.setVisibility(View.VISIBLE);
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(AudioService.ACTION_STOP,
						null,
						getActivity(), AudioService.class);
				getActivity().startService(intent);

				buttonPlay.setVisibility(View.VISIBLE);
				buttonStop.setVisibility(View.GONE);
			}
		});

		if (point.hasAudio()) {
			buttonPlay.setVisibility(View.VISIBLE);
		} else {
			buttonPlay.setVisibility(View.GONE);
		}

		buttonStop.setVisibility(View.GONE);
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
	public void onDestroyView() {
		super.onDestroyView();

		pendingUrl = null;
		if (imageBitmap != null) {
			imageBitmap.recycle();
			imageBitmap = null;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_POINT, point);
	}

	private void tryUpdateImage() {
		if (point.hasPhoto()) {
			String remoteUrl = point.getPhotoUrl();
			Bitmap newBitmap = fileManager.getImageFullBitmap(remoteUrl, Utils.getDP(128), Utils.getDP(128));

			if (newBitmap == null && pendingUrl == null) {
				pendingUrl = remoteUrl;
				return;
			}

			imageView.setImageDrawable(new BitmapDrawable(Resources.getSystem(), newBitmap));

			if (imageBitmap != null) {
				imageBitmap.recycle();
			}

			imageBitmap = newBitmap;
			pendingUrl = null;
		}
	}

	@Override
	public void itemLoaded(String url) {
		if (url.equals(pendingUrl)) {
			tryUpdateImage();
		}
	}
}
