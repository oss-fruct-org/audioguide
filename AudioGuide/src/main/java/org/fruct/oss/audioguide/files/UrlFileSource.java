package org.fruct.oss.audioguide.files;

import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlFileSource implements FileSource {
	@Override
	public InputStream getInputStream(String fileUrl, Variant variant) throws IOException {
		URL url = new URL(fileUrl);

		HttpURLConnection conn = null;
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoInput(true);
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(15000);
		conn.setRequestMethod("GET");
		conn.connect();

		int code = conn.getResponseCode();

		return new HttpInputStream(conn);
	}

	private static class HttpInputStream extends FilterInputStream {
		private final HttpURLConnection conn;
		private int fileSize;

		protected HttpInputStream(HttpURLConnection conn) throws IOException {
			super(conn.getInputStream());

			try {
				fileSize = Integer.parseInt(conn.getHeaderField("Content-Length"));
			} catch (NumberFormatException ex) {
				fileSize = 1;
			}

			this.conn = conn;
		}

		@Override
		public int available() throws IOException {
			return fileSize;
		}

		@Override
		public void close() throws IOException {
			super.close();
			conn.disconnect();
		}
	}
}
