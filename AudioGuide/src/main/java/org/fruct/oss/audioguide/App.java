package org.fruct.oss.audioguide;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
	private final static Logger log = LoggerFactory.getLogger(App.class);
	private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();

		log.info("App onCreate");

		context = this.getApplicationContext();
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
	}

	public static Context getContext() {
		return context;
	}
}
