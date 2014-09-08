package org.fruct.oss.audioguide.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import org.fruct.oss.audioguide.adapters.TrackCursorAdapter;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.CursorReceiver;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

public class SelectTrackDialog extends DialogFragment implements DialogInterface.OnClickListener, CursorReceiver {
	private Listener listener;

	public static interface Listener {
		void trackSelected(Track track);
	}

	private TrackManager trackManager;
	private TrackCursorAdapter adapter;
	private CursorHolder cursorHolder;

	public static SelectTrackDialog newInstance() {
		return new SelectTrackDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.trackManager = DefaultTrackManager.getInstance();
		cursorHolder = trackManager.loadLocalTracks();
		adapter = new TrackCursorAdapter(getActivity());
		cursorHolder.attachToReceiver(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		cursorHolder.close();
		adapter = null;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setAdapter(adapter, this);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		Track selectedTrack = adapter.getTrack(i);

		if (listener != null) {
			listener.trackSelected(selectedTrack);
		}
	}

	@Override
	public Cursor swapCursor(Cursor cursor) {
		if (cursor.getCount() == 0) {
			getFragmentManager().popBackStack("select-track-dialog", FragmentManager.POP_BACK_STACK_INCLUSIVE);
			Toast.makeText(getActivity(), "No tracks to activate", Toast.LENGTH_SHORT).show();
		}

		if (adapter != null) {
			return adapter.swapCursor(cursor);
		} else {
			return null;
		}
	}
}
