package org.fruct.oss.audioguide;


import android.support.v4.app.Fragment;

public interface MultiPanel {
	void pushFragment(Fragment fragment);
	void popFragment();
	void replaceFragment(Fragment fragment, Fragment firstFragment);
}
