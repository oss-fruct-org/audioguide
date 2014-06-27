package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.FileChooserActivity;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.FileModelAdapter;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileManagerFragment extends ListFragment implements UploadFragment.Listener {
	public static final String ARG_PICKER_MODE = "arg-picker-mode";

	public static final String RESULT_URL = "result-uri";
	public static final String RESULT_TITLE = "result-title";
	public static final String RESULT_MIME = "result-mime";

	private final static Logger log = LoggerFactory.getLogger(FileManagerFragment.class);

	private FileModelAdapter adapter;
	//private FileManager fileManager;

	private boolean pickerMode;

    public static FileManagerFragment newInstance(boolean pickerMode) {
        FileManagerFragment fragment = new FileManagerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PICKER_MODE, pickerMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileManagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
			pickerMode = getArguments().getBoolean(ARG_PICKER_MODE, false);
        }

		//fileManager = FileManager.getInstance();

		//adapter = new FileModelAdapter(getActivity(), R.layout.list_file_item, fileManager.getImagesModel());
		setListAdapter(adapter);

		setHasOptionsMenu(true);
    }

	@Override
	public void onDestroy() {
		adapter.close();

		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.file_manager_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			addFile();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		if (pickerMode) {
			Intent intent = new Intent();
			FileContent item = adapter.getItem(position);

			intent.setData(Uri.parse(item.getUrl()));
			intent.putExtra(RESULT_MIME, item.getMimeType());
			intent.putExtra(RESULT_TITLE, item.getTitle());
			intent.putExtra(RESULT_URL, item.getUrl());

			if (getActivity().getParent() == null)
				getActivity().setResult(Activity.RESULT_OK, intent);
			else
				getActivity().getParent().setResult(Activity.RESULT_OK, intent);

			getActivity().finish();
		}
	}

	private void addFile() {
		UploadFragment fragment = new UploadFragment("*/*");
		fragment.setListener(this);
		fragment.show(getFragmentManager(), "upload-fragment");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof FileChooserActivity) {
			pickerMode = true;
		}
	}

	@Override
	public void fileCreated(FileContent file) {
		//fileManager.addFile(file);
	}
}
