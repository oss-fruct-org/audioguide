package org.fruct.oss.audioguide.files;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.utils.IoUtils;

import org.fruct.oss.audioguide.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public void setPersistentUrls(List<String> urls) {
		Set<File> filesInCache = new HashSet<>();
		Collections.addAll(filesInCache, persistentCache.getDirectory().listFiles());

		for (String url : urls) {
			File persistentCacheFile = persistentCache.get(url);

			if (persistentCacheFile == null || !persistentCacheFile.exists()) {
				File cachedFile = temporaryCache.get(url);
				FileInputStream inputStream = null;
				try {
					inputStream = new FileInputStream(cachedFile);
					persistentCache.save(url, inputStream, null);
				} catch (NullPointerException | IOException e) {
				} finally {
					Utils.sclose(inputStream);
				}
			} else {
				filesInCache.remove(persistentCacheFile);
			}
		}

		for (File file : filesInCache) {
			file.delete();
		}
	}

	private boolean contains(DiskCache cache, String url) {
		File file = cache.get(url);
		return file != null && file.exists();
	}
}
