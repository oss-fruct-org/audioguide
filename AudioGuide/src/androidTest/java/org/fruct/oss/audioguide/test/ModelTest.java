package org.fruct.oss.audioguide.test;

import android.test.AndroidTestCase;

import org.fruct.oss.audioguide.models.BaseModel;
import org.fruct.oss.audioguide.models.CombineModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class ModelTest extends AndroidTestCase {
	public void testCombineModel() {
		BaseModel<String> m1 = new BaseModel<String>();
		m1.setData(Arrays.asList("qwe", "asd"));

		BaseModel<String> m2 = new BaseModel<String>();
		m2.setData(Arrays.asList("zxc", "mnb"));

		CombineModel<String> mc = new CombineModel<String>(m1, m2);
		assertEquals(4, mc.getCount());

		assertEquals("qwe", mc.getItem(0));
		assertEquals("asd", mc.getItem(1));
		assertEquals("zxc", mc.getItem(2));
		assertEquals("mnb", mc.getItem(3));
	}

	public void testCombineModelIterator() {
		BaseModel<String> m1 = new BaseModel<String>();
		m1.setData(Arrays.asList("qwe", "asd"));

		BaseModel<String> m2 = new BaseModel<String>();
		m2.setData(Arrays.asList("zxc", "mnb"));

		CombineModel<String> mc = new CombineModel<String>(m1, m2);

		Iterator<String> iter = mc.iterator();

		assertEquals("qwe", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("asd", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("zxc", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("mnb", iter.next());
		assertFalse(iter.hasNext());

		boolean th = false;
		try {
			iter.next();
		} catch (Exception ex) {
			th = true;
		}
		assertTrue(th);
	}
}
