package org.fruct.oss.audioguide.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.FilterModel;
import org.fruct.oss.audioguide.models.Model;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.FilesContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

public class FileManager implements SharedPreferences.OnSharedPreferenceChangeListener, Closeable {
	public enum ScaleMode {
		NO_SCALE, SCALE_CROP, SCALE_FIT
	}

	private final static Logger log = LoggerFactory.getLogger(FileManager.class);

	private final Context context;
	private String authToken;

	private Downloader downloader;

	private IconCache iconCache;

	private PendingFiles pendingFiles;

	private Thread downloadThread;

	private WeakHashMap<FileListener, Object> fileListeners = new WeakHashMap<FileListener, Object>();


	// Files available in GeTS
	private List<FileContent> getsFiles = new ArrayList<FileContent>();

	public FileManager(Context context) {
		this.context = context;

		pendingFiles = new PendingFiles(context);

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
					String responseString = Utils.downloadUrl(Gets.GETS_SERVER + "/files/listFiles.php", request);
					GetsResponse response = GetsResponse.parse(responseString, FilesContent.class);

					if (response.getCode() != 0) {
						log.error("Error code returned while downloading files");
						AUtils.reportError(context, "Can't download GeTS file list");
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
				for (FileContent fileContent : filesContent.getFiles()) {
					String remoteUrl = fileContent.getUrl();
					pendingFiles.insert(remoteUrl);
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

	private BaseModel<FileContent> imagesModel = new BaseModel<FileContent>();

	public void addFile(FileContent file) {
		getsFiles.add(file);

		notifyFilesUpdated();

		if (file.isImage()) {
			insertImageUri(Uri.parse(file.getUrl()));
		}
	}

	public Bitmap getImageBitmap(String remoteUrl, int width, int height, ScaleMode mode) {
		Bitmap bitmap = iconCache.get(remoteUrl);
		if (bitmap == null || bitmap.getWidth() < width || bitmap.getHeight() < height) {
			// Create sampled bitmap that not worse than passed dimension (width, height)
			String localUrl = downloader.getUrl(remoteUrl);
			if (localUrl == null) {
				pendingFiles.insert(remoteUrl);
				return null;
			}

			//if (bitmap != null) {
			//	bitmap.recycle();
			//}

			bitmap = AUtils.decodeSampledBitmapFromResource(Resources.getSystem(),
					localUrl, width, height);
			iconCache.put(remoteUrl, bitmap);
		}

		if (mode == ScaleMode.NO_SCALE) {
			return bitmap;
		} else {
			float ax = bitmap.getWidth() / (float) width;
			float ay = bitmap.getHeight() / (float) height;
			float ma = mode == ScaleMode.SCALE_CROP
					? Math.min(ax, ay)
					: Math.max(ax, ay);

			Matrix matrix = new Matrix();
			matrix.postScale(1/ma, 1/ma);

			if (mode == ScaleMode.SCALE_CROP) {
				// TODO: scale_crop can't handle non-square dst
				int minDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
				return Bitmap.createBitmap(bitmap, 0, 0, minDim, minDim, matrix, true);
			} else {
				return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}
		}
	}

	public Bitmap getImageBitmap(String remoteUrl, int width, int height) {
		return getImageBitmap(remoteUrl, width, height, ScaleMode.NO_SCALE);
	}

	public Bitmap getImageBitmap(String remoteUrl) {
		return getImageBitmap(remoteUrl, Utils.getDP(48), Utils.getDP(48));
	}

	public boolean isFileLocal(String remoteUrl) {
		return null != downloader.getUrl(remoteUrl);
	}

	public void insertImageUri(Uri uri) {
		log.info("insert image uri {}", uri);
		//imageDownloader.insertUri(uri);
		pendingFiles.insert(uri.toString());
	}

	public void insertAudioUri(String uri) {
		log.info("insert audio uri {}", uri);

		//audioDownloader.insertUri(uri);
		pendingFiles.insert(uri);

	}

	public Uri getAudioUri(Uri remoteUri) {
		String remoteUrl = remoteUri.toString();

		String localUrl = downloader.getUrl(remoteUrl);
		if (localUrl == null) {
			pendingFiles.insert(remoteUrl);
			return remoteUri;
		}

		return Uri.parse(localUrl);
	}

	private Runnable downloadRunnable = new Runnable() {
		private PendingFiles pendingFiles;
		private Handler handler = new Handler(Looper.getMainLooper());
		private Downloader downloader;
		private String currentRemoteUrl;

		@Override
		public void run() {
			this.pendingFiles = FileManager.this.pendingFiles;
			this.downloader = FileManager.this.downloader;

			downloader.setProgressListener(new ProgressInputStream.ProgressListener() {
				@Override
				public void update(final int current, final int max) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							for (FileListener listener : fileListeners.keySet()) {
								listener.itemDownloadProgress(currentRemoteUrl, current, max);
							}
						}
					});
				}
			});

			while (!Thread.interrupted()) {
				if (!turn()) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}

		private boolean turn() {
			assert pendingFiles != null;
			final String pendingUrl= pendingFiles.getPendingUrl();
			if (pendingUrl == null) {
				log.debug("No files to download, waiting");
				synchronized (pendingFiles) {
					try {
						pendingFiles.wait();
					} catch (InterruptedException ex) {
						return false;
					}
				}
			} else {
				// Download url using downloader
				log.debug("Loading {}", pendingUrl);
				currentRemoteUrl = pendingUrl;
				String localUrl = downloader.downloadRemoteUrl(pendingUrl);
				if (localUrl == null) {
					log.error("Can't download file. Stopping thread");
					AUtils.reportError(context, "Error downloading file");
					return false;
				}
				log.debug("Successfully loaded {}: {}", pendingUrl, localUrl);

				pendingFiles.remove(pendingUrl);

				handler.post(new Runnable() {
					@Override
					public void run() {
						for (FileListener listener : fileListeners.keySet()) {
							listener.itemLoaded(pendingUrl);
						}
					}
				});
				currentRemoteUrl = null;
			}

			return true;
		}
	};

	public static FileManager instance;
	public synchronized static FileManager getInstance() {
		if (instance == null) {
			instance = new FileManager(App.getContext()) {
				@Override
				public synchronized void close() {
					super.close();
				}

				@Override
				public void addWeakListener(FileListener listener) {
					super.addWeakListener(listener);
				}

				@Override
				public void startLoading() {
				}

				@Override
				public Model<FileContent> getImagesModel() {
					return super.getImagesModel();
				}

				@Override
				public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
					super.onSharedPreferenceChanged(sharedPreferences, key);
				}

				@Override
				public void addFile(FileContent file) {
				}

				@Override
				public Bitmap getImageBitmap(String remoteUrl, int width, int height, ScaleMode mode) {
					return super.getImageBitmap(remoteUrl, width, height, mode);
				}

				@Override
				public Bitmap getImageBitmap(String remoteUrl, int width, int height) {
					return super.getImageBitmap(remoteUrl, width, height);
				}

				@Override
				public Bitmap getImageBitmap(String remoteUrl) {
					return super.getImageBitmap(remoteUrl);
				}

				@Override
				public boolean isFileLocal(String remoteUrl) {
					return super.isFileLocal(remoteUrl);
				}

				@Override
				public void insertImageUri(Uri uri) {
				}

				@Override
				public void insertAudioUri(String uri) {
				}

				@Override
				public Uri getAudioUri(Uri remoteUri) {
					return super.getAudioUri(remoteUri);
				}
			};
		}

		return instance;
	}
}
