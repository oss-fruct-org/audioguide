package org.fruct.oss.audioguide.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.models.FilterModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.FilesContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.Downloader;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileManager implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static Logger log = LoggerFactory.getLogger(FileManager.class);

	private final Context context;
	private String authToken;
	private List<FileContent> files = new ArrayList<FileContent>();

	private Downloader imageDownloader;
	private Downloader audioDownloader;

	private IconCache iconCache;

	public FileManager(Context context) {
		this.context = context;

		imageDownloader = new Downloader(context, "point-icons");
		audioDownloader = new Downloader(context, "audio-tracks");

		this.iconCache = new IconCache(1024);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.registerOnSharedPreferenceChangeListener(this);

		String authToken = pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);
		setNewAuthToken(authToken);
	}

	public void close() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Begins background loading of files
	 */
	public void startLoading() {
		AsyncTask<Void, Void, FilesContent> filesTask = new AsyncTask<Void, Void, FilesContent>() {
			@Override
			protected FilesContent doInBackground(Void... voids) {
				if (authToken == null) {
					log.warn("Trying get files without auth token");
					return null;
				}

				String request = String.format(Locale.ROOT, GetsStorage.LIST_FILES, authToken);
				try {
					String responseString = Utils.downloadUrl(GetsStorage.GETS_SERVER + "/files/listFiles.php", request);
					GetsResponse response = GetsResponse.parse(responseString, FilesContent.class);

					if (response.getCode() != 0) {
						log.error("Error code returned while downloading files");
						return null;
					}

					return ((FilesContent) response.getContent());
				} catch (IOException e) {
					log.error("File list download error: ", e);
					return null;
				} catch (GetsException e) {
					log.error("Wrong response from server: ", e);
					return null;
				}
			}

			@Override
			protected void onPostExecute(FilesContent filesContent) {
				if (filesContent == null)
					return;

				files.clear();
				files.addAll(filesContent.getFiles());

				notifyFilesUpdated();
			}
		};

		filesTask.execute();
	}

	private void notifyFilesUpdated() {
		imagesModel.setData(files);
	}

	public Model<FileContent> getImagesModel() {
		return imagesModel;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(GetsStorage.PREF_AUTH_TOKEN)) {
			setNewAuthToken(sharedPreferences.getString(GetsStorage.PREF_AUTH_TOKEN, null));
		}
	}

	private void setNewAuthToken(String token) {
		authToken = token;
		files.clear();
		startLoading();
	}

	private FilterModel<FileContent> imagesModel = new FilterModel<FileContent>() {
		@Override
		public boolean check(FileContent fileContent) {
			return true;
		}
	};

	public void addFile(FileContent file) {
		files.add(file);
		notifyFilesUpdated();

		if (file.isImage()) {
			insertImageUri(Uri.parse(file.getUrl()));
		}
	}


	// Image methods
	public void addWeakImageListener(Downloader.Listener listener) {
		imageDownloader.addWeakListener(listener);
	}

	public Bitmap getImageBitmap(String imageUrl) {
		Bitmap bitmap = iconCache.get(imageUrl);
		if (bitmap != null)
			return bitmap;

		Uri remotePhotoUri = Uri.parse(imageUrl);
		Uri localPhotoUri = imageDownloader.getUri(remotePhotoUri);

		if (localPhotoUri != null && !localPhotoUri.equals(remotePhotoUri)) {
			String localPhotoPath = localPhotoUri.getPath();
			Bitmap newBitmap = BitmapFactory.decodeFile(localPhotoPath);
			Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(newBitmap,
					Utils.getDP(48), Utils.getDP(48));
			newBitmap.recycle();
			iconCache.put(imageUrl, thumbBitmap);
			return thumbBitmap;
		}

		return null;
	}

	public void insertImageUri(Uri uri) {
		imageDownloader.insertUri(uri);
	}


	// Audio methods
	public void insertAudioUri(Uri uri) {
		audioDownloader.insertUri(uri);
	}

	public Uri getAudioUri(Uri uri) {
		return audioDownloader.getUri(uri);
	}

	public static FileManager instance;
	public synchronized static FileManager getInstance() {
		if (instance == null) {
			instance = new FileManager(App.getContext());
		}

		return instance;
	}

}
