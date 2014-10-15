package org.fruct.oss.audioguide.files;

import org.fruct.oss.audioguide.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class DirectoryFileStorage implements FileStorage {
	private File directory;
	private Set<String> currentStoringFiles = Collections.synchronizedSet(new HashSet<String>());
	private Mode mode = Mode.FALLBACK;

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

	public DirectoryFileStorage(String directoryPath, Executor executor, Mode mode) throws IOException {
		this(directoryPath, executor);
		this.mode = mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
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

	@Override
	public void pullFile(FileStorage otherStorage, String fileUrl, FileSource.Variant variant) throws IOException {
		String otherLocalFilePath = otherStorage.getFile(fileUrl, variant);

		File otherLocalFile = new File(otherLocalFilePath);
		File newLocalFile = new File(directory, toFileName(fileUrl, variant));

		if (newLocalFile.exists())
			return;

		if (mode != Mode.COPY) {
			if (otherLocalFile.renameTo(newLocalFile)) {
				return;
			}
		}

		if (mode == Mode.RENAME)
			throw new IOException("Can't rename file " + otherLocalFilePath + " to " + newLocalFile);

		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			input = new FileInputStream(otherLocalFile);
			output = new FileOutputStream(newLocalFile);

			Utils.copyStream(input, output);

			otherLocalFile.delete();
		} catch (IOException ex) {
			newLocalFile.delete();
			throw ex;
		} finally {
			if (input != null)
				input.close();

			if (output != null)
				output.close();

		}
	}

	@Override
	public List<String> retainUrls(List<String> keepUrls) {
		Set<File> existingFiles = new HashSet<File>(Arrays.asList(directory.listFiles()));
		List<String> absentUrls = new ArrayList<String>();

		for (String keepUrl : keepUrls) {
			String nameFull = toFileName(keepUrl, FileSource.Variant.FULL);
			File fileFull = new File(directory, nameFull);

			if (!existingFiles.remove(fileFull)) {
				absentUrls.add(keepUrl);
			}
		}

		for (File remainingFile : existingFiles) {
			remainingFile.delete();
		}

		return absentUrls;
	}

	public enum Mode {
		FALLBACK,
		RENAME,
		COPY
	}
}
