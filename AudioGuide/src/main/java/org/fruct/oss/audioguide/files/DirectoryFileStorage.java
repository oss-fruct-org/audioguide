package org.fruct.oss.audioguide.files;

import org.fruct.oss.audioguide.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class DirectoryFileStorage implements FileStorage {
	private File directory;

	public DirectoryFileStorage(String directoryPath, Executor executor) throws IOException{
		this.directory = new File(directoryPath);

		if (!directory.exists() || !directory.isDirectory()) {
			throw new IOException("Wrong directory");
		}

		executor.execute(new Runnable() {
			@Override
			public void run() {
				initialize();
			}
		});
	}

	private void initialize() {

	}

	private String toFileName(String fileUrl, FileSource.Variant variant) {
		return Utils.hashString(fileUrl) + "-" + variant.toString();
	}

	@Override
	public String storeFile(String fileUrl, FileSource.Variant variant, InputStream inputStream) throws IOException {
		File file = new File(directory, toFileName(fileUrl, variant));

		FileOutputStream output = new FileOutputStream(file);
		Utils.copyStream(inputStream, output);

		return file.getPath();
	}

	@Override
	public String getFile(String fileUrl, FileSource.Variant variant) {
		File file = new File(directory, toFileName(fileUrl, variant));

		if (file.exists() && file.canRead()) {
			return file.getPath();
		} else {
			return null;
		}
	}
}
