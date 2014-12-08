package org.fruct.oss.audioguide;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.BitmapSetter;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.ImageViewSetter;


public class GalleryActivity extends ActionBarActivity {
	public static final String EXTRA_URL = "org.fruct.oss.audioguide.GalleryActivity.EXTRA_URL";

	private BitmapProcessor bitmapProcessor;

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
				FileManager fileManager = FileManager.getInstance();
				bitmapProcessor = BitmapProcessor.requestBitmap(fileManager, url, FileSource.Variant.FULL,
						viewGroup.getMeasuredWidth(), viewGroup.getMeasuredHeight(), FileManager.ScaleMode.NO_SCALE,
						new ImageViewSetter(imageView));
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
		bitmapProcessor.recycle();
		super.onDestroy();
	}
}