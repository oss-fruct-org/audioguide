package org.fruct.oss.audioguide.files;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

public class IconCache extends LruCache<String, Bitmap> {
	public IconCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1)
			return 32;
		else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			return value.getByteCount() / 1024;
		else
			return value.getAllocationByteCount() / 1024;
	}
}
