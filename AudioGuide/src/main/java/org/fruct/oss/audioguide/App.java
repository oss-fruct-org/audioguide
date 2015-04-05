package org.fruct.oss.audioguide;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.fruct.oss.audioguide.files2.DatabasePersistenceChecker;
import org.fruct.oss.audioguide.files2.PersistableDiskCache;
import org.fruct.oss.audioguide.files2.PersistenceChecker;
import org.fruct.oss.audioguide.track.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class App extends Application {
	private final static int CACHE_SIZE = 50_000_000;
	private final static Logger log = LoggerFactory.getLogger(App.class);

	private static Context context;
	private static App instance;

	private Database database;
	private PersistableDiskCache cache;
	private DatabasePersistenceChecker persistenceChecker;

	@Override
	public void onCreate() {
		super.onCreate();

		log.info("App onCreate");

		context = this.getApplicationContext();
		instance = this;
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		setupDatabase();
		setupImageLoader();
	}

	private void setupDatabase() {
		database = new Database(context);
	}

	private void setupImageLoader() {
		File cacheDir;
		if (BuildConfig.DEBUG) {
			cacheDir = new File("/sdcard/debug/uil");
		} else {
			cacheDir = new File(context.getCacheDir(), "uil");
		}

		File tmpCacheDir = new File(cacheDir, "tmpcache");
		tmpCacheDir.mkdirs();

		File persistentCacheDir = new File(cacheDir, "perscache");
		persistentCacheDir.mkdirs();

		DiskCache tmpCache;
		DiskCache persistentCache = new UnlimitedDiscCache(persistentCacheDir);

		try {
			tmpCache = new LruDiscCache(tmpCacheDir, new HashCodeFileNameGenerator(), CACHE_SIZE);
		} catch (IOException e) {
			tmpCache = new LimitedAgeDiscCache(tmpCacheDir, null, 3600);
			// TODO: handle error
		}

		persistenceChecker = new DatabasePersistenceChecker(database);
		cache = new PersistableDiskCache(persistentCache, tmpCache, persistenceChecker);

		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
				.defaultDisplayImageOptions(defaultOptions)
				.diskCache(cache)
				.build();

		ImageLoader.getInstance().init(config);
	}

	public Database getDatabase() {
		return database;
	}

	public DatabasePersistenceChecker getPersistenceChecker() {
		return persistenceChecker;
	}

	public PersistableDiskCache getCache() {
		return cache;
	}

	public static App getInstance() {
		return instance;
	}

	public static Context getContext() {
		return context;
	}


}
