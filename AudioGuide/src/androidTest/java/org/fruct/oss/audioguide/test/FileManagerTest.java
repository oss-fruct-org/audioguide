package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import junit.framework.AssertionFailedError;

import org.fruct.oss.audioguide.files.FileListener;
import org.fruct.oss.audioguide.files.FileManager2;
import org.fruct.oss.audioguide.files.FileSource;
import org.fruct.oss.audioguide.files.FileStorage;
import org.fruct.oss.audioguide.files.PostProcessor;
import org.fruct.oss.audioguide.files.UrlResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

public class FileManagerTest extends AndroidTestCase {
	public static final String URL1 = "http://example.com/file.xml";

	private FileManager2 fileManager;

	private FileSource remoteFileSource;
	private FileSource localFileSource;

	private UrlFileStorage cacheStorage;
	private UrlFileStorage persistentStorage;

	private ExecutorService executor1;
	private ExecutorService executor2;

	private UrlResolver urlResolver;

	private CountDownLatch latch;
	private String itemLoaded;
	private TestListener listener;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		remoteFileSource = mock(FileSource.class);
		localFileSource = mock(FileSource.class);

		urlResolver = new ExactUrlResolver();

		cacheStorage = new UrlFileStorage();
		persistentStorage = new UrlFileStorage();

		executor1 = Executors.newSingleThreadExecutor();
		executor2 = Executors.newSingleThreadExecutor();

		fileManager = new FileManager2(remoteFileSource, localFileSource,
				urlResolver, cacheStorage, persistentStorage,
				executor1, executor2);

