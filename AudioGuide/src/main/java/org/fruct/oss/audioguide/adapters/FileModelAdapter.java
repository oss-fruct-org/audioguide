package org.fruct.oss.audioguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.models.ModelListener;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.files.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

public class FileModelAdapter extends BaseAdapter implements Closeable, ModelListener, FileListener {
	private final static Logger log = LoggerFactory.getLogger(FileModelAdapter.class);
	private final int resource;
	private final Context context;
	private final Model<FileContent> model;

	private final Set<String> pendingIconUris = new HashSet<String>();
	private final FileManager fileManager;

	public FileModelAdapter(Context context, int resource, Model<FileContent> files) {
		this.context = context;
		this.resource = resource;
		this.model = files;

		fileManager = FileManager.getInstance();
		fileManager.addWeakListener(this);

		model.addListener(this);
	}

	@Override
	public void close() {
		model.removeListener(this);
	}

	@Override
	public int getCount() {
		return model.getCount();
	}

	@Override
	public FileContent getItem(int i) {
		return model.getItem(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
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
		holder.image.setImageDrawable(null);

		if (file.isImage()) {
			String imageUrl = file.getUrl();
			if (pendingIconUris.contains(imageUrl)) {
				pendingIconUris.remove(imageUrl);
			}

			Bitmap iconBitmap = fileManager.getImageBitmap(imageUrl);
			if (iconBitmap != null) {
				holder.image.setImageDrawable(new BitmapDrawable(context.getResources(), iconBitmap));
			} else {
				pendingIconUris.add(imageUrl);
				holder.image.setImageDrawable(null);
			}
		}

		return view;
	}

	@Override
	public void itemLoaded(final String uri) {
		if (pendingIconUris.contains(uri))
			notifyDataSetChanged();
	}

	@Override
	public void itemDownloadProgress(String url, int current, int max) {

	}


	@Override
	public void dataSetChanged() {
		notifyDataSetChanged();
	}

	private static class FileHolder {
		TextView title;

		ImageView image;
	}
}
