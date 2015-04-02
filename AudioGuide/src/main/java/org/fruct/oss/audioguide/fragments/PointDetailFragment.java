package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.pagercontainer.PagerContainer;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.GalleryActivity;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.NavigationDrawerFragment;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.ImageViewSetter;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class PointDetailFragment extends Fragment {
    private static final String ARG_POINT = "point";
	private static final String STATE_POINT = "point";
	private static final String ARG_IS_OVERLAY = "overlay";
	private static final String STATE_IS_OVERLAY = "overlay";

	private Point point;
	private boolean isOverlay;

	private boolean isStateSaved;

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

		if (point.hasAudio()) {
			//fileManager.requestDownload(point.getAudioUrl(), FileSource.Variant.FULL, FileManager.Storage.CACHE);
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

		setupGallery2(view);

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
			FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) innerLayout.getLayoutParams();
			layoutParams.setMargins(marginValue, marginValue, marginValue, marginValue);
			layoutParams.gravity = Gravity.CENTER;

			view.setClickable(true);
		}

		return view;
	}

	private void setupGallery2(View view) {
		HorizontalScrollView galleryScroll = (HorizontalScrollView) view.findViewById(R.id.gallery_scroll);

		LinearLayout galleryLayout = (LinearLayout) view.findViewById(R.id.gallery);
		TrackManager trackManager = DefaultTrackManager.getInstance();

		Context context = getActivity();

		List<String> photos = trackManager.getPointPhotos(point);
		if (photos.isEmpty()) {
			galleryScroll.setVisibility(View.GONE);
			return;
		}

		final int MARGIN_SIZE = Utils.getDP(8);
		final int IMAGE_SIZE = Utils.getDP(80);

		for (final String photo : photos) {
			ImageView photoImage = new ImageView(context);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, IMAGE_SIZE );
			params.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE);

			photoImage.setLayoutParams(params);
			photoImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			ImageLoader.getInstance().displayImage(photo, photoImage);

			galleryLayout.addView(photoImage);

			photoImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					galleryImageClicked(photo);
				}
			});
		}

		galleryScroll.requestLayout();
	}

	private void galleryImageClicked(String photo) {
		Intent intent = new Intent(getActivity(), GalleryActivity.class);
		intent.putExtra(GalleryActivity.EXTRA_URL, photo);
		getActivity().startActivity(intent);
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

    @Override
    public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE_POINT, point);
		outState.putBoolean(STATE_IS_OVERLAY, isOverlay);

		isStateSaved = true;
	}


	public Point getPoint() {
		return point;
	}
}
