package org.fruct.oss.audioguide.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import junit.framework.TestCase;

import org.fruct.oss.audioguide.files.DirectoryFileStorage;
import org.fruct.oss.audioguide.files.FileSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DirectoryFileStorageTest extends AndroidTestCase {
	public static final String URL1 = "http://example.com/file.xml";
	public static final String URL2 = "http://example.com/file2.xml";

	private RenamingDelegatingContext context;
	private File dir;
	private ExecutorService executor;
	private DirectoryFileStorage fileStorage;

	public void setUp() throws Exception {
		super.setUp();

		context = new RenamingDelegatingContext(getContext(), "test-prefix-");
		dir = context.getDir("test", Context.MODE_PRIVATE);
		executor = Executors.newSingleThreadExecutor();
		fileStorage = new DirectoryFileStorage(dir.getPath(), executor);
		waitExecutor(executor);
	}

	public void tearDown() throws Exception {
		for (File file : dir.listFiles()) {
			file.delete();
		}
		dir.delete();
		executor.shutdownNow();
	}

	public void testStoreFile() throws Exception {
		fileStorage.storeFile(URL1, FileSource.Variant.FULL, createStream("qwe"));
		fileStorage.storeFile(URL1, FileSource.Variant.PREVIEW, createStream("asd"));
	}

	public void testGetFile() throws Exception {
		fileStorage.storeFile(URL1, FileSource.Variant.FULL, createStream("qwe"));

		assertNotNull(fileStorage.getFile(URL1, FileSource.Variant.FULL));
		assertNull(fileStorage.getFile(URL1, FileSource.Variant.PREVIEW));
		assertNull(fileStorage.getFile(URL2, FileSource.Variant.FULL));
	}

	public void testFileContent() throws Exception {
		fileStorage.storeFile(URL1, FileSource.Variant.FULL, createStream("qwe"));
		String file = fileStorage.getFile(URL1, FileSource.Variant.FULL);
		assertEquals("qwe", getFirstLine(file));
	}

	private String getFirstLine(String file) throws Exception {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			return reader.readLine();
		} finally {
			assertNotNull(reader);
			reader.close();
		}
	}

	private InputStream createStream(String str) {
		try {
			return new ByteArrayInputStream(str.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Android doesn't support UTF-8");
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
}