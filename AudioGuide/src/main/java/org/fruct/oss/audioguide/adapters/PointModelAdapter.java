package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointModelAdapter extends BaseAdapter implements Closeable, ModelListener, FileListener {
	private final static Logger log = LoggerFactory.getLogger(PointModelAdapter.class);

	private final FileManager fileManager;

	private final RuntimeException stackTraceException;

	private final Context context;
	private final int resource;
	private final Model<Point> model;
	private Set<Point> highlightedItems = new HashSet<Point>();

	private Set<String> pendingIconUrls = new HashSet<String>();
	private HashMap<String, PointHolder> pendingAudioUrls = new HashMap<String, PointHolder>();

	private boolean closed = false;

	public PointModelAdapter(Context context, int resource, Model<Point> model) {
		this.context = context;
		this.resource = resource;
		this.model = model;
		this.model.addListener(this);

		this.fileManager = FileManager.getInstance();
		this.fileManager.addWeakListener(this);

		stackTraceException = new RuntimeException();
	}

	@Override
	public void close() {
		model.removeListener(this);

		closed = true;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		if (!closed) {
			close();
			log.warn("ModelAdapter should be explicitly closed!", stackTraceException);
		}
	}

	@Override
	public int getCount() {
		return model.getCount();
	}

	@Override
	public Point getItem(int i) {
		return model.getItem(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		PointHolder holder;
		View view;

		if (convertView != null) {
			view = convertView;
			holder = (PointHolder) view.getTag();
		} else {
			view = ((Activity) context).getLayoutInflater().inflate(resource, parent, false);
			assert view != null;

			holder = new PointHolder();
			view.setTag(holder);

			holder.text1 = (TextView) view.findViewById(android.R.id.text1);
			holder.text2 = (TextView) view.findViewById(android.R.id.text2);
			holder.audioImage = (ImageView) view.findViewById(R.id.audioImage);
			holder.icon = (ImageView) view.findViewById(android.R.id.icon);
			holder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		}

		Point point = getItem(position);
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

		return view;
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
	public void dataSetChanged() {
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

	private static class PointHolder {
		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
		ImageView icon;
		ProgressBar progressBar;
	}
}
