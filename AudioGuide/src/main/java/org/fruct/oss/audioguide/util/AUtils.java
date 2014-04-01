package org.fruct.oss.audioguide.util;


import android.util.TypedValue;

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
}
