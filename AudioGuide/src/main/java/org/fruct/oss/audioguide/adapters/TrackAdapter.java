package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<Track> {
	private final static Logger log = LoggerFactory.getLogger(TrackAdapter.class);

	private final Context context;
	private final int resource;
	private final List<Track> items;


	public TrackAdapter(Context context, int resource, List<Track> items) {
		super(context, resource, items);

		this.context = context;
		this.resource = resource;
		this.items = items;
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

		Track track = items.get(position);
		holder.track = track;

		holder.text1.setText(track.getHumanReadableHam());
		holder.text2.setText(track.getDescription());

		setupButton(holder.localImage, holder.track.isLocal());
		setupButton(holder.activeImage, holder.track.isActive());

		return view;
	}

	private void setupButton(ImageView button, boolean isHighlighted) {
		if (isHighlighted)
			button.setColorFilter(0xff999911);
		else
			button.setColorFilter(0xff000000);
	}

	private static class TrackHolder {
		Track track;
		TextView text1;
		TextView text2;
		ImageView localImage;
		ImageView activeImage;

	}
}
