package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
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

			holder.downloadButton = (ImageButton) view.findViewById(R.id.downloadButton);
			holder.downloadButton.setTag(holder);
			holder.downloadButton.setOnClickListener(this);

			holder.activateButton = (ImageButton) view.findViewById(R.id.activateButton);
			holder.activateButton.setTag(holder);
			holder.activateButton.setOnClickListener(this);

			view.setTag(holder);
		}

		Track track = items.get(position);
		holder.track = track;

		holder.text1.setText(track.getName());
		holder.text2.setText(track.getDescription());

		setupButton(holder.downloadButton, holder.track.isLocal());
		setupButton(holder.activateButton, holder.track.isActive());

		return view;
	}

	private void setupButton(ImageButton button, boolean isHighlighted) {
		if (isHighlighted)
			button.setColorFilter(0xff999911);
		else
			button.setColorFilter(0xff000000);

		button.setFocusable(false);
		button.setClickable(true);

	}

	@Override
	public void onClick(View view) {
		TrackHolder holder = (TrackHolder) view.getTag();

		if (view == holder.downloadButton) {
			if (listener != null)
				listener.downloadButtonClicked(holder.track);
		} else if (view == holder.activateButton) {
			if (listener != null)
				listener.activateButtonClicked(holder.track);
		}
	}

	private static class TrackHolder {
		Track track;
		TextView text1;
		TextView text2;
		ImageButton downloadButton;
		ImageButton activateButton;

	}

	public static interface Listener {
		void downloadButtonClicked(Track track);
		void activateButtonClicked(Track track);
	}
}
