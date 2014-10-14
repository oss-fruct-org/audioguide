package org.fruct.oss.audioguide.files;

import org.fruct.oss.audioguide.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

public class DirectoryFileStorage implements FileStorage {
	private File directory;
	private Set<String> currentStoringFiles = Collections.synchronizedSet(new HashSet<String>());

	public DirectoryFileStorage(String directoryPath, Executor executor) throws IOException{
		this.directory = new File(directoryPath);
		this.directory.mkdirs();

		if (!directory.exists() || !directory.isDirectory()) {
			throw new IOException("Wrong directory: "+ directory);
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
		String name = toFileName(fileUrl, variant);

		currentStoringFiles.add(name);

		try {
			File file = new File(directory, name);

			FileOutputStream output = new FileOutputStream(file);

			try {
				Utils.copyStream(inputStream, output);
			} catch (IOException ex) {
				file.delete();
				throw ex;
			}
			return file.getPath();
		} finally {
			currentStoringFiles.remove(name);
		}
	}

	@Override
	public String getFile(String fileUrl, FileSource.Variant variant) {
		String name = toFileName(fileUrl, variant);

		if (currentStoringFiles.contains(name))
			return null;

		File file = new File(directory, name);

		if (file.exists() && file.canRead()) {
			return file.getPath();
		} else {
			return null;
		}
	}
}
