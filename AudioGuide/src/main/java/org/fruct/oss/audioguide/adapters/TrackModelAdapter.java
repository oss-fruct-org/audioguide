package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.track.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class TrackModelAdapter extends BaseAdapter implements ModelListener, Closeable {
	private final static Logger log = LoggerFactory.getLogger(TrackModelAdapter.class);

	private final Context context;
	private final int resource;
	private final Model<Track> model;
	private Map<String, Integer> highlight = new HashMap<String, Integer>();

	private boolean closed = false;

	private RuntimeException stackTraceException;

	public TrackModelAdapter(Context context, int resource, Model<Track> model) {
		this.context = context;
		this.resource = resource;
		this.model = model;
		this.model.addListener(this);

		stackTraceException = new RuntimeException();
	}

	public void close() {
		this.model.removeListener(this);
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
	public Track getItem(int i) {
		return model.getItem(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TrackHolder holder;
		View view;

		if (convertView != null) {
			view = convertView;
			holder = (TrackHolder) view.getTag();
		} else {
			view = ((Activity) context).getLayoutInflater().inflate(resource, parent, false);
			assert view != null;

			holder = new TrackHolder();
			holder.text1 = (TextView) view.findViewById(android.R.id.text1);
			holder.text2 = (TextView) view.findViewById(android.R.id.text2);

			holder.localImage = (ImageView) view.findViewById(R.id.localImage);
			holder.localImage.setTag(holder);

			holder.activeImage = (ImageView) view.findViewById(R.id.activeImage);
			holder.activeImage.setTag(holder);

			view.setTag(holder);
		}

		Track track = (Track) getItem(position);
		holder.track = track;

		holder.text1.setText(track.getHumanReadableName());
		holder.text2.setText(track.getDescription());

		setupButton(holder.localImage, holder.track.isLocal());
		setupButton(holder.activeImage, holder.track.isActive());

		Integer highlightColor = highlight.get(track.getName());
		if (highlightColor != null) {
			view.setBackgroundColor(highlightColor);
		} else {
			view.setBackgroundColor(0xffffffff);
		}

		return view;
	}

	public void clearTrackHighlights() {
		highlight.clear();
	}

	public void addTrackHighlight(Track track, int color) {
		if (track == null) {
			return;
		}

		highlight.put(track.getName(), color);
	}

	private void setupButton(ImageView button, boolean isHighlighted) {
		if (isHighlighted)
			button.setColorFilter(0xff999911);
		else
			button.setColorFilter(0xff000000);
	}

	@Override
	public void dataSetChanged() {
		notifyDataSetChanged();
	}

	private static class TrackHolder {
		Track track;
		TextView text1;
		TextView text2;
		ImageView localImage;
		ImageView activeImage;
	}
}
