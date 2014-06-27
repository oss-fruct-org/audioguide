package org.fruct.oss.audioguide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.files.DefaultFileManager;
import org.fruct.oss.audioguide.files.FileManager;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class EditPointDialog extends DialogFragment implements DialogInterface.OnClickListener, CategoriesDialog.Listener {
	private final static Logger log = LoggerFactory.getLogger(EditPointDialog.class);

	private static final int REQUEST_CODE_IMAGE = 1;
	private static final int REQUEST_CODE_AUDIO = 0;

	private Listener listener;
	private List<Category> categories;

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

	private TextView audioFileLabel;
	private Button audioFileButton;

	private ImageButton photoButton;

	private Spinner categorySpinner;

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

		TrackManager trackManager = DefaultTrackManager.getInstance();
		categories = trackManager.getCategories();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), AUtils.getDialogTheme());
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.dialog_add_point, null);
		assert view != null;

		editName = (EditText) view.findViewById(R.id.text_title);
		editDescription = (EditText) view.findViewById(R.id.text_description);

		photoButton = (ImageButton) view.findViewById(R.id.photo_button);

		audioFileLabel = (TextView) view.findViewById(R.id.audio_file_title);
		audioFileButton = (Button) view.findViewById(R.id.audio_file_button);

		categorySpinner = (Spinner) view.findViewById(R.id.category_spinner);
		setupCategorySpinner(categorySpinner);

		if (point != null) {
			if (!Utils.isNullOrEmpty(point.getName())) editName.setText(point.getName());
			if (!Utils.isNullOrEmpty(point.getDescription()))
				editDescription.setText(point.getDescription());
			//if (!Utils.isNullOrEmpty(point.point())) editUrl.setText(point.getUrl());
		}

		photoButton.setOnClickListener(new View.OnClickListener() {
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



		/*categoryField.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CategoriesDialog categoriesDialog = CategoriesDialog.newChoiceInstance();
				categoriesDialog.setListener(EditPointDialog.this);
				categoriesDialog.show(getFragmentManager(), "categories-dialog");
			}
		});*/

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
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(mimeType);
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		startActivityForResult(Intent.createChooser(intent, "Choose file"), requestCode);
	}

	private void setupCategorySpinner(Spinner spinner) {
		List<String> categoryNames = Utils.map(categories, new Utils.Function<String, Category>() {
			@Override
			public String apply(Category category) {
				return category.getDescription();
			}
		});

		ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, categoryNames);
		categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(categoriesAdapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				point.setCategoryId(categories.get(i).getId());
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {
			}
		});

		if (!categories.isEmpty())
			point.setCategoryId(categories.get(0).getId());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		FileManager fm = DefaultFileManager.getInstance();
		ContentResolver resolver = getActivity().getContentResolver();

		if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Uri imageUri = fm.insertLocalFile(uri.getLastPathSegment(), uri);
			point.setPhotoUrl(imageUri.toString());

			try {
				InputStream photoStream = resolver.openInputStream(uri);
				Drawable drawable = Drawable.createFromStream(photoStream, "Photo");
				photoStream.close();
				photoButton.setImageDrawable(drawable);
			} catch (IOException ignore) {
			}
		}
		if (requestCode == REQUEST_CODE_AUDIO && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Uri imageUri = fm.insertLocalFile(uri.getLastPathSegment(), uri);
			point.setAudioUrl(imageUri.toString());
			audioFileLabel.setText(point.getAudioUrl());
		}
	}

	@Override
	public void categorySelected(Category category) {
		/*categoryField.setText(category.getDescription());
		selectedCategory = category;
		*/
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
