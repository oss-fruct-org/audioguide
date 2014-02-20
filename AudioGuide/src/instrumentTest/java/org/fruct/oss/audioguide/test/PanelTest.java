package org.fruct.oss.audioguide.test;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import org.fruct.oss.audioguide.MainActivity;
import org.fruct.oss.audioguide.R;

public class PanelTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private Solo solo;

	public PanelTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAddPanel() {
		//solo.clickOnActionBarItem(R.id.action_test);
		//solo.clickOnActionBarHomeButton();
		//solo.waitForActivity("qweqwe");
	}
}
