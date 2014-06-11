package org.fruct.oss.audioguide.fragments.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.fruct.oss.audioguide.CategoriesDialog;
import org.fruct.oss.audioguide.FileChooserActivity;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.fragments.FileManagerFragment;
import org.fruct.oss.audioguide.fragments.UploadFragment;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPointDialog extends DialogFragment implements DialogInterface.OnClickListener, CategoriesDialog.Listener {
	private final static Logger log = LoggerFactory.getLogger(EditPointDialog.class);

	private static final int REQUEST_CODE_IMAGE = 1;
	private static final int REQUEST_CODE_AUDIO = 0;

	private Listener listener;

	public interface Listener {
		void pointCreated(Point point);
		void pointUpdated(Point point);
	}

	private Point point;
	private boolean isNewPoint;

	private EditText editName;
	private EditText editDescription;
	private EditText editUrl;
	private Category selectedCategory;

	private TextView imageFileLabel;
	private Button imageFileButton;

	private TextView audioFileLabel;
	private Button audioFileButton;

	private TextView categoryLabel;
	private Button categoryButton;

	public EditPointDialog() {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable("point", point);
		outState.putBoolean("isNewPoint", isNewPoint);
	}

	public static EditPointDialog newInstance(Point point) {
		Bundle args = new Bundle();
		args.putParcelable("point", point);

		EditPointDialog fragment = new EditPointDialog();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		point = getArguments().getParcelable("point");
		if (point == null) {
			point = new Point();
			isNewPoint = true;
		}

		if (savedInstanceState != null) {
			point = savedInstanceState.getParcelable("point");
			isNewPoint = savedInstanceState.getBoolean("isNewPoint");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), AUtils.getDialogTheme());
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.dialog_add_point, null);
		assert view != null;

		editName = (EditText) view.findViewById(R.id.text_title);
		editDescription = (EditText) view.findViewById(R.id.text_description);

		imageFileLabel = (TextView) view.findViewById(R.id.image_file_title);
		imageFileButton = (Button) view.findViewById(R.id.image_file_button);

		audioFileLabel = (TextView) view.findViewById(R.id.audio_file_title);
		audioFileButton = (Button) view.findViewById(R.id.audio_file_button);

		categoryLabel = (TextView) view.findViewById(R.id.category_title);
		categoryButton = (Button) view.findViewById(R.id.category_button);

		if (point != null) {
			if (!Utils.isNullOrEmpty(point.getName())) editName.setText(point.getName());
			if (!Utils.isNullOrEmpty(point.getDescription()))
				editDescription.setText(point.getDescription());
			//if (!Utils.isNullOrEmpty(point.point())) editUrl.setText(point.getUrl());
		}

		imageFileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showFileChooserDialog("image/*", REQUEST_CODE_IMAGE);
			}
		});
		audioFileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showFileChooserDialog("audio/*", REQUEST_CODE_AUDIO);
			}
		});

		categoryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CategoriesDialog categoriesDialog = CategoriesDialog.newChoiceInstance();
				categoriesDialog.setListener(EditPointDialog.this);
				categoriesDialog.show(getFragmentManager(), "categories-dialog");
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(AUtils.getDialogContext(getActivity()));
		builder.setView(view)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		String name = editName.getText().toString();
		String description = editDescription.getText().toString();
		//String url = editUrl.getText().toString();

		if (name != null) {
			point.setName(name);
		}

		if (description != null) {
			point.setDescription(description);
		}

		if (listener != null) {
			if (isNewPoint)
				listener.pointCreated(point);
			else
				listener.pointUpdated(point);
		}
	}

	private void showFileChooserDialog(String mimeType, int requestCode) {
		Intent intent = new Intent(getActivity(), FileChooserActivity.class);
		intent.setType(mimeType);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			assert uri != null;
			log.info("Image file {} chosen", uri.toString());

			String title = data.getStringExtra(FileManagerFragment.RESULT_TITLE);
			point.setPhotoUrl(data.getStringExtra(FileManagerFragment.RESULT_URL));

			imageFileLabel.setText(title);
		} else if (requestCode == REQUEST_CODE_AUDIO && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			assert uri != null;
			log.info("Audio file {} chosen", uri.toString());

			String title = data.getStringExtra(FileManagerFragment.RESULT_TITLE);
			point.setAudioUrl(data.getStringExtra(FileManagerFragment.RESULT_URL));

			audioFileLabel.setText(title);
		}
	}

	@Override
	public void categorySelected(Category category) {
		categoryLabel.setText(category.getDescription());
		selectedCategory = category;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
