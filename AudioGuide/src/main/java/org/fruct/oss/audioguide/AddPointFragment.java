package org.fruct.oss.audioguide;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;

@Deprecated
public class AddPointFragment extends DialogFragment implements DialogInterface.OnClickListener {
	private final Track track;
	private EditText titleText;
	private EditText descriptionText;

	public AddPointFragment(Track track) {
		this.track = track;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_add_point, null);
		assert view != null;

		titleText = (EditText) view.findViewById(R.id.text_title);
		descriptionText = (EditText) view.findViewById(R.id.text_description);

		builder.setView(view)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);

		return builder.create();
	}


	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		Point point = new Point(titleText.getText().toString(),
				descriptionText.getText().toString(),
				"", "", 61., 34.);

		TrackManager.getInstance().sendPoint(track, point);
	}
}