		latch = new CountDownLatch(1);
		itemLoaded = null;
		listener = new TestListener();
		fileManager.addListener(listener);
	}

	@Override
	protected void tearDown() throws Exception {
		fileManager.close();

		super.tearDown();
	}

	public void testUrlFileStorage() throws Exception {
		assertEquals("data:data2", cacheStorage.storeFile("file.txt", FileSource.Variant.FULL, createStream("data2")));
	}

	public void testInitialNoFiles() {
		assertNull(fileManager.getLocalFile(URL1, FileSource.Variant.FULL));
	}

	public void testFileDownload() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		fileManager.requestDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE);
		waitListener();

		assertNotNull(itemLoaded);
		String localFile = fileManager.getLocalFile(URL1, FileSource.Variant.FULL);
		assertNotNull(localFile);
		assertEquals("data:test", localFile);
	}

	public void testProcessing() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		Future<Integer> future = fileManager.postDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE, new CountPostProcessor());
		waitFuture(future);

		assertEquals(9, (int) future.get());
	}

	public void testProcessingReady() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		fileManager.requestDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE);
		Future<Integer> future = fileManager.postDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE, new CountPostProcessor());
		waitFuture(future);
		assertEquals(9, (int) future.get());
	}

	public void testTwoFutureProcessing() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);

		Future<Integer> future1 = fileManager.postDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE, new CountDelayPostProcessor(latch1));
		Future<Integer> future2 = fileManager.postDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE, new CountDelayPostProcessor(latch2));
		latch1.countDown();
		latch2.countDown();
		waitFuture(future1);
		waitFuture(future2);

		assertEquals(9, (int) future1.get());
		assertEquals(9, (int) future2.get());
	}

	public void testProcessingCancel() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		CountDownLatch latch1 = new CountDownLatch(1);
		CountDownLatch latch2 = new CountDownLatch(1);
		CountMonitorPostProcessor postProcessor = new CountMonitorPostProcessor(latch1, latch2);
		Future<Integer> future1 = fileManager.postDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE, postProcessor);

		waitLatch(latch1);
		future1.cancel(true);
		latch2.countDown();

		waitExecutor(executor2);
		assertTrue(postProcessor.isInterrupted());
	}

	public void testVariant() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("TESTTEST"));
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.PREVIEW)).thenReturn(createStream("test"));

		fileManager.requestDownload(URL1, FileSource.Variant.PREVIEW, FileManager2.Storage.CACHE);
		waitListener();

		String localFile = fileManager.getLocalFile(URL1, FileSource.Variant.PREVIEW);
		assertNotNull(localFile);
		assertEquals("data:test!", localFile);

		localFile = fileManager.getLocalFile(URL1, FileSource.Variant.FULL);
		assertNull(localFile);
	}

	public void testStoreVariant() throws Exception {
		assertEquals("data:data2!", cacheStorage.storeFile("file.txt", FileSource.Variant.PREVIEW, createStream("data2")));
	}

	private InputStream createStream(String str) {
		try {
			return new ByteArrayInputStream(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Android doesn't support UTF-8");
		}
	}

	private void waitListener() {
		try {
			if (!latch.await(100, TimeUnit.MILLISECONDS)) {
				throw new AssertionFailedError("Listener method didn't called");
			}
		} catch (InterruptedException e) {
			throw new AssertionFailedError("Listener method didn't called");
		}
	}

	private void waitLatch(CountDownLatch latch) {
		try {
			if (!latch.await(100, TimeUnit.MILLISECONDS)) {
				throw new AssertionFailedError("Latch didn't called");
			}
		} catch (InterruptedException e) {
			throw new AssertionFailedError("Latch didn't called");
		}
	}

	private void waitFuture(Future<?> future) {
		try {
			future.get(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignore) {
		} catch (ExecutionException e) {
			throw new RuntimeException("Processing error", e);
		} catch (TimeoutException e) {
			throw new AssertionFailedError("Processing timeout");
		}
	}

	private void waitExecutor(ExecutorService executorService) {
		Future<?> future = executorService.submit(new Runnable() {
			@Override
			public void run() {
			}
		});

		try {
			future.get(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Executor wait interrupted");
		} catch (ExecutionException e) {
			throw new RuntimeException("Executor wait failed");
		} catch (TimeoutException e) {
			throw new RuntimeException("Executor wait timeout");
		}
	}

	private class ExactUrlResolver implements UrlResolver {
		@Override
		public String getUrl(String fileUrl, FileSource.Variant variant) {
			return fileUrl;
		}
	}

	private class TestListener implements FileListener {
		@Override
		public void itemLoaded(String fileUrl) {
			itemLoaded = fileUrl;
			latch.countDown();
		}

		@Override
		public void itemDownloadProgress(String url, int current, int max) {

		}
	}

	private class UrlFileStorage implements FileStorage {
		private HashMap<String, String> files = new HashMap<String, String>();

		@Override
		public String storeFile(String fileUrl, FileSource.Variant variant, InputStream inputStream) throws IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String str = "data:" + reader.readLine() + (variant == FileSource.Variant.FULL ? "" : "!");
			files.put(fileUrl + variant.toString(), str);
			return str;
		}

		@Override
		public String getFile(String fileUrl, FileSource.Variant variant) {
			return files.get(fileUrl + variant.toString());
		}
	}

	private class CountPostProcessor implements PostProcessor<Integer> {
		@Override
		public Integer postProcess(String localUrl) {
			return localUrl.length();
		}
	}

	private class CountDelayPostProcessor implements PostProcessor<Integer> {
		private final CountDownLatch latch;

		public CountDelayPostProcessor(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public Integer postProcess(String localUrl) {
			try {
				if (!latch.await(100, TimeUnit.MILLISECONDS)) {
					throw new RuntimeException("CountDelayPostProcessor too long wait");
				}
			} catch (InterruptedException ignored) {
			}
			return localUrl.length();
		}
	}

	private class CountMonitorPostProcessor implements PostProcessor<Integer> {
		private final CountDownLatch monitorLatch;
		private final CountDownLatch contLatch;
		private boolean isInterrupted;

		public CountMonitorPostProcessor(CountDownLatch monitorLatch, CountDownLatch contLatch) {
			this.monitorLatch = monitorLatch;
			this.contLatch = contLatch;
		}

		@Override
		public Integer postProcess(String localUrl) {
			monitorLatch.countDown();

			try {
				if (!contLatch.await(100, TimeUnit.MILLISECONDS)) {
					throw new RuntimeException("CountDelayPostProcessor too long wait");
				}
			} catch (InterruptedException ignored) {
				isInterrupted = true;
			}

			return localUrl.length();
		}

		public boolean isInterrupted() {
			return isInterrupted;
		}
	}
}
