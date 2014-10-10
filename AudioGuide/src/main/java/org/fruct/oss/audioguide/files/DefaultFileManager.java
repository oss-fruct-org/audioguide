package org.fruct.oss.audioguide.files;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.PostUrlContent;
import org.fruct.oss.audioguide.track.GetsBackend;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DefaultFileManager implements FileManager, Closeable {
	private static final Logger log = LoggerFactory.getLogger(DefaultFileManager.class);

	private final Context context;
	private final File cacheDir;
	//private final IconCache iconCache;
	private final WeakHashMap<FileListener, Object> fileListeners = new WeakHashMap<FileListener, Object>();
	private final Handler mainHandler;

	private final ExecutorService executor;
	private final ExecutorService scaleExecutor;

	private List<FileRecord> files = new ArrayList<FileRecord>();

	private final Map<String, Future<String>> requestedRemoteUrls = new HashMap<String, Future<String>>();
	private final Map<String, Runnable> requestedBitmaps = new HashMap<String, Runnable>();
	private final Map<String, List<BitmapSetter>> activeBitmapSetters = new HashMap<String, List<BitmapSetter>>();

	private boolean isClosed;

	DefaultFileManager(Context context) {
		this.context = context;

		executor = Executors.newSingleThreadExecutor();
		scaleExecutor = Executors.newSingleThreadExecutor();

		mainHandler = new Handler(Looper.getMainLooper());

		cacheDir = new File(context.getExternalCacheDir(), "ag-file-storage");
		cacheDir.mkdir();

		executor.execute(new Runnable() {
			@Override
			public void run() {
				initFromLocalCache();
			}
		});

		/*iconCache = new IconCache(1024);
		downloadThread = new Thread(this);
		downloadThread.start();*/
	}

	private void initFromLocalCache() {
		for (File file : cacheDir.listFiles()) {
			String cachedPath = file.getPath();
			FileRecord fileRecord = updateLocalRecord(null, null, cachedPath);
		}
	}

	private synchronized FileRecord updateLocalRecord(String remoteUrl, String localUrl, String cachedPath) {
		FileRecord matchedFileRecord = null;
		for (FileRecord fileRecord : files) {
			if (cachedPath != null && cachedPath.equals(fileRecord.cachedPath)) {
				matchedFileRecord = fileRecord;
				break;
			}

			if (remoteUrl != null && remoteUrl.equals(fileRecord.remoteUrl)) {
				matchedFileRecord = fileRecord;
				break;
			}
		}

		if (matchedFileRecord == null) {
			matchedFileRecord = new FileRecord();
			files.add(matchedFileRecord);
		}

		if (remoteUrl != null) {
			matchedFileRecord.remoteUrl = remoteUrl;
		}

		if (localUrl != null) {
			matchedFileRecord.localUrl = localUrl;
		}

		if (cachedPath != null) {
			matchedFileRecord.cachedPath = cachedPath;
		}

		return matchedFileRecord;
	}

	private synchronized FileRecord findFileRecordByRemoteUrl(String remoteUrl) {
		String cachedPath = new File(cacheDir, Utils.hashString(remoteUrl)).getPath();
		for (FileRecord fileRecord : files) {
			if (remoteUrl.equals(fileRecord.remoteUrl)) {
				return fileRecord;
			}

			if (cachedPath.equals(fileRecord.cachedPath)) {
				fileRecord.remoteUrl = remoteUrl;
				return fileRecord;
			}
		}

		return null;
	}

	private FileRecord findFileRecordByCachedPath(String cachedPath) {
		for (FileRecord fileRecord : files) {
			if (cachedPath.equals(fileRecord.cachedPath)) {
				return fileRecord;
			}
		}

		return null;
	}


	@Override
	public synchronized void close() {
		if (!isClosed) {
			log.debug("close");
			executor.shutdown();
			scaleExecutor.shutdown();
			isClosed = true;
		}

		instance = null;
	}

	@Override
	public Uri insertLocalFile(String title, Uri localUri) {
		File cacheFile = new File(cacheDir, "upload-" + UUID.randomUUID().toString());
		performFileCopying(localUri, cacheFile);
		updateLocalRecord(null, localUri.toString(), cacheFile.getPath());

		log.trace("Inserting local file + " + localUri + ", cached " + cacheFile);

		return Uri.fromFile(cacheFile);
	}

	private void performFileCopying(final Uri localUri, final File cacheFile) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				InputStream in = null;
				FileOutputStream out = null;
				try {
					ContentResolver resolver = context.getContentResolver();
					in = resolver.openInputStream(localUri);

					out = new FileOutputStream(cacheFile);

					Utils.copyStream(in, out);
				} catch (IOException ex) {

				} finally {
					Utils.sclose(in);
					Utils.sclose(out);
				}
			}
		});
	}

	@Override
	public void insertRemoteFile(String title, final Uri remoteUri) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				File cachedFile = new File(cacheDir, Utils.hashString(remoteUri.toString()));
				String cachedPath = cachedFile.exists() ? cachedFile.getPath() : null;
				updateLocalRecord(remoteUri.toString(), null, cachedPath);

				log.trace("Inserting remote file " + remoteUri + ". Cached file " + cachedFile);
			}
		});
	}

	@Override
	public String getLocalPath(Uri remoteUri) {
		log.trace("Local path requested {}", remoteUri);

		if (remoteUri.getScheme().equals("file")) {
			return remoteUri.getPath();
		}

		FileRecord fileRecord = findFileRecordByRemoteUrl(remoteUri.toString());
		if (fileRecord == null) {
			log.trace("  File record not found");
			insertRemoteFile("no-title", remoteUri);
			return null;
		} else {
			log.trace("  File record found with cachedPath {}", fileRecord.cachedPath);
			if (fileRecord.cachedPath == null) {
				return null;
			} else {
				return fileRecord.cachedPath;
			}
		}
	}

	@Override
	public Uri uploadLocalFile(Uri cachedUri) throws IOException, GetsException {
		log.trace("Uploading cached file {}", cachedUri);

		FileRecord fileRecord = findFileRecordByCachedPath(cachedUri.getPath());
		if (fileRecord == null) {
			log.warn("  No file record for requested url found");
			return null;
		}

		if (fileRecord.remoteUrl != null) {
			log.warn("  Remote url for requested url already exists");
			return Uri.parse(fileRecord.remoteUrl);
		}

		Uri remoteUri = uploadFile(fileRecord.title, cachedUri);
		File cachedFile = new File(cacheDir, Utils.hashString(remoteUri.toString()));
		new File(fileRecord.cachedPath).renameTo(cachedFile);

		fileRecord.cachedPath = cachedFile.getPath();

		log.trace("  File uploaded. Cached file renamed to {}", fileRecord.cachedPath);

		return remoteUri;
	}

	@Override
	public void requestAudioDownload(final String remoteUrl) {
		String cachedPath = getLocalPath(Uri.parse(remoteUrl));
		if (cachedPath == null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					downloadUrl(remoteUrl);
				}
			});
		}
	}

	private void requestDownload(final String remoteUrl) {
		synchronized (requestedRemoteUrls) {
			if (!requestedRemoteUrls.containsKey(remoteUrl)) {
				Callable<String> callable = new Callable<String>() {
					@Override
					public String call() throws Exception {
						try {
							String localPath = downloadUrl(remoteUrl);
							synchronized (requestedBitmaps) {
								Runnable runnable = requestedBitmaps.get(remoteUrl);
								scaleExecutor.execute(runnable);
							}
							return localPath;
						} finally {
							synchronized (requestedRemoteUrls) {
								requestedRemoteUrls.remove(remoteUrl);
							}
						}
					}
				};

				Future<String> future = executor.submit(callable);
				requestedRemoteUrls.put(remoteUrl, future);
			}
		}
	}

	@Override
	public void requestImageBitmap(final String remoteUrl, final int width, final int height, final ScaleMode mode, final BitmapSetter bitmapSetter, String clientId) {
		log.trace("Requested image bitmap {}", remoteUrl);
		Utils.putMultiMap(activeBitmapSetters, clientId, bitmapSetter);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (bitmapSetter.getTag() != this)
						return;

					String cachedPath = getLocalPath(Uri.parse(remoteUrl));
					if (cachedPath == null) {
						log.error("  Request image bitmap failed");
						return;
					}

					Bitmap bitmap = AUtils.decodeSampledBitmapFromResource(Resources.getSystem(),
								cachedPath, width, height);

					if (mode != ScaleMode.NO_SCALE) {
						float ax = bitmap.getWidth() / (float) width;
						float ay = bitmap.getHeight() / (float) height;
						float ma = mode == ScaleMode.SCALE_CROP
								? Math.min(ax, ay)
								: Math.max(ax, ay);

						Matrix matrix = new Matrix();
						matrix.postScale(1/ma, 1/ma);

						Bitmap oldBitmap = bitmap;
						if (mode == ScaleMode.SCALE_CROP) {
							// TODO: scale_crop can't handle non-square dst
							int minDim = Math.min(bitmap.getWidth(), bitmap.getHeight());
							bitmap = Bitmap.createBitmap(bitmap, 0, 0, minDim, minDim, matrix, true);
						} else {
							bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
						}
						oldBitmap.recycle();
					}

					bitmapSetter.bitmapReady(bitmap);
				} finally {
					synchronized (scaleExecutor) {
						requestedBitmaps.remove(remoteUrl);
					}
				}
			}
		};

		synchronized (scaleExecutor) {
			String cachedPath = getLocalPath(Uri.parse(remoteUrl));
			bitmapSetter.setTag(runnable);
			if (cachedPath == null) {
				requestedBitmaps.put(remoteUrl, runnable);
				requestDownload(remoteUrl);
			} else {
				scaleExecutor.submit(runnable);
			}
		}
	}

	@Override
	public void recycleAllBitmaps(String clientId) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			for (BitmapSetter bitmapSetter : Utils.getMultiMap(activeBitmapSetters, clientId)) {
				bitmapSetter.recycle();
			}
		}

		activeBitmapSetters.clear();
	}

	private String downloadUrl(final String remoteUrl) {
		String cachedPath = getLocalPath(Uri.parse(remoteUrl));
		if (cachedPath != null) {
			return cachedPath;
		}

		File cachedFile = new File(cacheDir, Utils.hashString(remoteUrl));
		try {
			downloadUrl(remoteUrl, cachedFile);
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					for (FileListener fileListener : fileListeners.keySet()) {
						fileListener.itemLoaded(remoteUrl);
					}
				}
			});

		} catch (IOException e) {
			log.error("Cannot download url: " + remoteUrl, e);
			cachedFile.delete();
			return null;
		}

		log.trace("Url {} loaded", remoteUrl);
		updateLocalRecord(remoteUrl, null, cachedFile.getPath());
		return cachedFile.getPath();
	}


	/*@Override
	public Bitmap getImageBitmap(String remoteUrl, int width, int height, ScaleMode mode) {
		Bitmap bitmap = iconCache.get(remoteUrl, mode);
		if (bitmap == null || bitmap.getWidth() < width || bitmap.getHeight() < height) {
			// Create sampled bitmap that not worse than passed dimension (width, height)
			String localUrl = getLocalPath(Uri.parse(remoteUrl));
			if (localUrl == null || !new File(localUrl).exists()) {
				insertRemoteFile("no-title", Uri.parse(remoteUrl));
				return null;
			}

			bitmap = AUtils.decodeSampledBitmapFromResource(Resources.getSystem(),
					localUrl, width, height);
			iconCache.put(remoteUrl, mode, bitmap);
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
	}*/

	private Uri uploadFile(String title, Uri cachedUri) throws IOException, GetsException {
		String postUrl = uploadStage1(title);
		FileContent fileContent = uploadStage2(cachedUri, postUrl);
		return Uri.parse(fileContent.getUrl());
	}

	private String uploadStage1(String title) throws IOException, GetsException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String token = pref.getString(GetsBackend.PREF_AUTH_TOKEN, null);

		// FIXME: escape strings in xml
		String request = String.format(Locale.ROOT, GetsBackend.UPLOAD_FILE, token, title);
		String uploadUrl = Gets.GETS_SERVER + "/files/uploadFile.php";

		String responseStr = Utils.downloadUrl(uploadUrl, request);
		GetsResponse response = GetsResponse.parse(responseStr, PostUrlContent.class);

		if (response.getCode() != 0) {
			throw new GetsException("Gets return error " + response.getCode());
		}

		return ((PostUrlContent) response.getContent()).getPostUrl();
	}

	private FileContent uploadStage2(Uri localUri, String postUrl) throws IOException, GetsException {
		ContentResolver resolver = context.getContentResolver();
		InputStream stream = resolver.openInputStream(localUri);
		String mimeType = resolver.getType(localUri);
		if (mimeType == null) {
			log.warn("Cannot determine mime type of content {}", localUri);
			mimeType = "image/png";
		}

		if (stream == null) {
			throw new IOException("Can't retrieve local file stream");
		}

		String responseStr = Utils.postStream(postUrl, stream, mimeType);
		GetsResponse response = GetsResponse.parse(responseStr, FileContent.class);

		if (response.getCode() != 0) {
			// TODO: "Error request to google drive" can cause crash
			log.error("Gets did return error code " + response.getCode());
			throw new GetsException("Gets return error " + response.getCode());
		}

		return ((FileContent) response.getContent());
	}

	@Override
	public boolean isLocal(Uri uri) {
		return uri.getScheme().equals("file");
	}

	@Override
	public void addWeakListener(FileListener pointCursorAdapter) {
		fileListeners.put(pointCursorAdapter, "null");
	}

	private static DefaultFileManager instance;
	public synchronized static DefaultFileManager getInstance() {
		if (instance == null) {
			instance = new DefaultFileManager(App.getContext());
		}

		return instance;
	}

	/*@Override
	public void run() {
		while (!isClosed) {
			Cursor cursor;
			synchronized (db) {
				if (isClosed)
					break;

				cursor = db.rawQuery("SELECT title, remoteUrl FROM file " +
						"WHERE cacheUrl IS NULL;", null);

				if (!cursor.moveToFirst()) {
					try {
						db.wait();
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {
					}
					cursor.close();
					continue;
				}
			}

			do {
				final String remoteUrl = cursor.getString(1);
				final String title = cursor.getString(0);
				final File cacheFile = new File(cacheDir, UUID.randomUUID().toString());

				try {
					downloadUrl(remoteUrl, cacheFile);

					synchronized (db) {
						if (isClosed)
							return;

						db.execSQL("UPDATE file SET cacheUrl=? WHERE remoteUrl=?",
								Utils.toArray(Uri.fromFile(cacheFile), remoteUrl));
					}

					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							for (FileListener listener : fileListeners.keySet()) {
								listener.itemLoaded(remoteUrl);
							}
						}
					});
				} catch (IOException ex) {
					log.error("Error downloading file: {}", remoteUrl);
					cacheFile.delete();
				}

				if (isClosed)
					return;
			} while (cursor.moveToNext());

			cursor.close();
		}
	}*/

	private void downloadUrl(final String uri, File localFile) throws IOException {
		URL url = new URL(uri);

		OutputStream output = null;
		InputStream input = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(15000);
			conn.setRequestMethod("GET");
			conn.connect();

			int code = conn.getResponseCode();
			int fileSize;
			try {
				fileSize = Integer.parseInt(conn.getHeaderField("Content-Length"));
			} catch (NumberFormatException ex) {
				fileSize = 1;
			}

			if (code == 200) {
				output = new FileOutputStream(localFile);
				input = conn.getInputStream();
				input = new ProgressInputStream(input, fileSize, 100000, new ProgressInputStream.ProgressListener() {
					@Override
					public void update(final int current, final int max) {
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								for (FileListener fileListener : fileListeners.keySet()) {
									fileListener.itemDownloadProgress(uri, Math.min(current, max), max);
								}
							}
						});
					}
				});
				Utils.copyStream(input, output);
			}
		} finally {
			if (output != null)
				output.close();

			if (input != null)
				input.close();

			if (conn != null)
				conn.disconnect();
		}
	}

	private static class FileRecord {
		String title;
		String remoteUrl;
		String cachedPath;
		String localUrl;
	}
}
