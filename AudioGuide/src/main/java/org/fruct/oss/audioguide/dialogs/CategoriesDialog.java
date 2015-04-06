package org.fruct.oss.audioguide.dialogs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.fruct.oss.audioguide.track.gets.Category;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.TrackManager;

import java.util.List;

public class CategoriesDialog extends DialogFragment
		implements DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
	public static interface Listener {
		void categorySelected(Category category);
	}

	private TrackManager trackManager;
	private String[] labels;
	private boolean[] checked;
	private List<Category> categories;
	private boolean isSingle;
	private Listener listener;

	public static CategoriesDialog newInstance() {
		Bundle args = new Bundle();
		args.putBoolean("isSingle", false);

		CategoriesDialog dialog = new CategoriesDialog();
		dialog.setArguments(args);
		return dialog;
	}

	public static CategoriesDialog newChoiceInstance() {
		Bundle args = new Bundle();
		args.putBoolean("isSingle", true);

		CategoriesDialog dialog = new CategoriesDialog();
		dialog.setArguments(args);
		return dialog;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			isSingle = getArguments().getBoolean("isSingle");
		}

		trackManager = DefaultTrackManager.getInstance();
		categories = trackManager.getCategories();

		labels = new String[categories.size()];
		checked = new boolean[categories.size()];

		for (int i = 0; i < categories.size(); i++) {
			Category category = categories.get(i);
			labels[i] = category.getDescription();
			checked[i] = category.isActive();
		}
	}

	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		if (!isSingle) {
			builder.setPositiveButton(android.R.string.ok, this);
			builder.setMultiChoiceItems(labels, checked, this);
		} else {
			builder.setItems(labels, this);
		}

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int button) {
		if (isSingle && listener != null) {
			listener.categorySelected(categories.get(button));
		}
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int index, boolean isActive) {
		trackManager.setCategoryState(categories.get(index), isActive);
	}
}
