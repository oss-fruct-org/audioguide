package org.fruct.oss.audioguide.files;

import android.content.Context;

import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Downloader {
	private final static Logger log = LoggerFactory.getLogger(Downloader.class);

	public static final String TMP_FILE_SUFFIX = ".download";

	private final Context context;
	private final Set<String> files;

	private final File localFileDir;
	private ProgressInputStream.ProgressListener progressListener;

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

	public void setProgressListener(ProgressInputStream.ProgressListener listener) {
		this.progressListener = listener;
	}

	protected File openDir(String dir) {
		if (context == null)
			return null;

		return context.getExternalFilesDir(dir);
	}

	public String downloadRemoteUrl(final String url) {
		String uriHash = Utils.hashString(url);
		if (files.contains(uriHash)) {
			return getLocalPath(uriHash);
		} else {
			return download(url, uriHash);
		}
	}

	public String getUrl(final String url) {
		String uriHash = Utils.hashString(url);
		if (files.contains(uriHash))
			return getLocalPath(uriHash);
		else
			return null;
	}


	private String getLocalPath(String urlHash) {
		return localFileDir.getPath() + "/" + urlHash;
	}

	private String download(final String uri, final String urlHash) {
		String tmpFilePath = null;
		try {
			String localFilePath = localFileDir.getAbsolutePath() + "/" + urlHash;
			tmpFilePath = localFilePath + TMP_FILE_SUFFIX;
			downloadUrl(uri, tmpFilePath);

			if (!new File(tmpFilePath).renameTo(new File(localFilePath))) {
				log.warn("Can't rename successfully loaded file {}", tmpFilePath);
				throw new IOException("Can't rename file");
			}

			// All successfully loaded, add to list
			log.trace("Adding file to list {}", urlHash);
			files.add(urlHash);

			return getLocalPath(urlHash);
		} catch (IOException ex) {
			log.warn("Error downloading file " + uri, ex);
			if (!new File(tmpFilePath).delete()) {
				log.warn("Can't delete incomplete file {}", tmpFilePath);
			}

			return null;
		}
	}

	private void downloadUrl(String uri, String localFile) throws IOException {
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
			int fileSize = Integer.parseInt(conn.getHeaderField("Content-Length"));
			if (code == 200) {
				output = new FileOutputStream(localFile);
				input = conn.getInputStream();

				if (progressListener != null && fileSize > 0) {
					input = new ProgressInputStream(input, fileSize, fileSize / 10, progressListener);
				}

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
