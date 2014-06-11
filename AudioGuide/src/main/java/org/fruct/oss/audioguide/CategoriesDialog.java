package org.fruct.oss.audioguide;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;

import org.fruct.oss.audioguide.adapters.CategoryAdapter;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.TrackManager;

public class CategoriesDialog extends DialogFragment implements DialogInterface.OnClickListener, CategoryAdapter.Listener {
	private TrackManager trackManager;

	public static CategoriesDialog newInstance() {
		return new CategoriesDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();
	}


	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setAdapter(new CategoryAdapter(getActivity(), R.layout.list_category_item,
						trackManager.getCategories(), this), this)
				.setPositiveButton(android.R.string.ok, this);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int button) {

	}

	@Override
	public void categoryChecked(Category category, boolean isActive) {
		trackManager.setCategoryState(category, isActive);
	}
}
