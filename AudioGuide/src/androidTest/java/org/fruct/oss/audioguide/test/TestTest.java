package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import java.util.List;

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

	private static interface TestInterface {
		void foo();
		void bar(int a);
	}
}