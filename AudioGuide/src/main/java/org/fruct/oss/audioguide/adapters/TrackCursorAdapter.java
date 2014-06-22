package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Track;

public class TrackCursorAdapter extends CursorAdapter {
	public TrackCursorAdapter(Context context, Cursor c) {
		super(context, c, false);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.list_track_item, viewGroup, false);
		assert view != null;

		TrackHolder holder = new TrackHolder();
		holder.text1 = (TextView) view.findViewById(android.R.id.text1);
		holder.text2 = (TextView) view.findViewById(android.R.id.text2);

		holder.publicImage = (ImageButton) view.findViewById(R.id.publicImage);
		holder.publicImage.setTag(holder);

		holder.localImage = (ImageButton) view.findViewById(R.id.localImage);
		holder.localImage.setTag(holder);

		holder.activeImage = (ImageButton) view.findViewById(R.id.activeImage);
		holder.activeImage.setTag(holder);

		//holder.localImage.setOnClickListener(this);
		//holder.activeImage.setOnClickListener(this);

		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Track track = new Track(cursor);
		TrackHolder holder = ((TrackHolder) view.getTag());

		holder.track = track;

		holder.text1.setText(track.getHumanReadableName());
		holder.text2.setText(track.getDescription());

		setupButton(holder.publicImage, !holder.track.isPrivate());
		setupButton(holder.localImage, holder.track.isLocal());
		setupButton(holder.activeImage, holder.track.isActive());

		/*Integer highlightColor = highlight.get(track.getName());
		if (highlightColor != null) {
			view.setBackgroundColor(highlightColor);
		} else {
			view.setBackgroundColor(0xffffffff);
		}*/
	}

	private void setupButton(ImageView button, boolean isHighlighted) {
		if (isHighlighted) {
			button.clearColorFilter();
		} else {
			button.setColorFilter(0x40ffffff, PorterDuff.Mode.DST_ATOP);
		}
	}

	public Track getTrack(int position) {
		Cursor cursor = (Cursor) getItem(position);
		return new Track(cursor);
	}

	private static class TrackHolder {
		Track track;

		TextView text1;
		TextView text2;
		ImageButton publicImage;
		ImageButton localImage;
		ImageButton activeImage;
	}
}
