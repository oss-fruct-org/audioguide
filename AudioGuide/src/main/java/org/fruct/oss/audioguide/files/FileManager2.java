package org.fruct.oss.audioguide.files;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FileManager2 implements Closeable {
	private final FileSource remoteFileSource;
	private final FileSource localFileSource;
	private final UrlResolver urlResolver;
	private final FileStorage cacheStorage;
	private final FileStorage persistentStorage;

	private final ExecutorService executor;

	private final List<FileListener> listeners = new CopyOnWriteArrayList<FileListener>();

	private final HashMap<String, Future<String>> downloadTasks = new HashMap<String, Future<String>>();

	public FileManager2(FileSource remoteFileSource, FileSource localFileSource, UrlResolver urlResolver,
						FileStorage cacheStorage, FileStorage persistentStorage, ExecutorService executor) {

		this.remoteFileSource = remoteFileSource;
		this.localFileSource = localFileSource;
		this.urlResolver = urlResolver;

		this.cacheStorage = cacheStorage;
		this.persistentStorage = persistentStorage;

		this.executor = executor;

		/*queue = new PriorityBlockingQueue<Runnable>();
		executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, queue);*/
	}

	public void close() {
		executor.shutdownNow();
	}

	/**
	 * Returns cached local path of url and variant
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @return local cached path
	 */
	public String getLocalFile(String fileUrl, FileSource.Variant variant) {
		String localPath = cacheStorage.getFile(fileUrl);
		if (localPath != null) {
			return localPath;
		}

		return persistentStorage.getFile(fileUrl);
	}

	/**
	 * Request asynchronous download of file with url
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @param storage storage type
	 */
	public synchronized void requestDownload(final String fileUrl, final FileSource.Variant variant, final Storage storageType) {
		if (downloadTasks.containsKey(fileUrl) || getLocalFile(fileUrl, variant) != null) {
			return;
		}

		Future<String> future = executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				FileStorage storage = storageType == Storage.CACHE ? cacheStorage : persistentStorage;
				String localFile = storage.storeFile(fileUrl, remoteFileSource.getInputStream(fileUrl, variant));

				for (FileListener listener : listeners) {
					listener.itemLoaded(fileUrl);
				}

				return localFile;
			}
		});

		downloadTasks.put(fileUrl, future);
	}

	/**
	 * Ensure file downloaded and perform processing
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @param postProcessor transformation class
	 * @param <T> Type of result
	 * @return future of result
	 */
	public <T> Future<T> postDownload(String fileUrl, FileSource.Variant variant, PostProcessor<T> postProcessor) {
		return null;
	}

	/**
	 * Add file listener
	 * @param listener listener
	 */
	public void addListener(FileListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove file listener
	 * @param listener listener
	 */
	public void removeListener(FileListener listener) {
		listeners.remove(listener);
	}

	public enum Storage {
		CACHE, PERSISTENT
	}


}
