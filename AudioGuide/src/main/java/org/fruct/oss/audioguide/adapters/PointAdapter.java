package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Point;

import java.util.List;

public class PointAdapter extends ArrayAdapter<Point> {
	private Context context;
	private int resource;
	private List<Point> points;

	public PointAdapter(Context context, int resource, List<Point> points) {
		super(context, resource, points);

		this.context = context;
		this.resource = resource;
		this.points = points;
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
		}

		Point point = points.get(position);
		holder.point = point;

		holder.text1.setText(point.getName());
		holder.text2.setText(point.getDescription());
		holder.audioImage.setVisibility(point.hasAudio() ? View.VISIBLE : View.GONE);

		return view;
	}

	private static class PointHolder {
		Point point;
		TextView text1;
		TextView text2;

		ImageView audioImage;
	}
}
