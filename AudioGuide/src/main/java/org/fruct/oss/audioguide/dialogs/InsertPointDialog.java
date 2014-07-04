package org.fruct.oss.audioguide.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.widget.ListView;

import org.fruct.oss.audioguide.adapters.PointCursorAdapter;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

public class InsertPointDialog extends DialogFragment implements DialogInterface.OnClickListener, CursorHolder.Listener {
	private Track track;

	private PointCursorAdapter adapter;
	private TrackManager trackManager;
	private CursorHolder cursorHolder;
	private Point point;

	public static InsertPointDialog newInstance(Track track, Point point) {
		Bundle args = new Bundle();
		args.putParcelable("track", track);
		args.putParcelable("point", point);

		InsertPointDialog dialog = new InsertPointDialog();
		dialog.setArguments(args);
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = DefaultTrackManager.getInstance();
		track = getArguments().getParcelable("track");
		point = getArguments().getParcelable("point");
		adapter = new PointCursorAdapter(getActivity(), true);
		cursorHolder = trackManager.loadPoints(track);
		cursorHolder.setListener(this);
		cursorHolder.attachToAdapter(adapter);
	}

	@Override
	public void onDestroy() {
		cursorHolder.close();
		super.onDestroy();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListView listView = new ListView(getActivity());
		listView.setAdapter(adapter);

		builder.setView(listView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				trackManager.insertToTrack(track, point, adapter.getSelectedPosition());
			}
		});

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
	}

	@Override
	public void onReady(Cursor cursor) {
		if (cursor.getCount() == 0) {
			trackManager.insertToTrack(track, point, 0);
			dismiss();
		}
	}
}
