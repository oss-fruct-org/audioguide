package org.fruct.oss.audioguide.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.PostUrlContent;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class UploadFragment extends DialogFragment {
	private final static Logger log = LoggerFactory.getLogger(UploadFragment.class);

	public static interface Listener {
		void fileCreated(FileContent file);
	}

	private static final int IMAGE_REQUEST_CODE = 0;

	private Listener listener;

	private Uri localFileUri;
	private EditText titleEdit;
	private Button browseButton;
	private ProgressBar progressBar;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_upload_file, null);
		assert view != null;

		titleEdit = ((EditText) view.findViewById(R.id.title_text));
		browseButton = (Button) view.findViewById(R.id.browse_button);
		progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

		progressBar.setVisibility(View.GONE);

		browseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*");
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				startActivityForResult(Intent.createChooser(intent, "Choose file"), IMAGE_REQUEST_CODE);
			}
		});

		builder.setView(view);

		return builder.create();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				localFileUri = data.getData();
				log.debug("User select file {}, {}", localFileUri.toString(), data.getType());
				browseButton.setText("Upload");
				browseButton.setOnClickListener(uploadListener);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private View.OnClickListener uploadListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
			String token = pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);

			assert titleEdit != null;
			String request = String.format(Locale.ROOT, GetsStorage.UPLOAD_FILE, token, titleEdit.getText().toString());
			uploadStage1(GetsStorage.GETS_SERVER + "/files/uploadFile.php", request);
		}
	};

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	private void uploadStage1(final String url, final String request) {
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... voids) {
				try {
					String responseStr = Utils.downloadUrl(url, request);
					GetsResponse response = GetsResponse.parse(responseStr, PostUrlContent.class);

					if (response.getCode() != 0) {
						showError("Server return error: " + response.getMessage());
					}

					return ((PostUrlContent) response.getContent()).getPostUrl();
				} catch (IOException e) {
					log.error("GeTS error: ", e);
					showError("Error uploading file");
				} catch (GetsException e) {
					log.error("Response error: ", e);
					showError("Error uploading file: incorrect answer from server");
				}

				return null;
			}

			@Override
			protected void onPostExecute(String uploadUrl) {
				uploadStage2(uploadUrl);
			}
		}.execute();
 	}

	private void uploadStage2(final String uploadUrl) {
		AsyncTask<Void, Integer, FileContent> task = new AsyncTask<Void, Integer, FileContent>() {
			private int fileSize;
			private boolean started = false;

			@Override
			protected FileContent doInBackground(Void... voids) {
				try {
					InputStream stream = getActivity().getContentResolver().openInputStream(localFileUri);

					if (stream == null) {
						showError("Wrong file");
						return null;
					}

					fileSize = stream.available();
					log.debug("Stream size {}", stream.available());

					ProgressInputStream pStream = new ProgressInputStream(stream, fileSize, fileSize / 10,
							new ProgressInputStream.ProgressListener() {
						@Override
						public void update(int current, int max) {
							publishProgress(current);
						}
					});

					String responseStr = Utils.postStream(uploadUrl, pStream, "image/png");
					GetsResponse response = GetsResponse.parse(responseStr, FileContent.class);

					if (response.getCode() != 0) {
						log.error("Gets did return error code");
						showError("Error uploading file");
						return null;
					}

					return ((FileContent) response.getContent());
				} catch (IOException e) {
					log.error("GeTS error: ", e);
					showError("Error uploading file");
				} catch (GetsException e) {
					log.error("GeTS error: ", e);
					showError("Error uploading file");
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);

				if (!started) {
					started = true;
					progressBar.setMax(fileSize);
					progressBar.setVisibility(View.VISIBLE);
				}

				progressBar.setProgress(values[0]);
			}

			@Override
			protected void onPostExecute(FileContent fileContent) {
				Toast.makeText(getActivity(), "File successfully uploaded", Toast.LENGTH_LONG).show();

				if (listener != null) {
					listener.fileCreated(fileContent);
				}
				
				UploadFragment.this.dismiss();
			}
		}.execute();
	}

	private void showError(final String error) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
			}
		});
	}
}
