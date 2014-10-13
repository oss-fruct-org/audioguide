package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import java.util.List;
import java.util.concurrent.Callable;
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

	/*public void testExecutor() {
		ExecutorService executor = Executors.newSingleThreadExecutor();

		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {

				}
				return "qwerty";
			}
		});

		Future<String> future = executor.submit(task);
	}*/

	private static interface TestInterface {
		void foo();
		void bar(int a);
	}
}