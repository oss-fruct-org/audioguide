package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TestTest extends AndroidTestCase {
	public void testTest() {
		assertTrue(true);

		TestInterface mock = mock(TestInterface.class);
		mock.foo();
		mock.bar(1);
		mock.bar(2);

		verify(mock).foo();
		verify(mock).bar(1);
		verify(mock).bar(2);
		verify(mock, never()).bar(3);
	}

	public void testExecutor() throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newSingleThreadExecutor();

		final boolean[] called = new boolean[2];

		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					called[0] = true;
					Thread.sleep(300);
					called[1] = true;
				} catch (InterruptedException ex) {
				}
				return "qwerty";
			}
		});

		executor.submit(task);
		Thread.sleep(200);
		task.cancel(true);
		Thread.sleep(200);
		assertTrue(called[0]);
		assertFalse(called[1]);
	}

	public void testExecutor2() throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "qwerty";
			}
		});

		executor.submit(task);

		Thread.sleep(50);

		assertEquals("qwerty", task.get());
	}

	private static interface TestInterface {
		void foo();
		void bar(int a);
	}
}