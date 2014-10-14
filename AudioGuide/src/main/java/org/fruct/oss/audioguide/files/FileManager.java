package org.fruct.oss.audioguide.files;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.util.ProgressInputStream;
import org.fruct.oss.audioguide.util.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class FileManager implements Closeable {
	private final FileSource remoteFileSource;
	private final FileSource localFileSource;

	private final UrlResolver urlResolver;
	private final FileStorage cacheStorage;
	private final FileStorage persistentStorage;

	private final ExecutorService executor;
	private final ExecutorService processorExecutor;

	private final List<FileListener> listeners = new CopyOnWriteArrayList<FileListener>();

	private final HashMap<DownloadTaskParameters, DownloadTask> downloadTasks = new HashMap<DownloadTaskParameters, DownloadTask>();
	private final Utils.Function<Void, Runnable> listenerHandler;

	private boolean isClosed;

	public FileManager(FileSource remoteFileSource, FileSource localFileSource, UrlResolver urlResolver,
					   FileStorage cacheStorage, FileStorage persistentStorage,
					   ExecutorService executor, ExecutorService processorExecutor,
					   Utils.Function<Void, Runnable> listenerHandler) {
		this.listenerHandler = listenerHandler;
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
		isClosed = true;
	}

	/**
	 * Returns cached local path of url and variant
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @return local cached path
	 */
	public String getLocalFile(String fileUrl, FileSource.Variant variant) {
		String localPath = cacheStorage.getFile(fileUrl, variant);
		if (localPath != null) {
			return localPath;
		}

		return persistentStorage.getFile(fileUrl, variant);
	}

	/**
	 * Request asynchronous download of file with url
	 * @param fileUrl source url
	 * @param variant variant (PREVIEW, FULL)
	 * @param storageType storage type
	 */
	public synchronized void requestDownload(final String fileUrl, final FileSource.Variant variant, final Storage storageType) {
		final DownloadTaskParameters parm = new DownloadTaskParameters(fileUrl, variant);
		if (downloadTasks.containsKey(parm) || getLocalFile(fileUrl, variant) != null) {
			return;
		}

		Future<String> future = executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				FileStorage storage = storageType == Storage.CACHE ? cacheStorage : persistentStorage;

				InputStream inputStream = remoteFileSource.getInputStream(fileUrl, variant);
				ProgressInputStream progressInputStream
						= new ProgressInputStream(inputStream,
							inputStream.available(), 100000, new ProgressInputStream.ProgressListener() {
					@Override
					public void update(final int current, final int max) {
						listenerHandler.apply(new Runnable() {
							@Override
							public void run() {
								for (FileListener listener : listeners) {
									listener.itemDownloadProgress(fileUrl, current, max);
								}
							}
						});
					}
				});

				String localFile = storage.storeFile(fileUrl, variant, progressInputStream);
				inputStream.close();

				synchronized (FileManager.this) {
					listenerHandler.apply(new Runnable() {
						@Override
						public void run() {
							for (FileListener listener : listeners) {
								listener.itemLoaded(fileUrl);
							}
						}
					});

					DownloadTask downloadTask = downloadTasks.get(parm);

					assert downloadTask != null;

					for (FutureTask<?> processorFuture : downloadTask.postProcessorFutures) {
						processorExecutor.submit(processorFuture);
					}
				}

				return localFile;
			}
		});

		downloadTasks.put(parm, new DownloadTask(future));
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
		final DownloadTaskParameters parm = new DownloadTaskParameters(fileUrl, variant);

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

		DownloadTask downloadTask = downloadTasks.get(parm);
		if (downloadTask == null) {
			requestDownload(fileUrl, variant, storageType);
			downloadTask = downloadTasks.get(parm);
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

	public enum ScaleMode {
		NO_SCALE, SCALE_CROP, SCALE_FIT
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

	private class DownloadTaskParameters {
		private DownloadTaskParameters(String url, FileSource.Variant variant) {
			this.url = url;
			this.variant = variant;
		}

		private String url;
		private FileSource.Variant variant;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			DownloadTaskParameters that = (DownloadTaskParameters) o;

			if (!url.equals(that.url)) return false;
			return variant == that.variant;

		}

		@Override
		public int hashCode() {
			int result = url.hashCode();
			result = 31 * result + variant.hashCode();
			return result;
		}
	}

	private static FileManager instance;
	public static synchronized FileManager getInstance() {
		if (instance == null || instance.isClosed) {
			Context context = App.getContext();
			UrlFileSource remoteFileSource = new UrlFileSource();

			ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
			ExecutorService processExecutor = Executors.newSingleThreadExecutor();

			File cacheDir = new File(context.getCacheDir(), "ag-file-storage2");
			File persistentDir = context.getDir("ag-file-storage2-p", Context.MODE_PRIVATE);

			DirectoryFileStorage cacheStorage;
			DirectoryFileStorage persistentStorage;

			try {
				cacheStorage = new DirectoryFileStorage(cacheDir.getPath(), downloadExecutor);
				persistentStorage = new DirectoryFileStorage(persistentDir.getPath(), downloadExecutor);
			} catch (IOException e) {
				throw new RuntimeException("Can't initialize file manager", e);
			}

			final Handler handler = new Handler(Looper.getMainLooper());
			instance = new FileManager(remoteFileSource, null, null,
					cacheStorage, persistentStorage, downloadExecutor,  processExecutor,
					new Utils.Function<Void, Runnable>() {
						@Override
						public Void apply(final Runnable runnable) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									runnable.run();
								}
							});
							return null;
						}
					});
		}

		return instance;
	}
}
