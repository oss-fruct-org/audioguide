package org.fruct.oss.audioguide.files;

import java.io.Closeable;
import java.util.ArrayList;
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
	private final ExecutorService processorExecutor;

	private final List<FileListener> listeners = new CopyOnWriteArrayList<FileListener>();

	private final HashMap<String, DownloadTask> downloadTasks = new HashMap<String, DownloadTask>();

	public FileManager2(FileSource remoteFileSource, FileSource localFileSource, UrlResolver urlResolver,
						FileStorage cacheStorage, FileStorage persistentStorage, ExecutorService executor, ExecutorService processorExecutor) {

		this.remoteFileSource = remoteFileSource;
		this.localFileSource = localFileSource;
		this.urlResolver = urlResolver;

		this.cacheStorage = cacheStorage;
		this.persistentStorage = persistentStorage;

		this.executor = executor;
		this.processorExecutor = processorExecutor;

		/*queue = new PriorityBlockingQueue<Runnable>();
		executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, queue);*/
	}

	public void close() {
		executor.shutdownNow();
		processorExecutor.shutdownNow();
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
	 * @param storageType storage type
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

				synchronized (FileManager2.this) {
					for (FileListener listener : listeners) {
						listener.itemLoaded(fileUrl);
					}

					DownloadTask downloadTask = downloadTasks.get(fileUrl);

					assert downloadTask != null;

					for (FutureTask<?> processorFuture : downloadTask.postProcessorFutures) {
						processorExecutor.submit(processorFuture);
					}
				}

				return localFile;
			}
		});

		downloadTasks.put(fileUrl, new DownloadTask(future));
	}

	/**
	 * Ensure file downloaded and perform processing
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @param postProcessor transformation class
	 * @param <T> Type of result
	 * @return future of result
	 */
	public synchronized  <T> Future<T> postDownload(final String fileUrl, final FileSource.Variant variant, Storage storageType, final PostProcessor<T> postProcessor) {
		final String localFile = getLocalFile(fileUrl, variant);


		if (localFile != null) {
			FutureTask<T> processorFuture = new FutureTask<T>(new Callable<T>() {
				@Override
				public T call() throws Exception {
					return postProcessor.postProcess(localFile);
				}
			});

			processorExecutor.submit(processorFuture);
			return processorFuture;
		}

		FutureTask<T> processorFuture = new FutureTask<T>(new Callable<T>() {
			@Override
			public T call() throws Exception {
				String localFile = getLocalFile(fileUrl, variant);
				return postProcessor.postProcess(localFile);
			}
		});

		DownloadTask downloadTask = downloadTasks.get(fileUrl);
		if (downloadTask == null) {
			requestDownload(fileUrl, variant, storageType);
			downloadTask = downloadTasks.get(fileUrl);
		}

		downloadTask.postProcessors.add(postProcessor);
		downloadTask.postProcessorFutures.add(processorFuture);

		return processorFuture;
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

	private class DownloadTask {
		private DownloadTask(Future<String> future) {
			this.future = future;
			this.postProcessors = new ArrayList<PostProcessor<?>>();
			this.postProcessorFutures = new ArrayList<FutureTask<?>>();
		}

		final Future<String> future;

		// Parallel arrays of post processors and corresponding futures
		final List<PostProcessor<?>> postProcessors;
		final List<FutureTask<?>> postProcessorFutures;
	}
}
