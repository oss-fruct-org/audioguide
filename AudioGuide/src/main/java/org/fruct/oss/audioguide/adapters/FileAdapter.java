package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.FilesContent;

import java.util.ArrayList;

public class FileAdapter extends ArrayAdapter<FileContent> {
	private final int resource;
	private final Context context;

	public FileAdapter(Context context, int resource) {
		super(context, resource);

		this.context = context;
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FileHolder holder;
		View view;

		if (convertView != null) {
			view = convertView;
			holder = ((FileHolder) convertView.getTag());
		} else {
			view = ((Activity) context).getLayoutInflater().inflate(resource, parent, false);
			assert view != null;

			holder = new FileHolder();
			view.setTag(holder);

			holder.title = ((TextView) view.findViewById(android.R.id.text1));
			holder.image = ((ImageView) view.findViewById(android.R.id.icon));
		}

		FileContent file = getItem(position);
		holder.title.setText(file.getTitle());

		return view;
	}

	public void setFilesContent(FilesContent filesContent) {
		clear();

		for (FileContent fileContent : filesContent.getFiles()) {
			add(fileContent);
		}

		notifyDataSetInvalidated();
	}

	private static class FileHolder {
		TextView title;
		ImageView image;
	}
}
