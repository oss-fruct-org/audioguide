package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.FileManager;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.util.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointModelAdapter extends BaseAdapter implements Closeable, ModelListener, Downloader.Listener {
	private final static Logger log = LoggerFactory.getLogger(PointModelAdapter.class);

	private final TrackManager trackManager;
	private final FileManager fileManager;

	private final RuntimeException stackTraceException;

	private final Context context;
	private final int resource;
	private final Model<Point> model;
	private Set<Point> highlightedItems = new HashSet<Point>();
	private Set<String> pendingIconUris = new HashSet<String>();

	private boolean closed = false;

	public PointModelAdapter(Context context, int resource, Model<Point> model) {
		this.context = context;
		this.resource = resource;
		this.model = model;
		this.model.addListener(this);

		this.trackManager = TrackManager.getInstance();
		this.fileManager = FileManager.getInstance();

		this.fileManager.addWeakIconListener(this);

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
		}

		Point point = getItem(position);
		holder.point = point;

		holder.text1.setText(point.getName());
		holder.text2.setText(point.getDescription());
		holder.audioImage.setVisibility(point.hasAudio() ? View.VISIBLE : View.GONE);
		holder.icon.setImageDrawable(null);

		if (point.hasPhoto()) {
			String photoUrl = point.getPhotoUrl();
			if (pendingIconUris.contains(photoUrl)) {
				pendingIconUris.remove(photoUrl);
			}

			Bitmap iconBitmap = fileManager.getImageBitmap(photoUrl);
			if (iconBitmap != null) {
				holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), iconBitmap));
			} else {
				pendingIconUris.add(photoUrl);
				holder.icon.setImageDrawable(null);
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
	public void itemLoaded(final String uri) {
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (pendingIconUris.contains(uri))
					notifyDataSetChanged();

			}
		});
	}

	private static class PointHolder {
		PointHolder() {
			log.debug("PointHolder create");
		}

		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
		ImageView icon;
	}
}
