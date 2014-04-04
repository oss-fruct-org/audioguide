package org.fruct.oss.audioguide.fragments.edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.util.Utils;

public class EditPointDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final int IMAGE_REQUEST_CODE = 0;
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

    private EditText imageFileText;
    private Button imageFileButton;

	public EditPointDialog(Point point) {
		if (point == null) {
			this.point = new Point();
			isNewPoint = true;
		} else {
			this.point = point;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_add_point, null);
		assert view != null;

		editName = (EditText) view.findViewById(R.id.text_title);
		editDescription = (EditText) view.findViewById(R.id.text_description);

        imageFileText = (EditText) view.findViewById(R.id.image_file);
        imageFileButton = (Button) view.findViewById(R.id.image_file_button);
        //editUrl = (EditText) view.findViewById(R.id.edit_url);

		if (point != null) {
			if (!Utils.isNullOrEmpty(point.getName())) editName.setText(point.getName());
			if (!Utils.isNullOrEmpty(point.getDescription())) editDescription.setText(point.getDescription());
			//if (!Utils.isNullOrEmpty(point.point())) editUrl.setText(point.getUrl());
		}

        imageFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooserDialog();
            }
        });

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private void showFileChooserDialog() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(Intent.createChooser(intent, "Choose file"), IMAGE_REQUEST_CODE);
    }

	public void setListener(Listener listener) {
		this.listener = listener;
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                imageFileText.setText(data.getDataString());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
