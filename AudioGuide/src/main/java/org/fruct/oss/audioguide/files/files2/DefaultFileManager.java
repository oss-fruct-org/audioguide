package org.fruct.oss.audioguide.files.files2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.IconCache;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.parsers.FileContent;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.PostUrlContent;
import org.fruct.oss.audioguide.track.GetsStorage;
import org.fruct.oss.audioguide.util.AUtils;
import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

public class DefaultFileManager implements FileManager {
	private static final Logger log = LoggerFactory.getLogger(DefaultFileManager.class);

	private final Context context;
	private final FileDatabaseHelper dbHelper;
	private final SQLiteDatabase db;
	private final File cacheDir;
	private final IconCache iconCache;
	private final WeakHashMap<FileListener, Object> fileListeners = new WeakHashMap<FileListener, Object>();

	DefaultFileManager(Context context) {
		this.context = context;
		dbHelper = new FileDatabaseHelper(context);
		db = dbHelper.getWritableDatabase();

		cacheDir = new File(context.getExternalCacheDir(), "ag-file-storage");
		cacheDir.mkdir();

		iconCache = new IconCache(1024);
	}

	public void close() {
		dbHelper.close();
	}

	@Override
	public Uri insertLocalFile(String title, Uri localUri) {
		File cacheFile = new File(cacheDir, UUID.randomUUID().toString());

		db.execSQL("INSERT INTO file VALUES (?, ?, ?, NULL, 0);",
				Utils.toArray(title, localUri.toString(), Uri.fromFile(cacheFile).toString()));

		performFileCopying(localUri, cacheFile);
		return Uri.fromFile(cacheFile);
	}

	private void performFileCopying(final Uri localUri, final File cacheFile) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
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

				return null;
			}
		}.execute();
	}

	@Override
	public void insertRemoteFile(String title, Uri remoteUri) {
		db.execSQL("INSERT INTO file VALUES (?, NULL, NULL, ?, 0);", Utils.toArray(title, remoteUri.toString()));
	}

	@Override
	public String getLocalPath(Uri uri) {
		if (uri.getScheme().equals("file")) {
			return uri.getPath();
		}

		Cursor cursor = db.rawQuery("SELECT cacheUrl FROM file WHERE remoteUrl=?",
				Utils.toArray(uri.toString()));

		try {
			if (!cursor.moveToFirst()) {
				return null;
			} else {
				String urlStr = cursor.getString(0);
				if (urlStr == null)
					return null;
				else
					return Uri.parse(urlStr).getPath();
			}
		} finally {
			cursor.close();
		}
	}

	@Override
	public Uri uploadLocalFile(Uri cachedUri) {
		Cursor cursor = db.rawQuery("SELECT title, remoteUrl FROM file WHERE cacheUrl=?;",
				Utils.toArray(cachedUri.toString()));

		if (!cursor.moveToFirst())
			return null;

		if (!cursor.isNull(1)) {
			return Uri.parse(cursor.getString(1));
		}

		Uri remoteUri = uploadFile(cursor.getString(0), cachedUri);
		db.execSQL("UPDATE file SET remoteUrl=? WHERE cacheUrl=?",
				Utils.toArray(remoteUri.toString(), cachedUri.toString()));

		cursor.close();
		return remoteUri;
	}

	@Override
	public Bitmap getImageBitmap(String remoteUrl, int width, int height, ScaleMode mode) {
		Bitmap bitmap = iconCache.get(remoteUrl);
		if (bitmap == null || bitmap.getWidth() < width || bitmap.getHeight() < height) {
			// Create sampled bitmap that not worse than passed dimension (width, height)
			String localUrl = getLocalPath(Uri.parse(remoteUrl));
			if (localUrl == null) {
				insertRemoteFile("no-title", Uri.parse(remoteUrl));
				return null;
			}

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

	private Uri uploadFile(String title, Uri cachedUri) {
		try {
			String postUrl = uploadStage1(title);
			FileContent fileContent = uploadStage2(cachedUri, postUrl);
			return Uri.parse(fileContent.getUrl());
		} catch (IOException e) {
			log.error("GeTS error: ", e);
			//showError("Error uploading file");
		} catch (GetsException e) {
			log.error("Response error: ", e);
			//showError("Error uploading file: incorrect answer from server");
		}

		return null;
	}

	private String uploadStage1(String title) throws IOException, GetsException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String token = pref.getString(GetsStorage.PREF_AUTH_TOKEN, null);

		// FIXME: escape strings in xml
		String request = String.format(Locale.ROOT, GetsStorage.UPLOAD_FILE, token, title);
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

	}

	private static DefaultFileManager instance;
	public synchronized static DefaultFileManager getInstance() {
		if (instance == null) {
			instance = new DefaultFileManager(App.getContext());
		}

		return instance;
	}
}
