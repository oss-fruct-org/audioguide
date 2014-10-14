package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.widget.CursorAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.ImageViewSetter;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointCursorAdapter extends CursorAdapter implements FileListener, View.OnClickListener, View.OnTouchListener {
	private final static Logger log = LoggerFactory.getLogger(PointCursorAdapter.class);

	private final FileManager fileManager;

	private final Context context;

	private boolean isPlaceSelectable;
	private Set<Point> highlightedItems = new HashSet<Point>();

	private HashMap<String, PointHolder> pendingAudioUrls = new HashMap<String, PointHolder>();

	private List<BitmapProcessor> bitmapProcessors = new ArrayList<BitmapProcessor>();

	private int selectedPosition = -1;

	public PointCursorAdapter(Context context, boolean isPlaceSelectable) {
		super(context, null, false);

		this.context = context;
		this.isPlaceSelectable = isPlaceSelectable;

		this.fileManager = FileManager.getInstance();
		this.fileManager.addListener(this);
	}

	public void close() {
		for (BitmapProcessor proc : bitmapProcessors) {
			proc.recycle();
		}
		this.fileManager.removeListener(this);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		log.debug("newView");

		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.list_point_item, viewGroup, false);
		assert view != null;

		PointHolder holder = new PointHolder();
		view.setTag(holder);

		holder.text1 = (TextView) view.findViewById(android.R.id.text1);
		holder.text2 = (TextView) view.findViewById(android.R.id.text2);
		holder.audioImage = (ImageView) view.findViewById(R.id.audio_image);
		holder.icon = (ImageView) view.findViewById(android.R.id.icon);
		holder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

		holder.positionBottom = (View) view.findViewById(R.id.position_bottom);
		holder.positionTop = (View) view.findViewById(R.id.position_top);
		holder.bitmapSetter = new ImageViewSetter(holder.icon);

		if (isPlaceSelectable) {
			view.setOnTouchListener(this);
			view.setFocusable(true);
		}

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Point point = new Point(cursor);
		log.debug("bindView " + point.getName());

		PointHolder holder = ((PointHolder) view.getTag());

		holder.point = point;

		holder.text1.setText(point.getName());
		holder.text2.setText(point.getDescription());
		holder.icon.setImageDrawable(null);
		holder.position = cursor.getPosition();

		if (point.hasPhoto()) {
			String photoUrl = point.getPhotoUrl();

			BitmapProcessor processor = BitmapProcessor.requestBitmap(fileManager, photoUrl, FileSource.Variant.FULL, Utils.getDP(48), Utils.getDP(48), FileManager.ScaleMode.SCALE_CROP, holder.bitmapSetter);
			bitmapProcessors.add(processor);
		} else {
			holder.bitmapSetter.setTag(null);
		}

		/*if (holder.pendingUrl != null) {
			log.debug("Removing url");
			pendingAudioUrls.remove(holder.pendingUrl);
			holder.pendingUrl = null;
		}*/

		if (point.hasAudio()) {
			String audioUrl = point.getAudioUrl();
			pendingAudioUrls.remove(audioUrl);

			FileManager.Storage storageType = fileManager.getStorageType(audioUrl, FileSource.Variant.FULL);
			if (storageType != null) {
				holder.progressBar.setVisibility(View.GONE);
				holder.audioImage.setVisibility(View.VISIBLE);

				if (storageType == FileManager.Storage.PERSISTENT) {
					holder.audioImage.setColorFilter(0xff3983CC, PorterDuff.Mode.SRC_ATOP);
				} else {
					holder.audioImage.clearColorFilter();
				}

				holder.pendingUrl = null;
			} else {
				holder.audioImage.setVisibility(View.GONE);
				holder.progressBar.setVisibility(View.VISIBLE);
				holder.progressBar.setProgress(0);
				holder.pendingUrl = audioUrl;
				pendingAudioUrls.put(audioUrl, holder);
			}
		} else {
			holder.progressBar.setVisibility(View.GONE);
			holder.audioImage.setVisibility(View.GONE);
			holder.pendingUrl = null;
		}

		if (selectedPosition == holder.position) {
			holder.positionTop.setVisibility(View.VISIBLE);
			holder.positionBottom.setVisibility(View.GONE);
		} else if (selectedPosition == holder.position + 1) {
			holder.positionBottom.setVisibility(View.VISIBLE);
			holder.positionTop.setVisibility(View.GONE);
		} else {
			holder.positionBottom.setVisibility(View.GONE);
			holder.positionTop.setVisibility(View.GONE);
		}

		if (highlightedItems.contains(point)) {
			view.setBackgroundColor(0xffffd700);
		} else {
			view.setBackgroundColor(0x00ffffff);
		}
	}

	public void setHighlightedItems(List<Point> pointsInRange) {
		this.highlightedItems = new HashSet<Point>(pointsInRange);
		notifyDataSetChanged();
	}

	public void addHighlightedItem(Point point) {
		highlightedItems.add(point);
		notifyDataSetChanged();
	}

	public void removeHighlightedItem(Point point) {
		highlightedItems.remove(point);
		notifyDataSetChanged();
	}

	@Override
	public void itemLoaded(final String url) {
		PointHolder holder = pendingAudioUrls.get(url);
		if (holder != null && url.equals(holder.pendingUrl)) {
			holder.progressBar.setVisibility(View.GONE);
			holder.audioImage.setVisibility(View.VISIBLE);
			pendingAudioUrls.remove(url);
		}

		notifyDataSetChanged();
	}

	@Override
	public void itemDownloadProgress(String url, int current, int max) {
		PointHolder holder = pendingAudioUrls.get(url);
		if (holder != null && url.equals(holder.pendingUrl)) {
			holder.progressBar.setMax(max);
			holder.progressBar.setProgress(current);
		}
	}

	@Override
	public void itemDownloadError(String fileUrl) {

	}

	public Point getPoint(int position) {
		Cursor cursor = (Cursor) getItem(position);
		return new Point(cursor);
	}

	@Override
	public void onClick(View view) {
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			PointHolder holder = (PointHolder) view.getTag();
			if (motionEvent.getY() < view.getMeasuredHeight() / 2) {
				selectedPosition = holder.position;
			} else {
				selectedPosition = holder.position + 1;
			}
			notifyDataSetChanged();
		}

		return false;
	}

	public int getSelectedPosition() {
		return selectedPosition;
	}

	private static class PointHolder {
		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
		ImageView icon;
		ProgressBar progressBar;

		View positionBottom;
		View positionTop;

		String pendingUrl;

		int position;
		ImageViewSetter bitmapSetter;
	}
}
