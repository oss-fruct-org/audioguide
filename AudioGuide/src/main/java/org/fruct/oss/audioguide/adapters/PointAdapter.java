package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointAdapter extends ArrayAdapter<Point> {
	private final static Logger log = LoggerFactory.getLogger(PointAdapter.class);

	private final TrackManager trackManager;

	private Context context;
	private int resource;
	private List<Point> points;
	private Set<Point> highlightedItems = new HashSet<Point>();

	public PointAdapter(Context context, int resource, List<Point> points) {
		super(context, resource, points);

		this.context = context;
		this.resource = resource;
		this.points = points;
		this.trackManager = TrackManager.getInstance();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
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

		Point point = points.get(position);
		holder.point = point;

		holder.text1.setText(point.getName());
		holder.text2.setText(point.getDescription());
		holder.audioImage.setVisibility(point.hasAudio() ? View.VISIBLE : View.GONE);

		Bitmap iconBitmap = trackManager.getPointIconBitmap(point);
		if (iconBitmap != null) {
			holder.icon.setImageDrawable(new BitmapDrawable(getContext().getResources(), iconBitmap));
		} else {
			holder.icon.setImageDrawable(null);
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

	private static class PointHolder {
		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
		ImageView icon;
	}
}
