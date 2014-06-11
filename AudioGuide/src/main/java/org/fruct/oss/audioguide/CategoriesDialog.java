package org.fruct.oss.audioguide;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

import org.fruct.oss.audioguide.adapters.CategoryAdapter;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;

public class CategoriesDialog extends DialogFragment
		implements DialogInterface.OnClickListener, CategoryAdapter.Listener, DialogInterface.OnMultiChoiceClickListener {
	private TrackManager trackManager;
	private String[] labels;
	private boolean[] checked;
	private List<Category> categories;

	public static CategoriesDialog newInstance() {
		return new CategoriesDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();
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

		builder.setMultiChoiceItems(labels, checked, this);
		builder.setPositiveButton(android.R.string.ok, this);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int button) {

	}

	@Override
	public void categoryChecked(Category category, boolean isActive) {
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int index, boolean isActive) {
		trackManager.setCategoryState(categories.get(index), isActive);
	}
}
