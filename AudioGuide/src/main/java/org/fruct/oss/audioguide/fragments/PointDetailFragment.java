package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pagercontainer.PagerContainer;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.NavigationDrawerFragment;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.ImageViewSetter;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;

public class PointDetailFragment extends Fragment {
    private static final String ARG_POINT = "point";
	private static final String STATE_POINT = "point";
	private static final String ARG_IS_OVERLAY = "c";
	private static final String STATE_IS_OVERLAY = "overlay";

	private Point point;
	private boolean isOverlay;

	private MultiPanel multiPanel;
	private FileManager fileManager;

	private ImageView imageView;
	private BitmapProcessor bitmapProcessor;

	//private Bitmap imageBitmap;

	private int imageSize;
	private boolean isStateSaved;
	private boolean isImageExpanded;

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
		isStateSaved = false;

		if (point.hasAudio())
			initializeBottomPanel();
	}

	private void initializeBottomPanel() {
		PanelFragment panelFragment = (PanelFragment) getFragmentManager().findFragmentByTag("bottom-panel-fragment");

		if (panelFragment == null) {
			panelFragment = PanelFragment.newInstance(point, -1, null);
			getFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.bottom_up, R.anim.bottom_down)
					.replace(R.id.panel_container,
					panelFragment, "bottom-panel-fragment").commit();
		}

		panelFragment.setFallbackPoint(point);
	}

	@Override
	public void onStop() {
		if (!isStateSaved) {
			PanelFragment panelFragment = (PanelFragment) getFragmentManager().findFragmentByTag("bottom-panel-fragment");
			if (panelFragment != null) {
				panelFragment.clearFallbackPoint();
			}
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

		fileManager = FileManager.getInstance();

		if (point.hasAudio()) {
			fileManager.requestDownload(point.getAudioUrl(), FileSource.Variant.FULL, FileManager.Storage.CACHE);
		}

		setHasOptionsMenu(true);
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
				imageSize = imageView.getMeasuredWidth();
				tryUpdateImage(imageSize, imageSize);
			}
		});

		setupGallery(view);

		if (isOverlay) {
			View innerLayout = view.findViewById(R.id.inner_layout);

			setupOverlayMode(innerLayout);

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getActivity().getSupportFragmentManager().popBackStack("details-fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
				}
			});

			int marginValue = Utils.getDP(24);
			((FrameLayout.LayoutParams) innerLayout.getLayoutParams())
					.setMargins(marginValue, marginValue, marginValue, marginValue);

			view.setClickable(true);
		}

		return view;
	}

	private void setupGallery(View view) {
		TestPagerAdapter adapter = new TestPagerAdapter();

		PagerContainer pagerContainer = ((PagerContainer) view.findViewById(R.id.pager_container));
		ViewPager pager = pagerContainer.getViewPager();

		pager.setClipChildren(false);
		pager.setOffscreenPageLimit(adapter.getCount());
		pager.setAdapter(adapter);
		pager.setPageMargin(15);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.point_details_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_show_on_map) {
			showOnMap();
		}

		return super.onOptionsItemSelected(item);
	}

	private void showOnMap() {
		NavigationDrawerFragment frag =
				(NavigationDrawerFragment)
						getActivity().getSupportFragmentManager()
								.findFragmentById(R.id.navigation_drawer);

		Bundle bundle = new Bundle();
		bundle.putParcelable(MapFragment.ARG_POINT, point);
		frag.selectItem(1, bundle);
	}

	private void setupOverlayMode(View view) {
		if (!isOverlay)
			return;

		Drawable background = getResources().getDrawable(R.drawable.marker_4);
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

		if (!isOverlay) {
			if (bitmapProcessor != null) {
				bitmapProcessor.recycle();
			}
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			if (imageView.getDrawable() instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
				if (bitmap != null && !bitmap.isRecycled())
					bitmap.recycle();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_POINT, point);
		outState.putBoolean(STATE_IS_OVERLAY, isOverlay);

		isStateSaved = true;
	}

	private void tryUpdateImage(int imageWidth, int imageHeight) {
		if (point.hasPhoto()) {
			String remoteUrl = point.getPhotoUrl();
			imageView.setAdjustViewBounds(true);

			bitmapProcessor = BitmapProcessor.requestBitmap(fileManager, remoteUrl, FileSource.Variant.FULL, imageWidth, imageHeight, FileManager.ScaleMode.NO_SCALE, new ImageViewSetter(imageView));
		} else {
			imageView.setVisibility(View.GONE);
		}
	}

	public Point getPoint() {
		return point;
	}

	private class TestPagerAdapter extends PagerAdapter {
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View view = getActivity().getLayoutInflater().inflate(R.layout.test_image_layout, container, false);
			container.addView(view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return 5;
		}

		@Override
		public boolean isViewFromObject(View view, Object o) {
			return (view == o);
		}
	}
}
