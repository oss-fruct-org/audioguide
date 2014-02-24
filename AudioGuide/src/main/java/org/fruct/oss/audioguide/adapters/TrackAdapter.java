package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<Track> implements View.OnClickListener {
	private final static Logger log = LoggerFactory.getLogger(TrackAdapter.class);

	private final Context context;
	private final int resource;
	private final List<Track> items;

	private Listener listener;

	public TrackAdapter(Context context, int resource, List<Track> items) {
		super(context, resource, items);

		this.context = context;
		this.resource = resource;
		this.items = items;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
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
			holder.track = items.get(position);
			holder.button1 = (ImageButton) view.findViewById(R.id.imageButton);
			holder.button1.setTag(holder.track);

			holder.button1.setOnClickListener(this);

			view.setTag(holder);
		}

		Track track = holder.track;
		holder.text1.setText(track.getName());
		holder.text2.setText(track.getDescription());

		if (track.isLocal())
			holder.button1.setColorFilter(0xff999911);
		else
			holder.button1.setColorFilter(0xff000000);

		holder.button1.setFocusable(false);
		holder.button1.setClickable(true);

		return view;
	}

	@Override
	public void onClick(View view) {
		if (view instanceof ImageButton) {
			ImageButton imageButton = (ImageButton) view;
			Track track = (Track) imageButton.getTag();
			if (listener != null)
				listener.buttonClicked(track);
		}
	}

	private static class TrackHolder {
		Track track;
		TextView text1;
		TextView text2;
		ImageButton button1;
	}

	public static interface Listener {
		void buttonClicked(Track track);
	}
}
