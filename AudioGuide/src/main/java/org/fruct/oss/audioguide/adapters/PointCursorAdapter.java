package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.events.AudioDownloadFinished;
import org.fruct.oss.audioguide.events.AudioDownloadProgress;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.EventReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class PointCursorAdapter extends CursorAdapter implements View.OnClickListener {
	private final static Logger log = LoggerFactory.getLogger(PointCursorAdapter.class);

	private final DiskCache cache;

	private boolean isPlaceSelectable;
	private Set<Point> highlightedItems = new HashSet<>();

	private HashMap<String, PointHolder> pendingAudioUrls = new HashMap<>();

	public PointCursorAdapter(Context context, boolean isPlaceSelectable) {
		super(context, null, false);

		this.isPlaceSelectable = isPlaceSelectable;
		this.cache = ImageLoader.getInstance().getDiskCache();
		EventBus.getDefault().register(this);
	}

	public void close() {
		EventBus.getDefault().unregister(this);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		log.debug("newView");

		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.list_point_item, viewGroup, false);
		assert view != null;

		PointHolder holder = new PointHolder();
		view.setTag(holder);

		holder.text1 = (TextView) view.findViewById(android.R.id.text1);
		holder.audioImage = (ImageView) view.findViewById(R.id.audio_image);
		holder.icon = (ImageView) view.findViewById(android.R.id.icon);
		holder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Point point = new Point(cursor);
		log.debug("bindView " + point.getName());

		PointHolder holder = ((PointHolder) view.getTag());

		holder.point = point;

		holder.text1.setText(point.getName());
		holder.position = cursor.getPosition();

		if (point.hasPhoto()) {
			String photoUrl = point.getPhotoUrl();
			ImageLoader.getInstance().displayImage(photoUrl, holder.icon);
		} else {
			holder.icon.setImageDrawable(null);
		}

		/*if (holder.pendingUrl != null) {
			log.debug("Removing url");
			pendingAudioUrls.remove(holder.pendingUrl);
			holder.pendingUrl = null;
		}*/


		if (point.hasAudio()) {
			String audioUrl = point.getAudioUrl();
			pendingAudioUrls.remove(audioUrl);

			File existingFile = cache.get(audioUrl);

			if (existingFile != null && existingFile.exists()) {
				holder.progressBar.setVisibility(View.GONE);
				holder.audioImage.setVisibility(View.VISIBLE);
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

		if (highlightedItems.contains(point)) {
			view.setBackgroundColor(0xffffd700);
		} else {
			view.setBackgroundColor(0x00ffffff);
		}
	}

	public void setHighlightedItems(List<Point> pointsInRange) {
		this.highlightedItems = new HashSet<>(pointsInRange);
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

	@EventReceiver
	public void onEventMainThread(AudioDownloadProgress event) {
		String url = event.getUrl();
		PointHolder holder = pendingAudioUrls.get(url);
		if (holder != null && url.equals(holder.pendingUrl)) {
			holder.progressBar.setMax(event.getTotal());
			holder.progressBar.setProgress(event.getCurrent());
		}
	}

	@EventReceiver
	public void onEventMainThread(AudioDownloadFinished event) {
		String url = event.getUrl();
		PointHolder holder = pendingAudioUrls.get(url);
		if (holder != null && url.equals(holder.pendingUrl)) {
			holder.progressBar.setVisibility(View.GONE);
			holder.audioImage.setVisibility(View.VISIBLE);
			pendingAudioUrls.remove(url);
		}

		notifyDataSetChanged();
	}

	public Point getPoint(int position) {
		Cursor cursor = (Cursor) getItem(position);
		return new Point(cursor);
	}

	@Override
	public void onClick(View view) {
	}

	/*
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
*/

	private static class PointHolder {
		Point point;
		TextView text1;

		ImageView audioImage;
		ImageView icon;
		ProgressBar progressBar;

		String pendingUrl;

		//View positionBottom;
		//View positionTop;

		int position;
	}
}
