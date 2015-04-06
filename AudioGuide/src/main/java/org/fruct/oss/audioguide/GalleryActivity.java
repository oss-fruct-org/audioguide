package org.fruct.oss.audioguide;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;


public class GalleryActivity extends ActionBarActivity {
	public static final String EXTRA_URL = "org.fruct.oss.audioguide.GalleryActivity.EXTRA_URL";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gallery);

		final ImageView imageView = (ImageView) findViewById(R.id.image_view);
		final String url = getIntent().getStringExtra(EXTRA_URL);
		final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.root_layout);

		viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				viewGroup.getViewTreeObserver().removeGlobalOnLayoutListener(this);

				ImageLoader.getInstance().displayImage(url, imageView);
			}
		});


		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
