package org.fruct.oss.audioguide.fragments.edit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

public class EditTrackDialog extends DialogFragment implements DialogInterface.OnClickListener {
	private Listener listener;

	public interface Listener {
		void trackCreated(Track track);
		void trackUpdated(Track track);
	}

	private Track track;
	private boolean isNewTracks;

	private EditText editName;
	private EditText editDescription;
	private EditText editUrl;

	/**
	 * Show content of track and allow edit
	 * @param track Track to edit. May be null
	 */
	public EditTrackDialog(Track track) {
		if (track == null) {
			this.track = new Track();
			this.track.setPrivate(true);
			isNewTracks = true;
		} else {
			this.track = track;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_edit_track, null);
		assert view != null;

		editName = (EditText) view.findViewById(R.id.edit_name);
		editDescription = (EditText) view.findViewById(R.id.edit_description);
		editUrl = (EditText) view.findViewById(R.id.edit_url);

		if (track != null) {
			if (!Utils.isNullOrEmpty(track.getHname())) editName.setText(track.getHname());
			if (!Utils.isNullOrEmpty(track.getDescription())) editDescription.setText(track.getDescription());
			if (!Utils.isNullOrEmpty(track.getUrl())) editUrl.setText(track.getUrl());
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		String name = editName.getText().toString();
		String description = editDescription.getText().toString();
		String url = editUrl.getText().toString();

		// Assign name to track only when track is creating
		if (isNewTracks) {
			track.setName(createName(name));
		}

		if (name != null) {
			track.setHname(name);
		}

		if (description != null) {
			track.setDescription(description);
		}

		if (url != null) {
			track.setUrl(url);
		}

		// This track initially local, trackManager doesn't download points for it
		track.setLocal(true);

		if (listener != null) {
			if (isNewTracks)
				listener.trackCreated(track);
			else
				listener.trackUpdated(track);
		}
	}

	private String createName(String hname) {
		return "tr_" + hname.toLowerCase().replace(' ', '_');
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
