package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.files2.DefaultFileManager;
import org.fruct.oss.audioguide.files.files2.FileManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;

public class PointDetailFragment extends Fragment implements FileListener {
    private static final String ARG_POINT = "point";
	private static final String STATE_POINT = "point";
	private static final String ARG_IS_OVERLAY = "c";
	private static final String STATE_IS_OVERLAY = "overlay";

	private Point point;
	private boolean isOverlay;

	private MultiPanel multiPanel;
	private DefaultFileManager fileManager;

	private String pendingUrl;
	private ImageView imageView;
	private Bitmap imageBitmap;

	private int imageSize;

	/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param point Point.
     * @param isOverlay
	 * @return A new instance of fragment PointDetailFragment.
     */
    public static PointDetailFragment newInstance(Point point, boolean isOverlay) {
        PointDetailFragment fragment = new PointDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POINT, point);
		args.putBoolean(ARG_IS_OVERLAY, isOverlay);
        fragment.setArguments(args);
        return fragment;
    }

    public PointDetailFragment() {
    }

	@Override
	public void onStart() {
		super.onStart();

		initializeBottomPanel();
	}

	private void initializeBottomPanel() {
		PanelFragment panelFragment = (PanelFragment) getFragmentManager().findFragmentByTag("bottom-panel-fragment");

		if (panelFragment == null) {
			panelFragment = PanelFragment.newInstance(point, -1);
			getFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
					.replace(R.id.panel_container,
					panelFragment, "bottom-panel-fragment").commit();
		}

		panelFragment.setFallbackPoint(point);
	}

	@Override
	public void onStop() {
		PanelFragment panelFragment = (PanelFragment) getFragmentManager().findFragmentByTag("bottom-panel-fragment");
		if (panelFragment != null) {
			panelFragment.clearFallbackPoint();
		}

		super.onStop();
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            point = getArguments().getParcelable(ARG_POINT);
			isOverlay = getArguments().getBoolean(ARG_IS_OVERLAY);
		}

		if (savedInstanceState != null) {
			point = savedInstanceState.getParcelable(STATE_POINT);
			isOverlay = savedInstanceState.getBoolean(STATE_IS_OVERLAY);
		}

		fileManager = DefaultFileManager.getInstance();
		fileManager.addWeakListener(this);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_point_detail, container, false);
		assert view != null;

		TextView title = (TextView) view.findViewById(android.R.id.text1);
		title.setText(point.getName());

		TextView description = (TextView) view.findViewById(android.R.id.text2);
		String descriptionString = point.getDescription();

		if (!Utils.isNullOrEmpty(descriptionString)) {
			description.setText(descriptionString);
		} else {
			description.setVisibility(View.GONE);
		}

		imageView = (ImageView) view.findViewById(android.R.id.icon);

		view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				imageSize = view.getMeasuredWidth();
				tryUpdateImage(imageSize, imageSize);

				/*if (isOverlay) {
					int width = view.getMeasuredWidth();
					int height = view.getMeasuredHeight();

					width -= Utils.getDP(48);
					height -= Utils.getDP(48);

					view.setLayoutParams(new FrameLayout.LayoutParams(width, height));
				}*/
			}
		});

		setupOverlayMode(view);

		//setupAudioButton(view);
		//setupCenterButton(view);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().getSupportFragmentManager().beginTransaction()
						.remove(PointDetailFragment.this)
						.commit();
			}
		});

		return view;
	}

	private void setupOverlayMode(View view) {
		if (!isOverlay)
			return;

		Drawable background = getResources().getDrawable(R.drawable.marker_1);
		background.setColorFilter(0xccffffff, PorterDuff.Mode.MULTIPLY);

		view.setBackgroundDrawable(background);
	}

	/*private void setupCenterButton(View view) {
		final Button buttonCenter = (Button) view.findViewById(R.id.button_map);
		buttonCenter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MainActivity activity = (MainActivity) getActivity();

				NavigationDrawerFragment frag =
						(NavigationDrawerFragment)
								activity.getSupportFragmentManager()
										.findFragmentById(R.id.navigation_drawer);

				Bundle params = new Bundle();
				params.putParcelable("point", point);

				frag.selectItem(1, params);
			}
		});
	}

	private void setupAudioButton(View view) {
		final Button buttonPlay = (Button) view.findViewById(R.id.button_play);
		final Button buttonStop = (Button) view.findViewById(R.id.button_stop);

		buttonPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (point == null)
					return;

				Intent intent = new Intent(TrackingService.ACTION_PLAY,
						Uri.parse(point.getAudioUrl()),
						getActivity(), TrackingService.class);
				getActivity().startService(intent);

				buttonPlay.setVisibility(View.GONE);
				buttonStop.setVisibility(View.VISIBLE);
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(TrackingService.ACTION_STOP,
						null,
						getActivity(), TrackingService.class);
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
	}*/

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
		//if (imageBitmap != null) {
		//	imageBitmap.recycle();
		//	imageBitmap = null;
		//}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_POINT, point);
		outState.putBoolean(STATE_IS_OVERLAY, isOverlay);
	}

	private void tryUpdateImage(int imageWidth, int imageHeight) {
		if (point.hasPhoto()) {
			String remoteUrl = point.getPhotoUrl();
			Bitmap newBitmap = fileManager.getImageBitmap(remoteUrl, imageWidth, imageHeight, FileManager.ScaleMode.NO_SCALE);

			if (newBitmap == null && pendingUrl == null) {
				pendingUrl = remoteUrl;
				return;
			}

			imageView.setAdjustViewBounds(true);
			imageView.setMaxWidth(imageSize);
			imageView.setMaxHeight(imageSize);

			imageView.setImageDrawable(new BitmapDrawable(Resources.getSystem(), newBitmap));

			imageBitmap = newBitmap;
			pendingUrl = null;
		}
	}

	@Override
	public void itemLoaded(String url) {
		if (url.equals(pendingUrl)) {
			tryUpdateImage(imageView.getWidth(), imageView.getHeight());
		}
	}

	@Override
	public void itemDownloadProgress(String url, int current, int max) {

	}
}
