package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.gets.Category;

import java.util.List;

public class CategoryAdapter extends ArrayAdapter<Category> {
	private final Listener listener;

	public static interface Listener {
		void categoryChecked(Category category, boolean isActive);
	}

	public CategoryAdapter(Context context, int resource, List <Category> array, Listener listener) {
		super(context, resource, array);

		this.listener = listener;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		final CategoryHolder holder;

		if (convertView != null) {
			view = convertView;
			holder = ((CategoryHolder) view.getTag());
		} else {
			view = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.list_category_item, parent, false);
			holder = new CategoryHolder();
			holder.checkBox = ((CheckBox) view.findViewById(R.id.checkbox));
			holder.textView = ((TextView) view.findViewById(android.R.id.text1));

			holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
					listener.categoryChecked(holder.category, state);
				}
			});

			view.setTag(holder);
		}

		holder.category = getItem(position);
		holder.textView.setText(holder.category.getDescription());
		holder.checkBox.setChecked(holder.category.isActive());

		return view;
	}

	private static class CategoryHolder {
		CheckBox checkBox;
		TextView textView;

		Category category;
	}
}
