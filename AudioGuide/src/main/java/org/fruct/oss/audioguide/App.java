package org.fruct.oss.audioguide;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class App extends Application {
	private final static Logger log = LoggerFactory.getLogger(App.class);
	private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();

		log.info("App onCreate");

		context = this.getApplicationContext();
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		setupImageLoader();
	}

	private void setupImageLoader() {
		File cacheDir;
		if (BuildConfig.DEBUG) {
			cacheDir = new File("/sdcard/debug/uil");
		} else {
			cacheDir = new File(context.getCacheDir(), "uil");
		}

		cacheDir.mkdirs();
		UnlimitedDiscCache cache = new UnlimitedDiscCache(cacheDir);

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

	public static Context getContext() {
		return context;
	}
}
