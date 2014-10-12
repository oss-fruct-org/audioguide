package org.fruct.oss.audioguide.files;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

public class IconCache extends LruCache<IconCache.Key, Bitmap> {
	public IconCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(Key key, Bitmap value) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1)
			return 32;
		else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			return value.getByteCount() / 1024;
		else
			return value.getAllocationByteCount() / 1024;
	}

	public Bitmap get(String url, FileManager.ScaleMode mode) {
		return get(new Key(url, mode));
	}

	public void put(String url, FileManager.ScaleMode mode, Bitmap bitmap) {
		put(new Key(url, mode), bitmap);
	}

	static class Key {
		private Key(String url, FileManager.ScaleMode mode) {
			this.url = url;
			this.mode = mode;
		}

		final String url;
		final FileManager.ScaleMode mode;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Key key = (Key) o;

			if (mode != key.mode) return false;
			if (!url.equals(key.url)) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = url.hashCode();
			result = 31 * result + mode.hashCode();
			return result;
		}
	}
}
