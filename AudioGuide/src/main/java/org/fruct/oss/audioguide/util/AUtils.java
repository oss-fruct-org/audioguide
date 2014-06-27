package org.fruct.oss.audioguide.util;


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.fragments.CommonFragment;
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

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	// http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
	public static Bitmap decodeSampledBitmapFromResource(Resources res, String filename,
														 int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}

	public static void reportError(Context context, String errorMessage) {
		LocalBroadcastManager.getInstance(context).sendBroadcast(
				new Intent(CommonFragment.BC_ERROR_MESSAGE)
						.putExtra(CommonFragment.BC_ARG_MESSAGE, errorMessage));
	}

	private static long distance2(Point a, Point b) {
		return (a.x - (long) b.x) * (a.x - (long) b.x) + (a.y - (long) b.y) * (a.y - (long) b.y);
	}

	public static double distanceToLine(Point a, Point b, Point c) {
		long d1 = distance2(a, c);
		long d2 = distance2(b, c);

		long dx = b.x - (long) a.x;
		long dy = b.y - (long) a.y;

		long num = dy * c.x - dx * c.y - a.x * (long) b.y + a.y * (long) b.x;

		double res = Math.min(Math.min(d1, d2), Math.abs(num) / Math.sqrt((dx * dx + dy * dy)));
		return res;
	}
}
