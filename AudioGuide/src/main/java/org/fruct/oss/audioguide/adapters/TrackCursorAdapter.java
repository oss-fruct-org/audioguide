package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.BitmapProcessor;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.ImageViewSetter;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.tasks.StoreTrackTask;
import org.fruct.oss.audioguide.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class TrackCursorAdapter extends CursorAdapter implements View.OnClickListener, FileListener {
	private StoreTrackTask storeTrackTask;

	public TrackCursorAdapter(Context context) {
		super(context, null, false);
	}

	public void close() {
		if (storeTrackTask != null) {
			storeTrackTask.cancel(true);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View view = null;
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		try {
			view = inflater.inflate(R.layout.list_track_item, viewGroup, false);
		} catch (InflateException ex) {
			Log.e("ERRR", ex.getCause().getCause().getCause().toString());
		}
		assert view != null;

		TrackHolder holder = new TrackHolder();
		holder.text1 = (TextView) view.findViewById(android.R.id.text1);
		holder.text2 = (TextView) view.findViewById(android.R.id.text2);

		holder.publicImage = (ImageButton) view.findViewById(R.id.publicImage);
		holder.publicImage.setTag(holder);

		holder.localImage = (ImageButton) view.findViewById(R.id.localImage);
		holder.localImage.setTag(holder);

		holder.icon = (ImageView) view.findViewById(android.R.id.icon);

		//holder.activeImage = (ImageButton) view.findViewById(R.id.activeImage);
		//holder.activeImage.setTag(holder);

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

		if (track.hasPhoto()) {
			String photoUrl = track.getPhotoUrl();
			ImageLoader.getInstance().displayImage(photoUrl, holder.icon);
		} else {
			holder.icon.setImageDrawable(null);
		}

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
		TrackHolder holder = (TrackHolder) view.getTag();
		if (holder != null) {
			Track track = holder.track;
			if (holder.localImage == view && !track.isLocal()) {
				storeTrackTask = new StoreTrackTask(holder.track);
				storeTrackTask.execute();
			}
		}
	}

	@Override
	public void itemLoaded(String fileUrl) {

	}

	@Override
	public void itemDownloadProgress(String fileUrl, int current, int max) {

	}

	@Override
	public void itemDownloadError(String fileUrl) {

	}

	private static class TrackHolder {
		Track track;

		ImageView icon;

		TextView text1;
		TextView text2;
		ImageButton publicImage;
		ImageButton localImage;
		//ImageButton activeImage;
	}
}
