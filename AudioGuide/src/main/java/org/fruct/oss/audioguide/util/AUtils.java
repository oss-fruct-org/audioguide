package org.fruct.oss.audioguide.util;


import android.util.TypedValue;

import org.fruct.oss.audioguide.App;

public class AUtils {
	public static int getDP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	public static float getSP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	private AUtils() {
		throw new UnsupportedOperationException();
	}
}
