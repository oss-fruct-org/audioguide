package org.fruct.oss.audioguide.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.CheckBox;

public class WarnDialog extends DialogFragment implements DialogInterface.OnClickListener {
	private CheckBox checkbox;

	private int messageId;
	private int configureId;
	private int disableId;
	private String disablePref;
	private PendingIntent configureIntent;

	public WarnDialog() {
	}

	public static WarnDialog newInstance(int messageId, int configureId, int disableId,
										 String disablePref, PendingIntent configureIntent) {
		Bundle args = new Bundle(4);
		args.putInt("messageId", messageId);
		args.putInt("configureId", configureId);
		args.putInt("disableId", disableId);
		args.putString("disablePref", disablePref);
		args.putParcelable("configureIntent", configureIntent);

		WarnDialog warnDialog = new WarnDialog();
		warnDialog.setArguments(args);

		return warnDialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();

		this.messageId = args.getInt("messageId");
		this.configureId = args.getInt("configureId");
		this.disableId = args.getInt("disableId");
		this.disablePref = args.getString("disablePref");
		this.configureIntent = args.getParcelable("configureIntent");
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		//AlertDialog.Builder builder = new AlertDialog.Builder(
		//		new ContextThemeWrapper(getActivity(), Utils.getDialogTheme()));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setMessage(messageId);
		builder.setPositiveButton(configureId, this);
		builder.setNegativeButton(android.R.string.cancel, this);

		checkbox = new CheckBox(getActivity());
		checkbox.setText(disableId);
		builder.setView(checkbox);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int which) {
		if (checkbox.isChecked()) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			pref.edit().putBoolean(disablePref, true).apply();
		}

		if (which == AlertDialog.BUTTON_POSITIVE) {
			try {
				configureIntent.send();
			} catch (PendingIntent.CanceledException ignore) {
			}
		}
	}

	protected void onAccept() {
	}
}
