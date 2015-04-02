package org.fruct.oss.audioguide.files2;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PersistableDiskCache implements DiskCache {
	private final DiskCache persistentCache;
	private final DiskCache temporaryCache;
	private final PersistenceChecker checker;

	public PersistableDiskCache(DiskCache persistentCache, DiskCache temporaryCache, PersistenceChecker checker) {
		this.persistentCache = persistentCache;
		this.temporaryCache = temporaryCache;
		this.checker = checker;
	}

	@Override
	public File getDirectory() {
		return new File("/sdcard");
	}

	@Override
	public File get(String imageUri) {
		File file = persistentCache.get(imageUri);
		if (file == null || !file.exists()) {
			file = temporaryCache.get(imageUri);
		}

		if (file != null && file.exists()) {
			return file;
		} else {
			return null;
		}
	}

	@Override
	public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
		if (checker.isPersistent(imageUri)) {
			return persistentCache.save(imageUri, imageStream, listener);
		} else {
			return temporaryCache.save(imageUri, imageStream, listener);
		}
	}

	@Override
	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
		if (checker.isPersistent(imageUri)) {
			return persistentCache.save(imageUri, bitmap);
		} else {
			return temporaryCache.save(imageUri, bitmap);
		}
	}

	@Override
	public boolean remove(String imageUri) {
		boolean ret = persistentCache.remove(imageUri);
		ret |= temporaryCache.remove(imageUri);
		return ret;
	}

	@Override
	public void close() {
		persistentCache.close();
		temporaryCache.close();
	}

	@Override
	public void clear() {
		temporaryCache.clear();
		persistentCache.clear();
	}
}
