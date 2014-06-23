package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointCursorAdapter extends CursorAdapter implements FileListener {
	private final static Logger log = LoggerFactory.getLogger(PointCursorAdapter.class);

	private final FileManager fileManager;

	private final Context context;
	private Set<Point> highlightedItems = new HashSet<Point>();

	private Set<String> pendingIconUrls = new HashSet<String>();
	private HashMap<String, PointHolder> pendingAudioUrls = new HashMap<String, PointHolder>();


	public PointCursorAdapter(Context context) {
		super(context, null, false);

		this.context = context;

		this.fileManager = FileManager.getInstance();
		this.fileManager.addWeakListener(this);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.list_point_item, viewGroup, false);
		assert view != null;

		PointHolder holder = new PointHolder();
		view.setTag(holder);

		holder.text1 = (TextView) view.findViewById(android.R.id.text1);
		holder.text2 = (TextView) view.findViewById(android.R.id.text2);
		holder.audioImage = (ImageView) view.findViewById(R.id.audioImage);
		holder.icon = (ImageView) view.findViewById(android.R.id.icon);
		holder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Point point = new Point(cursor);
		PointHolder holder = ((PointHolder) view.getTag());

		holder.point = point;

		holder.text1.setText(point.getName());
		holder.text2.setText(point.getDescription());
		holder.audioImage.setVisibility(point.hasAudio() ? View.VISIBLE : View.GONE);
		holder.icon.setImageDrawable(null);

		if (point.hasPhoto()) {
			String photoUrl = point.getPhotoUrl();
			if (pendingIconUrls.contains(photoUrl)) {
				pendingIconUrls.remove(photoUrl);
			}

			Bitmap iconBitmap = fileManager.getImageBitmap(photoUrl, Utils.getDP(48), Utils.getDP(48), FileManager.ScaleMode.SCALE_FIT);
			if (iconBitmap != null) {
				holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), iconBitmap));
			} else {
				pendingIconUrls.add(photoUrl);
				holder.icon.setImageDrawable(null);
			}
		}

		if (point.hasAudio()) {
			String audioUrl = point.getAudioUrl();
			if (pendingAudioUrls.containsKey(audioUrl)) {
				pendingAudioUrls.remove(audioUrl);
			}

			if (fileManager.isFileLocal(audioUrl)) {
				holder.progressBar.setVisibility(View.GONE);
			} else {
				holder.progressBar.setVisibility(View.VISIBLE);
				holder.progressBar.setProgress(0);
				pendingAudioUrls.put(audioUrl, holder);
				fileManager.insertAudioUri(audioUrl);
			}
		}

		if (highlightedItems.contains(point)) {
			view.setBackgroundColor(0xffffd700);
		} else {
			view.setBackgroundColor(0xffffffff);
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
		if (pendingIconUrls.contains(url))
			notifyDataSetChanged();

		PointHolder holder = pendingAudioUrls.get(url);
		if (holder != null) {
			holder.progressBar.setVisibility(View.GONE);
			pendingAudioUrls.remove(url);
		}
	}

	@Override
	public void itemDownloadProgress(String url, int current, int max) {
		PointHolder holder = pendingAudioUrls.get(url);

		if (holder != null) {
			holder.progressBar.setMax(max);
			holder.progressBar.setProgress(current);
		}
	}

	public Point getPoint(int position) {
		Cursor cursor = (Cursor) getItem(position);
		return new Point(cursor);
	}

	private static class PointHolder {
		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
		ImageView icon;
		ProgressBar progressBar;
	}
}