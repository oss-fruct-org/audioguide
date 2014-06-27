package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

public class TrackCursorAdapter extends CursorAdapter implements View.OnClickListener {
	public TrackCursorAdapter(Context context) {
		super(context, null, false);
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

		holder.localImage.setOnClickListener(this);
		holder.activeImage.setOnClickListener(this);

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

	@Override
	public void onClick(View view) {
		TrackManager trackManager = DefaultTrackManager.getInstance();
		TrackHolder holder = (TrackHolder) view.getTag();
		if (holder != null) {
			Track track = holder.track;
			if (holder.activeImage == view) {
				throw new UnsupportedOperationException();
			} else if (holder.localImage == view && !track.isLocal()) {
				trackManager.storeTrackLocal(holder.track);
			}
		}
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
