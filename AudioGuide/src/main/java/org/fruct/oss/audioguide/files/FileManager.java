package org.fruct.oss.audioguide.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.models.FilterModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.FilesContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.WeakHashMap;

public class FileManager implements SharedPreferences.OnSharedPreferenceChangeListener, Closeable {
	private final static Logger log = LoggerFactory.getLogger(FileManager.class);

	private final Context context;
	private String authToken;

	private Downloader downloader;

	private IconCache iconCache;

	private FileStorage fileStorage;

	private Thread downloadThread;

	private WeakHashMap<FileListener, Object> fileListeners = new WeakHashMap<FileListener, Object>();

	// Files available in GeTS
	private List<FileContent> getsFiles = new ArrayList<FileContent>();

	public FileManager(Context context) {
		this.context = context;

		fileStorage = new FileStorage(context);

		this.iconCache = new IconCache(1024);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.registerOnSharedPreferenceChangeListener(this);

		String authToken = pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);
		setNewAuthToken(authToken);

		downloader = new Downloader(context, "files-downloaded");
		downloadThread = new Thread(downloadRunnable);
		downloadThread.setName("Downloader thread");
		downloadThread.start();
	}

	public synchronized void close() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.unregisterOnSharedPreferenceChangeListener(this);

		downloadThread.interrupt();
		downloadThread = null;
	}

	public void addWeakListener(FileListener listener) {
		fileListeners.put(listener, log);
	}

	/**
	 * Downloads list of GeTS files
	 * This method can be called many times to keep file list up to date
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

				// New GeTS files ready
				// Store them in database
				for (FileContent fileContent : filesContent.getFiles()) {
					String remoteUrl = fileContent.getUrl();
					fileStorage.insertRemoteUrl(remoteUrl);
				}

				getsFiles.clear();
				getsFiles.addAll(filesContent.getFiles());

				notifyFilesUpdated();
			}
		};

		filesTask.execute();
	}

	private void notifyFilesUpdated() {
		imagesModel.setData(getsFiles);
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
		startLoading();
	}

	private FilterModel<FileContent> imagesModel = new FilterModel<FileContent>() {
		@Override
		public boolean check(FileContent fileContent) {
			return true;
		}
	};

	public void addFile(FileContent file) {
		getsFiles.add(file);

		notifyFilesUpdated();

		if (file.isImage()) {
			insertImageUri(Uri.parse(file.getUrl()));
		}
	}

	public Bitmap getImageBitmap(String remoteUrl) {
		Bitmap bitmap = iconCache.get(remoteUrl);
		if (bitmap != null)
			return bitmap;

		String localUrl = fileStorage.getLocalUrl(remoteUrl);

		if (localUrl != null) {
			Bitmap newBitmap = BitmapFactory.decodeFile(localUrl);
			Bitmap thumbBitmap = ThumbnailUtils.extractThumbnail(newBitmap,
					Utils.getDP(48), Utils.getDP(48));
			newBitmap.recycle();
			iconCache.put(remoteUrl, thumbBitmap);
			return thumbBitmap;
		}

		return null;
	}

	public Bitmap getImageFullBitmap(String remoteUrl) {
		String localUrl = fileStorage.getLocalUrl(remoteUrl);

		if (localUrl != null) {
			return BitmapFactory.decodeFile(localUrl);
		}

		return null;
	}

	public void insertImageUri(Uri uri) {
		log.info("insert image uri {}", uri);
		//imageDownloader.insertUri(uri);
		fileStorage.insertRemoteUrl(uri.toString());
	}


	// Audio methods
	public void insertAudioUri(Uri uri) {
		log.info("insert audio uri {}", uri);

		//audioDownloader.insertUri(uri);
		fileStorage.insertRemoteUrl(uri.toString());
	}

	public Uri getAudioUri(Uri remoteUri) {
		String remoteUrl = remoteUri.toString();

		String localUrl = fileStorage.getLocalUrl(remoteUrl);
		if (localUrl == null) {
			fileStorage.insertRemoteUrl(remoteUrl);
			return remoteUri;
		}

		return Uri.parse(localUrl);
	}

	private Runnable downloadRunnable = new Runnable() {
		private FileStorage fileStorage;
		private Handler handler = new Handler(Looper.getMainLooper());
		private Downloader downloader;

		@Override
		public void run() {
			this.fileStorage = FileManager.this.fileStorage;
			this.downloader = FileManager.this.downloader;

			while (!Thread.interrupted()) {
				if (!turn()) {
					break;
				}
			}
		}

		private boolean turn() {
			assert fileStorage != null;
			final String pendingUrl= fileStorage.getPendingUrl();
			if (pendingUrl == null) {
				log.debug("No files to download, waiting");
				synchronized (fileStorage) {
					try {
						fileStorage.wait();
					} catch (InterruptedException ex) {
						return false;
					}
				}
			} else {
				// Download url using downloader
				log.debug("Loading {}", pendingUrl);
				String localUrl = downloader.getUri(pendingUrl);
				if (localUrl == null) {
					log.error("Can't download file stopping thread");
					return false;
				}
				log.debug("Successfully loaded {}: {}", pendingUrl, localUrl);

				fileStorage.setFileLocal(pendingUrl, localUrl);

				handler.post(new Runnable() {
					@Override
					public void run() {
						for (FileListener listener : fileListeners.keySet()) {
							listener.itemLoaded(pendingUrl);
						}
					}
				});
			}

			return true;
		}
	};

	public static FileManager instance;
	public synchronized static FileManager getInstance() {
		if (instance == null) {
			instance = new FileManager(App.getContext());
		}

		return instance;
	}
}
