package org.fruct.oss.audioguide.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {
	private static boolean locked = true;

	public static void checkEditLocked(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		locked = pref.getBoolean("private-edit-locked", true);
	}

	public static void toggleEditLocked(Context context) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		pref.edit().putBoolean("private-edit-locked", locked = !locked).apply();
	}

	public static boolean isEditLocked() {
		return locked;
	}
}
