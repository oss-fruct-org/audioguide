package org.fruct.oss.audioguide.util;


import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.fruct.oss.audioguide.App;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

public class AUtils {
	public static int getDP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	public static float getSP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	public static GeoPoint copyGeoPoint(IGeoPoint p) {
		return new GeoPoint(p.getLatitudeE6(), p.getLongitudeE6());
	}

	private AUtils() {
		throw new UnsupportedOperationException();
	}

	public static int getDialogTheme() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			return android.R.style.Theme_Dialog;
		} else {
			return android.R.style.Theme_Holo_Light_Dialog;
		}
	}

	public static Context getDialogContext(Context context) {
		return new ContextThemeWrapper(context, getDialogTheme());
	}
}
