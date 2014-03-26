package org.fruct.oss.audioguide.util;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader {
	private final static Logger log = LoggerFactory.getLogger(Downloader.class);

	public static final String TMP_FILE_SUFFIX = ".download";

	private final Context context;
	private final Set<String> files;

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private final File localFileDir;

	public Downloader(Context context, String storageName) {
		this.context = context;

		// Directory should contain files with names representing md5's of corresponding uri's
		files = Collections.synchronizedSet(new HashSet<String>());
		localFileDir = openDir(storageName);
		if (localFileDir == null)
			return;

		log.trace("Loading local storage files for directory {}", localFileDir);
		for (File file : localFileDir.listFiles()) {
			if (file.isFile()) {
				log.trace("File {} added", file.getName());
				files.add(file.getName());
			}
		}
	}

	protected File openDir(String dir) {
		if (context == null)
			return null;

		return context.getExternalFilesDir(dir);
	}

	public void insertUri(Uri uri) {
		log.trace("Insert uri {}", uri);

		String uriString = uri.toString();
		String uriHash = Utils.hashString(uriString);

		if (!files.contains(uriHash)) {
			log.trace("Local storage doesn't contain uri {}", uri);
			enqueue(uriString, uriHash);
		}
	}

	/**
	 * Search uri in local storage and return local uri with file content if exists
	 * Otherwise returns same uri and enqueue it to download queue
	 * @param uri HTTP or HTTPS uri
	 * @return ready for use uri(cached or remote)
	 */
	public Uri getUri(final Uri uri) {
		String uriHash = Utils.hashString(uri.toString());
		if (files.contains(uriHash)) {
			String path = localFileDir.getPath() + "/" + uriHash;
			return Uri.fromFile(new File(path));
		} else {
			enqueue(uri.toString(), uriHash);
			return uri;
		}
	}

	private void enqueue(final String uri, final String uriHash) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				String tmpFilePath = null;
				try {
					String localFilePath = localFileDir.getAbsolutePath() + "/" + uriHash;
					tmpFilePath = localFilePath + TMP_FILE_SUFFIX;
					download(uri, tmpFilePath);

					if (!new File(tmpFilePath).renameTo(new File(localFilePath))) {
						log.warn("Can't rename successfully loaded file {}", tmpFilePath);
						throw new IOException("Can't rename file");
					}

					// All successfully loaded, add to list
					log.trace("Adding file to list {}", uriHash);
					files.add(uriHash);
				} catch (IOException ex) {
					log.warn("Error downloading file " + uri, ex);
					if (!new File(tmpFilePath).delete()) {
						log.warn("Can't delete incomplete file {}", tmpFilePath);
					}
				}
			}
		});
	}

	private void download(String uri, String localFile) throws IOException {
		URL url = new URL(uri);

		FileOutputStream output = null;
		InputStream input = null;
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestMethod("GET");
			conn.connect();

			int code = conn.getResponseCode();
			if (code == 200) {
				output = new FileOutputStream(localFile);
				input = conn.getInputStream();
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
}
