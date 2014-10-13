package org.fruct.oss.audioguide.test;

import android.os.CountDownTimer;
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

public class FileManagerTest extends AndroidTestCase {
	public static final String URL1 = "http://example.com/file.xml";

	private FileManager2 fileManager;
	private FileManager2 fileManager2;

	private FileSource remoteFileSource;
	private FileSource localFileSource;

	private UrlFileStorage cacheStorage;
	private UrlFileStorage persistentStorage;

	private FileStorage cacheStorageMock;
	private FileStorage persistentStorageMock;

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

		cacheStorageMock = mock(FileStorage.class);
		persistentStorageMock = mock(FileStorage.class);

		fileManager = new FileManager2(remoteFileSource, localFileSource,
				urlResolver, cacheStorage, persistentStorage, Executors.newSingleThreadExecutor());

		fileManager2 = new FileManager2(remoteFileSource, localFileSource,
				urlResolver, cacheStorageMock, persistentStorageMock, Executors.newSingleThreadExecutor());

		latch = new CountDownLatch(1);
		itemLoaded = null;
		listener = new TestListener();
		fileManager.addListener(listener);
	}

	@Override
	protected void tearDown() throws Exception {
		fileManager.close();
		fileManager2.close();

		super.tearDown();
	}

	public void testUrlFileStorage() throws Exception {
		assertEquals("data:data2", cacheStorage.storeFile("file.txt", createStream("data2")));
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

		Future<Integer> future = fileManager.postDownload(URL1, FileSource.Variant.FULL, new CountPostProcessor());
		waitFuture(future);

		assertEquals(4, (int) future.get());
	}

	public void testProcessingReady() throws Exception {
		when(remoteFileSource.getInputStream(URL1, FileSource.Variant.FULL)).thenReturn(createStream("test"));

		fileManager.requestDownload(URL1, FileSource.Variant.FULL, FileManager2.Storage.CACHE);
		Future<Integer> future = fileManager.postDownload(URL1, FileSource.Variant.FULL, new CountPostProcessor());
		waitFuture(future);
		assertEquals(4, (int) future.get());
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

	private class ExactUrlResolver implements UrlResolver {
		@Override
		public String getUrl(String fileUrl, FileSource.Variant variant) {
			return fileUrl;
		}
	}

	private class TestListener implements FileListener {
		@Override
		public void itemLoaded(String fileUrl) {
			latch.countDown();
			itemLoaded = fileUrl;
		}

		@Override
		public void itemDownloadProgress(String url, int current, int max) {

		}
	}

	private class UrlFileStorage implements FileStorage {
		private HashMap<String, String> files = new HashMap<String, String>();

		@Override
		public String storeFile(String fileUrl, InputStream inputStream) throws IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String str = "data:" + reader.readLine();
			files.put(fileUrl, str);
			return str;
		}

		@Override
		public String getFile(String fileUrl) {
			return files.get(fileUrl);
		}

		@Override
		public String[] getFiles() {
			return files.values().toArray(new String[files.size()]);
		}
	}

	private class CountPostProcessor implements PostProcessor<Integer> {
		@Override
		public Integer postProcess(String localUrl) {
			return localUrl.length();
		}
	}
}
